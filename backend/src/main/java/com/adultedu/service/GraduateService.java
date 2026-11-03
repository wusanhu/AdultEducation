package com.adultedu.service;

import cn.hutool.core.util.StrUtil;
import com.adultedu.common.BusinessException;
import com.adultedu.common.PageResult;
import com.adultedu.dto.BizDtos.AuditRequest;
import com.adultedu.dto.BizDtos.GraduateApplyRequest;
import com.adultedu.entity.ApprovalLog;
import com.adultedu.entity.Course;
import com.adultedu.entity.CreditRecord;
import com.adultedu.entity.GraduateApply;
import com.adultedu.entity.GraduateFile;
import com.adultedu.entity.ProjectMember;
import com.adultedu.entity.ProjectTeacher;
import com.adultedu.entity.StudyRecord;
import com.adultedu.entity.SysOrg;
import com.adultedu.entity.SysRole;
import com.adultedu.entity.SysUser;
import com.adultedu.entity.TrainingProject;
import com.adultedu.mapper.ApprovalLogMapper;
import com.adultedu.mapper.CourseMapper;
import com.adultedu.mapper.CreditRecordMapper;
import com.adultedu.mapper.GraduateApplyMapper;
import com.adultedu.mapper.GraduateFileMapper;
import com.adultedu.mapper.ProjectMemberMapper;
import com.adultedu.mapper.ProjectTeacherMapper;
import com.adultedu.mapper.StudyRecordMapper;
import com.adultedu.mapper.SysOrgMapper;
import com.adultedu.mapper.SysRoleMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.mapper.TrainingProjectMapper;
import com.adultedu.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraduateService {
    private final GraduateApplyMapper applyMapper;
    private final GraduateFileMapper fileMapper;
    private final TrainingProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final ProjectTeacherMapper teacherMapper;
    private final CourseMapper courseMapper;
    private final StudyRecordMapper studyMapper;
    private final CreditRecordMapper creditMapper;
    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;
    private final SysRoleMapper roleMapper;
    private final ApprovalLogMapper approvalLogMapper;
    private final CreditService creditService;
    private final CertificateService certificateService;
    private final NotificationService notificationService;
    private final DataScopeService dataScopeService;
    private final ProjectService projectService;
    private final StringRedisTemplate redis;

    // ---------------- 学员端 ----------------

    public Map<String, Object> eligibility(LoginUser current, Long projectId) {
        TrainingProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        ProjectMember member = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, current.getId())
                .eq(ProjectMember::getStatus, 1));
        BigDecimal credits = creditService.projectCreditTotal(current.getId(), projectId);
        GraduateApply active = applyMapper.selectOne(new LambdaQueryWrapper<GraduateApply>()
                .eq(GraduateApply::getUserId, current.getId()).eq(GraduateApply::getProjectId, projectId)
                .eq(GraduateApply::getAuditStatus, 0).orderByDesc(GraduateApply::getId).last("limit 1"));
        GraduateApply approved = applyMapper.selectOne(new LambdaQueryWrapper<GraduateApply>()
                .eq(GraduateApply::getUserId, current.getId()).eq(GraduateApply::getProjectId, projectId)
                .eq(GraduateApply::getAuditStatus, 1).orderByDesc(GraduateApply::getId).last("limit 1"));
        GraduateFile file = approved == null ? null : fileMapper.selectOne(new LambdaQueryWrapper<GraduateFile>()
                .eq(GraduateFile::getApplyId, approved.getId()).last("limit 1"));
        boolean canApply = member != null && credits.compareTo(project.getNeedCredit()) >= 0
                && active == null && approved == null;
        String reason = null;
        if (member == null) {
            reason = "您不在该培训班中";
        } else if (approved != null) {
            reason = "您已通过毕业审核";
        } else if (active != null) {
            reason = "您已有待审批的毕业申请";
        } else if (credits.compareTo(project.getNeedCredit()) < 0) {
            reason = "学分不足，还差 " + project.getNeedCredit().subtract(credits) + " 学分";
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("projectId", projectId);
        m.put("projectName", project.getProjectName());
        m.put("needCredit", project.getNeedCredit());
        m.put("credits", credits);
        m.put("canApply", canApply);
        m.put("reason", reason);
        m.put("pendingApplyId", active == null ? null : active.getId());
        m.put("approvedApplyId", approved == null ? null : approved.getId());
        m.put("certificateUrl", file == null ? null : file.getCertificateUrl());
        return m;
    }

    @Transactional
    public Long apply(LoginUser current, GraduateApplyRequest req) {
        String lockKey = "lock:graduate:apply:" + current.getId() + ":" + req.projectId();
        boolean locked = Boolean.TRUE.equals(redis.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(10)));
        if (!locked) {
            for (int i = 0; i < 10; i++) {
                if (hasPendingApply(current.getId(), req.projectId())) {
                    throw new BusinessException("您已有待审批的毕业申请，请勿重复提交");
                }
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            throw new BusinessException("毕业申请正在处理中，请稍后重试");
        }
        try {
            TrainingProject project = projectMapper.selectById(req.projectId());
            if (project == null) {
                throw new BusinessException("培训班不存在");
            }
            if (project.getStatus() != null && project.getStatus() == 3) {
                throw new BusinessException("培训班已归档，无法申请毕业");
            }
            ProjectMember member = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, req.projectId()).eq(ProjectMember::getUserId, current.getId())
                    .eq(ProjectMember::getStatus, 1));
            if (member == null) {
                throw new BusinessException("您不在该培训班中");
            }
            if (fileMapper.selectCount(new LambdaQueryWrapper<GraduateFile>()
                    .eq(GraduateFile::getUserId, current.getId()).eq(GraduateFile::getProjectId, req.projectId())) > 0) {
                throw new BusinessException("您已毕业，无需重复申请");
            }
            if (hasPendingApply(current.getId(), req.projectId())) {
                throw new BusinessException("您已有待审批的毕业申请，请勿重复提交");
            }
            BigDecimal credits = creditService.projectCreditTotal(current.getId(), req.projectId());
            if (credits.compareTo(project.getNeedCredit()) < 0) {
                throw new BusinessException("学分不足，还差 " + project.getNeedCredit().subtract(credits) + " 学分，无法申请毕业");
            }
            BigDecimal hours = studyHours(current.getId(), req.projectId());
            GraduateApply apply = new GraduateApply();
            apply.setUserId(current.getId());
            apply.setProjectId(req.projectId());
            apply.setApplyTime(LocalDateTime.now());
            apply.setAuditStatus(0);
            apply.setCreditSnapshot(credits);
            apply.setStudyHourSnapshot(hours);
            String node = firstNode(project);
            apply.setCurrentNode(node);
            applyMapper.insert(apply);
            writeLog(apply.getId(), "SUBMIT", current, node, "学员提交毕业申请");
            notifyNode(apply, node, current.getId());
            return apply.getId();
        } finally {
            releaseLockAfterCompletion(lockKey);
        }
    }

    /**
     * 事务提交后再释放锁，确保等待的请求能读到已提交的申请记录。
     */
    private void releaseLockAfterCompletion(String key) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            redis.delete(key);
            return;
        }
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        try {
                            redis.delete(key);
                        } catch (Exception ignored) {
                            // 锁过期会自动释放
                        }
                    }
                });
    }

    private boolean hasPendingApply(Long userId, Long projectId) {
        return applyMapper.selectCount(new LambdaQueryWrapper<GraduateApply>()
                .eq(GraduateApply::getUserId, userId).eq(GraduateApply::getProjectId, projectId)
                .eq(GraduateApply::getAuditStatus, 0)) > 0;
    }

    public Map<String, Object> myApplication(LoginUser current, Long projectId) {
        LambdaQueryWrapper<GraduateApply> qw = new LambdaQueryWrapper<GraduateApply>()
                .eq(GraduateApply::getUserId, current.getId());
        if (projectId != null) {
            qw.eq(GraduateApply::getProjectId, projectId);
        }
        qw.orderByDesc(GraduateApply::getApplyTime).last("limit 1");
        GraduateApply apply = applyMapper.selectOne(qw);
        if (apply == null) {
            return null;
        }
        return toMap(apply, true);
    }

    public List<Map<String, Object>> myApplications(LoginUser current) {
        List<GraduateApply> list = applyMapper.selectList(new LambdaQueryWrapper<GraduateApply>()
                .eq(GraduateApply::getUserId, current.getId())
                .orderByDesc(GraduateApply::getApplyTime));
        return list.stream().map(a -> toMap(a, true)).toList();
    }

    // ---------------- 管理端 ----------------

    public PageResult<Map<String, Object>> applyPage(LoginUser current, long pageNum, long pageSize,
                                                     Long projectId, Integer auditStatus, String currentNode, String keyword) {
        LambdaQueryWrapper<GraduateApply> qw = new LambdaQueryWrapper<>();
        if (projectId != null) {
            qw.eq(GraduateApply::getProjectId, projectId);
        }
        if (auditStatus != null) {
            qw.eq(GraduateApply::getAuditStatus, auditStatus);
        }
        if (StrUtil.isNotBlank(currentNode)) {
            qw.eq(GraduateApply::getCurrentNode, currentNode);
        }
        // 待审批列表只展示当前角色有权处理的节点，避免越权节点混入
        if (auditStatus != null && auditStatus == 0) {
            boolean admin = current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1);
            if (!admin) {
                if ("teacher".equals(current.getRoleCode())) {
                    qw.eq(GraduateApply::getCurrentNode, "TEACHER");
                } else if ("leader".equals(current.getRoleCode())) {
                    qw.eq(GraduateApply::getCurrentNode, "LEADER");
                }
            }
        }
        if (!applyScope(current, qw)) {
            return PageResult.of(List.of(), 0, pageNum, pageSize);
        }
        if (StrUtil.isNotBlank(keyword)) {
            List<Long> matched = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, keyword).or().like(SysUser::getUsername, keyword))
                    .stream().map(SysUser::getId).toList();
            if (matched.isEmpty()) {
                return PageResult.of(List.of(), 0, pageNum, pageSize);
            }
            qw.in(GraduateApply::getUserId, matched);
        }
        qw.orderByDesc(GraduateApply::getApplyTime).orderByDesc(GraduateApply::getId);
        Page<GraduateApply> page = applyMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(a -> toMap(a, false)).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    private boolean applyScope(LoginUser current, LambdaQueryWrapper<GraduateApply> qw) {
        if (current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1)) {
            return true;
        }
        if ("student".equals(current.getRoleCode())) {
            qw.eq(GraduateApply::getUserId, current.getId());
            return true;
        }
        if ("teacher".equals(current.getRoleCode())) {
            List<Long> projectIds = teacherMapper.selectList(new LambdaQueryWrapper<ProjectTeacher>()
                            .eq(ProjectTeacher::getUserId, current.getId()).select(ProjectTeacher::getProjectId))
                    .stream().map(ProjectTeacher::getProjectId).toList();
            List<Long> all = new ArrayList<>(projectIds);
            courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Course::getCreateUserId, current.getId())
                            .select(Course::getProjectId))
                    .forEach(c -> all.add(c.getProjectId()));
            if (all.isEmpty()) {
                return false;
            }
            qw.in(GraduateApply::getProjectId, all.stream().distinct().toList());
            return true;
        }
        List<Long> orgIds = dataScopeService.accessibleOrgIds(current);
        if (orgIds == null) {
            return true;
        }
        if (orgIds.isEmpty()) {
            return false;
        }
        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .and(w -> w.in(SysUser::getOrgId, orgIds).or().in(SysUser::getDeptId, orgIds)))
                .stream().map(SysUser::getId).toList();
        if (userIds.isEmpty()) {
            return false;
        }
        qw.in(GraduateApply::getUserId, userIds);
        return true;
    }

    public Map<String, Object> detail(LoginUser current, Long id) {
        GraduateApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException("毕业申请不存在");
        }
        if (!canView(current, apply)) {
            throw new BusinessException(403, "无权查看该毕业申请");
        }
        return toMap(apply, true);
    }

    private boolean canView(LoginUser current, GraduateApply apply) {
        if (current.getId().equals(apply.getUserId())) {
            return true;
        }
        if (current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1)) {
            return true;
        }
        if ("teacher".equals(current.getRoleCode())) {
            return isProjectTeacher(current.getId(), apply.getProjectId());
        }
        SysUser user = userMapper.selectById(apply.getUserId());
        return user != null && dataScopeService.canAccessOrg(current, user.getOrgId() == null ? user.getDeptId() : user.getOrgId());
    }

    @Transactional
    public void audit(LoginUser current, Long id, AuditRequest req) {
        GraduateApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException("毕业申请不存在");
        }
        if (apply.getAuditStatus() == null || apply.getAuditStatus() != 0) {
            throw new BusinessException("该申请已处理，不能重复审批");
        }
        TrainingProject project = projectMapper.selectById(apply.getProjectId());
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        projectService.assertProjectEditable(project);
        String node = apply.getCurrentNode();
        if (!canAudit(current, apply, node)) {
            throw new BusinessException(403, "当前节点无权审批该申请");
        }
        String action = req.action() == null ? "" : req.action().trim().toLowerCase();
        boolean approve = "approve".equals(action) || "pass".equals(action);
        if (!approve && !"reject".equals(action)) {
            throw new BusinessException("审批操作仅支持 approve / reject");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!approve) {
            if (StrUtil.isBlank(req.opinion())) {
                throw new BusinessException("驳回时必须填写驳回原因");
            }
            apply.setAuditStatus(2);
            apply.setCurrentNode("REJECTED");
            apply.setRejectReason(req.opinion());
            fillAudit(apply, node, current, now, req.opinion());
            applyMapper.updateById(apply);
            writeLog(id, "REJECT", current, node, req.opinion());
            notificationService.send(apply.getUserId(), "毕业申请被驳回",
                    "您的《" + project.getProjectName() + "》毕业申请被驳回。原因：" + req.opinion(), "GRADUATE", id);
            return;
        }
        fillAudit(apply, node, current, now, req.opinion());
        String next = nextNode(project, node);
        writeLog(id, "APPROVE", current, node, StrUtil.blankToDefault(req.opinion(), nodeLabel(node) + "通过"));
        if ("DONE".equals(next)) {
            apply.setAuditStatus(1);
            apply.setCurrentNode("DONE");
            apply.setFinalAuditUser(current.getId());
            apply.setFinalAuditTime(now);
            apply.setFinalAuditOpinion(req.opinion());
            applyMapper.updateById(apply);
            GraduateFile file = certificateService.generate(apply, userMapper.selectById(apply.getUserId()), project);
            ProjectMember member = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, apply.getProjectId())
                    .eq(ProjectMember::getUserId, apply.getUserId()));
            if (member != null) {
                member.setCompletionStatus(2);
                memberMapper.updateById(member);
            }
            notificationService.send(apply.getUserId(), "毕业审核已通过",
                    "恭喜您通过《" + project.getProjectName() + "》毕业审核，电子证书已生成，可在毕业档案中下载。",
                    "GRADUATE", id);
            log.info("毕业证书生成: applyId={}, url={}", id, file.getCertificateUrl());
        } else {
            apply.setCurrentNode(next);
            applyMapper.updateById(apply);
            notifyNode(apply, next, current.getId());
        }
    }

    private void fillAudit(GraduateApply apply, String node, LoginUser current, LocalDateTime now, String opinion) {
        if ("TEACHER".equals(node)) {
            apply.setFirstAuditUser(current.getId());
            apply.setFirstAuditTime(now);
            apply.setFirstAuditOpinion(opinion);
        } else if ("LEADER".equals(node)) {
            apply.setSecondAuditUser(current.getId());
            apply.setSecondAuditTime(now);
            apply.setSecondAuditOpinion(opinion);
        } else {
            apply.setFinalAuditUser(current.getId());
            apply.setFinalAuditTime(now);
            apply.setFinalAuditOpinion(opinion);
        }
    }

    private boolean canAudit(LoginUser current, GraduateApply apply, String node) {
        if (current.isSuperAdmin()) {
            return true;
        }
        if ("TEACHER".equals(node)) {
            return "teacher".equals(current.getRoleCode()) && isProjectTeacher(current.getId(), apply.getProjectId());
        }
        if ("LEADER".equals(node)) {
            if (!"leader".equals(current.getRoleCode())) {
                return false;
            }
            SysUser student = userMapper.selectById(apply.getUserId());
            return student != null && dataScopeService.canAccessOrg(current,
                    student.getOrgId() == null ? student.getDeptId() : student.getOrgId());
        }
        if ("ADMIN".equals(node)) {
            return current.isSuperAdmin();
        }
        return false;
    }

    private String firstNode(TrainingProject project) {
        if (flag(project.getNeedTeacherAudit()) && hasProjectTeacher(project.getId())) {
            return "TEACHER";
        }
        if (flag(project.getNeedLeaderAudit())) {
            return "LEADER";
        }
        return "ADMIN";
    }

    private String nextNode(TrainingProject project, String node) {
        return switch (node) {
            case "TEACHER" -> flag(project.getNeedLeaderAudit()) ? "LEADER"
                    : (flag(project.getNeedAdminAudit()) ? "ADMIN" : "DONE");
            case "LEADER" -> flag(project.getNeedAdminAudit()) ? "ADMIN" : "DONE";
            case "ADMIN" -> "DONE";
            default -> "DONE";
        };
    }

    private boolean flag(Integer v) {
        return v != null && v == 1;
    }

    private boolean hasProjectTeacher(Long projectId) {
        if (teacherMapper.selectCount(new LambdaQueryWrapper<ProjectTeacher>()
                .eq(ProjectTeacher::getProjectId, projectId)) > 0) {
            return true;
        }
        return courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getProjectId, projectId).isNotNull(Course::getCreateUserId)) > 0;
    }

    private boolean isProjectTeacher(Long userId, Long projectId) {
        if (teacherMapper.selectCount(new LambdaQueryWrapper<ProjectTeacher>()
                .eq(ProjectTeacher::getUserId, userId).eq(ProjectTeacher::getProjectId, projectId)) > 0) {
            return true;
        }
        return courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getProjectId, projectId).eq(Course::getCreateUserId, userId)) > 0;
    }

    private void notifyNode(GraduateApply apply, String node, Long excludeUserId) {
        TrainingProject project = projectMapper.selectById(apply.getProjectId());
        SysUser student = userMapper.selectById(apply.getUserId());
        String title = switch (node) {
            case "TEACHER" -> "毕业申请待初审";
            case "LEADER" -> "毕业申请待复审";
            default -> "毕业申请待终审";
        };
        String content = "学员" + (student == null ? "" : student.getRealName()) + "提交了《"
                + (project == null ? "" : project.getProjectName()) + "》毕业申请，请及时审批。";
        List<Long> recipients = new ArrayList<>();
        if ("TEACHER".equals(node)) {
            recipients.addAll(teacherMapper.selectList(new LambdaQueryWrapper<ProjectTeacher>()
                            .eq(ProjectTeacher::getProjectId, apply.getProjectId()))
                    .stream().map(ProjectTeacher::getUserId).toList());
            recipients.addAll(courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .eq(Course::getProjectId, apply.getProjectId()).isNotNull(Course::getCreateUserId))
                    .stream().map(Course::getCreateUserId).toList());
        } else {
            String roleCode = "LEADER".equals(node) ? "leader" : "super_admin";
            SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
            if (role != null) {
                recipients.addAll(userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getRoleId, role.getId()).eq(SysUser::getStatus, 1))
                        .stream().map(SysUser::getId).toList());
            }
        }
        List<Long> ids = recipients.stream().filter(id -> !id.equals(excludeUserId)).distinct().toList();
        notificationService.sendBatch(ids, title, content, "APPROVAL", apply.getId());
    }

    private BigDecimal studyHours(Long userId, Long projectId) {
        List<StudyRecord> records = studyMapper.selectList(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, userId).eq(StudyRecord::getProjectId, projectId));
        int seconds = records.stream().mapToInt(r -> r.getValidDuration() == null ? 0 : r.getValidDuration()).sum();
        return BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }

    private void writeLog(Long applyId, String action, LoginUser current, String node, String opinion) {
        ApprovalLog log = new ApprovalLog();
        log.setBizType("GRADUATE");
        log.setBizId(applyId);
        log.setNode(node);
        log.setAuditorId(current.getId());
        log.setAuditorName(current.getRealName());
        log.setAction(action);
        log.setOpinion(opinion);
        approvalLogMapper.insert(log);
    }

    private Map<String, Object> toMap(GraduateApply apply, boolean withDetail) {
        SysUser student = userMapper.selectById(apply.getUserId());
        TrainingProject project = projectMapper.selectById(apply.getProjectId());
        GraduateFile file = fileMapper.selectOne(new LambdaQueryWrapper<GraduateFile>()
                .eq(GraduateFile::getApplyId, apply.getId()).last("limit 1"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", apply.getId());
        m.put("userId", apply.getUserId());
        m.put("userName", student == null ? null : student.getRealName());
        m.put("username", student == null ? null : student.getUsername());
        m.put("orgName", student == null ? null : orgName(student.getOrgId() == null ? student.getDeptId() : student.getOrgId()));
        m.put("projectId", apply.getProjectId());
        m.put("projectName", project == null ? null : project.getProjectName());
        m.put("projectArchived", project != null && project.getStatus() != null && project.getStatus() == 3);
        m.put("needCredit", project == null ? null : project.getNeedCredit());
        m.put("applyTime", apply.getApplyTime());
        m.put("currentNode", apply.getCurrentNode());
        m.put("currentNodeLabel", nodeLabel(apply.getCurrentNode()));
        m.put("auditStatus", apply.getAuditStatus());
        m.put("auditStatusLabel", apply.getAuditStatus() == null ? "" :
                (apply.getAuditStatus() == 1 ? "已通过" : (apply.getAuditStatus() == 2 ? "已驳回" : "审批中")));
        m.put("rejectReason", apply.getRejectReason());
        m.put("creditSnapshot", apply.getCreditSnapshot());
        m.put("studyHourSnapshot", apply.getStudyHourSnapshot());
        m.put("firstAuditUser", apply.getFirstAuditUser());
        m.put("firstAuditUserName", userName(apply.getFirstAuditUser()));
        m.put("firstAuditTime", apply.getFirstAuditTime());
        m.put("firstAuditOpinion", apply.getFirstAuditOpinion());
        m.put("secondAuditUser", apply.getSecondAuditUser());
        m.put("secondAuditUserName", userName(apply.getSecondAuditUser()));
        m.put("secondAuditTime", apply.getSecondAuditTime());
        m.put("secondAuditOpinion", apply.getSecondAuditOpinion());
        m.put("finalAuditUser", apply.getFinalAuditUser());
        m.put("finalAuditUserName", userName(apply.getFinalAuditUser()));
        m.put("finalAuditTime", apply.getFinalAuditTime());
        m.put("finalAuditOpinion", apply.getFinalAuditOpinion());
        m.put("certificateNo", file == null ? null : file.getCertificateNo());
        m.put("certificateUrl", file == null ? null : file.getCertificateUrl());
        m.put("graduateTime", file == null ? null : file.getGraduateTime());
        if (withDetail) {
            m.put("approvalLogs", approvalLogMapper.selectList(new LambdaQueryWrapper<ApprovalLog>()
                    .eq(ApprovalLog::getBizType, "GRADUATE").eq(ApprovalLog::getBizId, apply.getId())
                    .orderByAsc(ApprovalLog::getCreateTime)));
        }
        return m;
    }

    // ---------------- 毕业档案 ----------------

    public PageResult<Map<String, Object>> filePage(LoginUser current, long pageNum, long pageSize,
                                                    Long projectId, String keyword, String year) {
        LambdaQueryWrapper<GraduateFile> qw = new LambdaQueryWrapper<>();
        if (projectId != null) {
            qw.eq(GraduateFile::getProjectId, projectId);
        }
        if (StrUtil.isNotBlank(year)) {
            qw.apply("DATE_FORMAT(graduate_time,'%Y') = {0}", year);
        }
        if (!fileScope(current, qw)) {
            return PageResult.of(List.of(), 0, pageNum, pageSize);
        }
        if (StrUtil.isNotBlank(keyword)) {
            List<Long> matched = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, keyword).or().like(SysUser::getUsername, keyword))
                    .stream().map(SysUser::getId).toList();
            if (matched.isEmpty()) {
                return PageResult.of(List.of(), 0, pageNum, pageSize);
            }
            qw.in(GraduateFile::getUserId, matched);
        }
        qw.orderByDesc(GraduateFile::getGraduateTime);
        Page<GraduateFile> page = fileMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(enrichFiles(page.getRecords()), page.getTotal(), pageNum, pageSize);
    }

    private boolean fileScope(LoginUser current, LambdaQueryWrapper<GraduateFile> qw) {
        if (current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1)) {
            return true;
        }
        if ("student".equals(current.getRoleCode())) {
            qw.eq(GraduateFile::getUserId, current.getId());
            return true;
        }
        if ("teacher".equals(current.getRoleCode())) {
            List<Long> projectIds = teacherMapper.selectList(new LambdaQueryWrapper<ProjectTeacher>()
                            .eq(ProjectTeacher::getUserId, current.getId()).select(ProjectTeacher::getProjectId))
                    .stream().map(ProjectTeacher::getProjectId).toList();
            if (projectIds.isEmpty()) {
                return false;
            }
            qw.in(GraduateFile::getProjectId, projectIds);
            return true;
        }
        List<Long> orgIds = dataScopeService.accessibleOrgIds(current);
        if (orgIds == null) {
            return true;
        }
        if (orgIds.isEmpty()) {
            return false;
        }
        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .and(w -> w.in(SysUser::getOrgId, orgIds).or().in(SysUser::getDeptId, orgIds)))
                .stream().map(SysUser::getId).toList();
        if (userIds.isEmpty()) {
            return false;
        }
        qw.in(GraduateFile::getUserId, userIds);
        return true;
    }

    private List<Map<String, Object>> enrichFiles(List<GraduateFile> files) {
        if (files.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = files.stream().map(GraduateFile::getUserId).distinct().toList();
        List<Long> projectIds = files.stream().map(GraduateFile::getProjectId).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        Map<Long, String> projects = projectIds.isEmpty() ? Map.of() : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(TrainingProject::getId, TrainingProject::getProjectName, (a, b) -> a));
        return files.stream().map(f -> {
            SysUser u = users.get(f.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("applyId", f.getApplyId());
            m.put("userId", f.getUserId());
            m.put("userName", u == null ? null : u.getRealName());
            m.put("username", u == null ? null : u.getUsername());
            m.put("orgName", u == null ? null : orgName(u.getOrgId() == null ? u.getDeptId() : u.getOrgId()));
            m.put("projectId", f.getProjectId());
            m.put("projectName", projects.get(f.getProjectId()));
            m.put("certificateNo", f.getCertificateNo());
            m.put("certificateUrl", f.getCertificateUrl());
            m.put("graduateTime", f.getGraduateTime());
            m.put("creditValue", f.getCreditValue());
            return m;
        }).toList();
    }

    public Map<String, Object> myCertificate(LoginUser current, Long projectId) {
        GraduateFile file = fileMapper.selectOne(new LambdaQueryWrapper<GraduateFile>()
                .eq(GraduateFile::getUserId, current.getId())
                .eq(projectId != null, GraduateFile::getProjectId, projectId)
                .orderByDesc(GraduateFile::getGraduateTime).last("limit 1"));
        if (file == null) {
            return null;
        }
        return enrichFiles(List.of(file)).get(0);
    }

    private String nodeLabel(String node) {
        if (node == null) {
            return "";
        }
        return switch (node) {
            case "TEACHER" -> "教员初审";
            case "LEADER" -> "单位领导复审";
            case "ADMIN" -> "管理员终审";
            case "DONE" -> "已完成";
            case "REJECTED" -> "已驳回";
            default -> node;
        };
    }

    private String orgName(Long orgId) {
        if (orgId == null) {
            return null;
        }
        SysOrg org = orgMapper.selectById(orgId);
        return org == null ? null : org.getOrgName();
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getRealName();
    }
}
