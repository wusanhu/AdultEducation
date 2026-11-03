package com.adultedu.service;

import cn.hutool.core.util.StrUtil;
import com.adultedu.common.BusinessException;
import com.adultedu.common.PageResult;
import com.adultedu.dto.BizDtos.CreditApplyRequest;
import com.adultedu.entity.ApprovalLog;
import com.adultedu.entity.Course;
import com.adultedu.entity.CreditRecord;
import com.adultedu.entity.ProjectMember;
import com.adultedu.entity.ProjectTeacher;
import com.adultedu.entity.StudyRecord;
import com.adultedu.entity.SysConfig;
import com.adultedu.entity.SysOrg;
import com.adultedu.entity.SysRole;
import com.adultedu.entity.SysUser;
import com.adultedu.entity.TrainingProject;
import com.adultedu.mapper.ApprovalLogMapper;
import com.adultedu.mapper.CourseMapper;
import com.adultedu.mapper.CreditRecordMapper;
import com.adultedu.mapper.ProjectMemberMapper;
import com.adultedu.mapper.ProjectTeacherMapper;
import com.adultedu.mapper.StudyRecordMapper;
import com.adultedu.mapper.SysConfigMapper;
import com.adultedu.mapper.SysOrgMapper;
import com.adultedu.mapper.SysRoleMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.mapper.TrainingProjectMapper;
import com.adultedu.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditService {
    private final CreditRecordMapper creditMapper;
    private final TrainingProjectMapper projectMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;
    private final SysRoleMapper roleMapper;
    private final SysConfigMapper configMapper;
    private final ProjectMemberMapper memberMapper;
    private final ProjectTeacherMapper teacherMapper;
    private final StudyRecordMapper studyRecordMapper;
    private final ApprovalLogMapper approvalLogMapper;
    private final DataScopeService dataScopeService;
    private final NotificationService notificationService;
    private final StringRedisTemplate redis;

    public PageResult<Map<String, Object>> ledgerPage(LoginUser current, long pageNum, long pageSize,
                                                      String keyword, Long userId, Long projectId, Long courseId,
                                                      Integer auditStatus, Integer sourceType,
                                                      String startDate, String endDate) {
        LambdaQueryWrapper<CreditRecord> qw = new LambdaQueryWrapper<>();
        if (userId != null) {
            if (!canSeeUser(current, userId)) {
                throw new BusinessException(403, "无权查看该学员学分");
            }
            qw.eq(CreditRecord::getUserId, userId);
        } else {
            List<Long> visible = visibleUserIds(current);
            if (visible != null) {
                if (visible.isEmpty()) {
                    return PageResult.of(List.of(), 0, pageNum, pageSize);
                }
                qw.in(CreditRecord::getUserId, visible);
            }
        }
        if (projectId != null) {
            qw.eq(CreditRecord::getProjectId, projectId);
        }
        if (courseId != null) {
            qw.eq(CreditRecord::getCourseId, courseId);
        }
        if (auditStatus != null) {
            qw.eq(CreditRecord::getAuditStatus, auditStatus);
        }
        if (sourceType != null) {
            qw.eq(CreditRecord::getSourceType, sourceType);
        }
        if (StrUtil.isNotBlank(startDate)) {
            qw.ge(CreditRecord::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StrUtil.isNotBlank(endDate)) {
            qw.lt(CreditRecord::getCreateTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        if (StrUtil.isNotBlank(keyword)) {
            List<Long> matched = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, keyword).or().like(SysUser::getUsername, keyword))
                    .stream().map(SysUser::getId).toList();
            if (matched.isEmpty()) {
                return PageResult.of(List.of(), 0, pageNum, pageSize);
            }
            qw.in(CreditRecord::getUserId, matched);
        }
        qw.orderByDesc(CreditRecord::getCreateTime).orderByDesc(CreditRecord::getId);
        Page<CreditRecord> page = creditMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(enrich(page.getRecords()), page.getTotal(), pageNum, pageSize);
    }

    private List<Map<String, Object>> enrich(List<CreditRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = records.stream().map(CreditRecord::getUserId).distinct().toList();
        List<Long> projectIds = records.stream().map(CreditRecord::getProjectId).distinct().toList();
        List<Long> courseIds = records.stream().map(CreditRecord::getCourseId).filter(java.util.Objects::nonNull).distinct().toList();
        List<Long> auditIds = records.stream().flatMap(r -> java.util.stream.Stream.of(r.getAuditUserId(), r.getApplyUserId(), r.getRevokeUserId()))
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? new HashMap<>() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        Map<Long, String> projectNames = projectIds.isEmpty() ? new HashMap<>() : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(TrainingProject::getId, TrainingProject::getProjectName, (a, b) -> a));
        Map<Long, String> courseNames = courseIds.isEmpty() ? new HashMap<>() : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getCourseName, (a, b) -> a));
        Map<Long, String> names = auditIds.isEmpty() ? new HashMap<>() : userMapper.selectBatchIds(auditIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        List<Map<String, Object>> result = new ArrayList<>(records.size());
        for (CreditRecord r : records) {
            Map<String, Object> m = new LinkedHashMap<>();
            SysUser u = users.get(r.getUserId());
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            m.put("userName", u == null ? null : u.getRealName());
            m.put("username", u == null ? null : u.getUsername());
            m.put("orgName", u == null ? null : orgName(u.getOrgId() == null ? u.getDeptId() : u.getOrgId()));
            m.put("projectId", r.getProjectId());
            m.put("projectName", projectNames.get(r.getProjectId()));
            m.put("courseId", r.getCourseId());
            m.put("courseName", courseNames.get(r.getCourseId()));
            m.put("creditValue", r.getCreditValue());
            m.put("sourceType", r.getSourceType());
            m.put("sourceLabel", r.getSourceType() != null && r.getSourceType() == 1 ? "课程自动" : "人工补录");
            m.put("auditStatus", r.getAuditStatus());
            m.put("auditStatusLabel", statusLabel(r.getAuditStatus()));
            m.put("auditNode", r.getAuditNode());
            m.put("auditUserId", r.getAuditUserId());
            m.put("auditUserName", names.get(r.getAuditUserId()));
            m.put("auditTime", r.getAuditTime());
            m.put("auditRemark", r.getAuditRemark());
            m.put("applyUserId", r.getApplyUserId());
            m.put("applyUserName", names.get(r.getApplyUserId()));
            m.put("remark", r.getRemark());
            m.put("revokeReason", r.getRevokeReason());
            m.put("revokeUserName", names.get(r.getRevokeUserId()));
            m.put("revokeTime", r.getRevokeTime());
            m.put("createTime", r.getCreateTime());
            result.add(m);
        }
        return result;
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 1 -> "待发放";
            case 2 -> "已发放";
            case 3 -> "已驳回";
            case 4 -> "已撤销";
            default -> "未知";
        };
    }

    /**
     * 可见的学员 ID 集合，null 表示不限。
     */
    public List<Long> visibleUserIds(LoginUser current) {
        if (current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1)) {
            return null;
        }
        if ("student".equals(current.getRoleCode())) {
            return List.of(current.getId());
        }
        if ("teacher".equals(current.getRoleCode())) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .eq(Course::getCreateUserId, current.getId()).select(Course::getId))
                    .stream().map(Course::getId).toList();
            List<Long> userIds = new ArrayList<>();
            if (!courseIds.isEmpty()) {
                userIds.addAll(studyRecordMapper.selectList(new LambdaQueryWrapper<StudyRecord>()
                                .in(StudyRecord::getCourseId, courseIds).select(StudyRecord::getUserId))
                        .stream().map(StudyRecord::getUserId).distinct().toList());
            }
            List<Long> projectIds = teacherMapper.selectList(new LambdaQueryWrapper<ProjectTeacher>()
                            .eq(ProjectTeacher::getUserId, current.getId()).select(ProjectTeacher::getProjectId))
                    .stream().map(ProjectTeacher::getProjectId).toList();
            if (!projectIds.isEmpty()) {
                userIds.addAll(memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                                .in(ProjectMember::getProjectId, projectIds).eq(ProjectMember::getStatus, 1)
                                .select(ProjectMember::getUserId))
                        .stream().map(ProjectMember::getUserId).toList());
            }
            return userIds.stream().distinct().toList();
        }
        List<Long> orgIds = dataScopeService.accessibleOrgIds(current);
        if (orgIds == null) {
            return null;
        }
        if (orgIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .and(w -> w.in(SysUser::getOrgId, orgIds).or().in(SysUser::getDeptId, orgIds))
                        .select(SysUser::getId))
                .stream().map(SysUser::getId).toList();
    }

    public boolean canSeeUser(LoginUser current, Long userId) {
        if (current.getId().equals(userId)) {
            return true;
        }
        List<Long> visible = visibleUserIds(current);
        return visible == null || visible.contains(userId);
    }

    public BigDecimal issuedTotal(Long userId, Long projectId) {
        LambdaQueryWrapper<CreditRecord> qw = new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, userId).eq(CreditRecord::getAuditStatus, 2);
        if (projectId != null) {
            qw.eq(CreditRecord::getProjectId, projectId);
        }
        return creditMapper.selectList(qw).stream().map(CreditRecord::getCreditValue)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal projectCreditTotal(Long userId, Long projectId) {
        List<CreditRecord> list = creditMapper.selectList(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, userId).eq(CreditRecord::getProjectId, projectId)
                .in(CreditRecord::getAuditStatus, 2));
        return list.stream().map(CreditRecord::getCreditValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 课程完成且测验通过后自动发放学分（幂等）。
     */
    @Transactional
    public CreditRecord awardCourseCredit(Long userId, Long projectId, Long courseId, BigDecimal creditValue, String remark) {
        String lockKey = "lock:credit:award:" + userId + ":" + projectId + ":" + courseId;
        boolean locked = Boolean.TRUE.equals(redis.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(15)));
        if (!locked) {
            // 另一个请求正在发放，等待其提交后复用结果，避免重复发放
            for (int i = 0; i < 10; i++) {
                CreditRecord current = findAwarded(userId, projectId, courseId);
                if (current != null) {
                    return current;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return findAwarded(userId, projectId, courseId);
        }
        try {
            CreditRecord exists = findAwarded(userId, projectId, courseId);
            if (exists != null) {
                return exists;
            }
            CreditRecord record = new CreditRecord();
            record.setUserId(userId);
            record.setProjectId(projectId);
            record.setCourseId(courseId);
            record.setCreditValue(creditValue);
            record.setSourceType(1);
            record.setAuditStatus(2);
            record.setAuditNode("DONE");
            record.setAuditTime(LocalDateTime.now());
            record.setAuditRemark("系统自动发放");
            record.setRemark(remark);
            try {
                creditMapper.insert(record);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                return findAwarded(userId, projectId, courseId);
            }
            Course course = courseMapper.selectById(courseId);
            notificationService.send(userId, "学分已到账",
                    "您已完成课程《" + (course == null ? "" : course.getCourseName()) + "》，获得 " + creditValue + " 学分。",
                    "COURSE", courseId);
            return record;
        } finally {
            releaseLockAfterCompletion(lockKey);
        }
    }

    /**
     * 事务提交后再释放锁，避免并发请求在数据可见前拿到锁造成重复发放。
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

    private CreditRecord findAwarded(Long userId, Long projectId, Long courseId) {
        return creditMapper.selectOne(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, userId).eq(CreditRecord::getProjectId, projectId)
                .eq(CreditRecord::getCourseId, courseId).eq(CreditRecord::getSourceType, 1)
                .eq(CreditRecord::getAuditStatus, 2).last("limit 1"));
    }

    @Transactional
    public Long apply(LoginUser current, CreditApplyRequest req) {
        SysUser student = userMapper.selectById(req.userId());
        if (student == null) {
            throw new BusinessException("学员不存在");
        }
        TrainingProject project = projectMapper.selectById(req.projectId());
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        if (memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, req.projectId()).eq(ProjectMember::getUserId, req.userId())
                .eq(ProjectMember::getStatus, 1)) == 0) {
            throw new BusinessException("该学员不在所选培训班中");
        }
        if (!canApplyForProject(current, req.projectId(), req.userId())) {
            throw new BusinessException(403, "无权为该学员补录学分");
        }
        CreditRecord record = new CreditRecord();
        record.setUserId(req.userId());
        record.setProjectId(req.projectId());
        record.setCreditValue(req.creditValue());
        record.setSourceType(2);
        record.setAuditStatus(1);
        record.setAuditNode(needLeaderAudit() ? "LEADER" : "ADMIN");
        record.setApplyUserId(current.getId());
        record.setRemark(req.remark());
        creditMapper.insert(record);
        writeLog("CREDIT", record.getId(), "SUBMIT", current, req.remark());
        notifyApprovers(record, current.getId(), record.getAuditNode());
        return record.getId();
    }

    private boolean canApplyForProject(LoginUser current, Long projectId, Long userId) {
        if (current.isSuperAdmin()) {
            return true;
        }
        if ("teacher".equals(current.getRoleCode())) {
            return teacherMapper.selectCount(new LambdaQueryWrapper<ProjectTeacher>()
                    .eq(ProjectTeacher::getProjectId, projectId).eq(ProjectTeacher::getUserId, current.getId())) > 0
                    || courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                    .eq(Course::getProjectId, projectId).eq(Course::getCreateUserId, current.getId())) > 0;
        }
        if ("leader".equals(current.getRoleCode())) {
            return canSeeUser(current, userId);
        }
        return false;
    }

    private void notifyApprovers(CreditRecord record, Long applyUserId, String node) {
        String roleCode = "LEADER".equals(node) ? "leader" : "super_admin";
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role == null) {
            return;
        }
        SysUser student = userMapper.selectById(record.getUserId());
        String title = "LEADER".equals(node) ? "学分补录申请待领导复核" : "学分补录申请待管理员终审";
        List<Long> approvers = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleId, role.getId()).eq(SysUser::getStatus, 1))
                .stream().map(SysUser::getId).filter(id -> !id.equals(applyUserId)).toList();
        notificationService.sendBatch(approvers, title,
                "学员" + (student == null ? "" : student.getRealName()) + "申请补录 " + record.getCreditValue()
                        + " 学分，原因：" + record.getRemark(), "APPROVAL", record.getId());
    }

    @Transactional
    public void audit(LoginUser current, Long id, boolean approve, String remark) {
        CreditRecord record = creditMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("学分记录不存在");
        }
        if (record.getAuditStatus() == null || record.getAuditStatus() != 1) {
            throw new BusinessException("该申请已处理，不能重复审批");
        }
        boolean admin = current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1);
        if (!admin) {
            if (!"leader".equals(current.getRoleCode())) {
                throw new BusinessException(403, "无权审批学分补录");
            }
            if (!"LEADER".equals(record.getAuditNode())) {
                throw new BusinessException(403, "当前节点无需领导审批");
            }
            if (!canSeeUser(current, record.getUserId())) {
                throw new BusinessException(403, "无权审批其他单位学员的申请");
            }
        }
        String node = StrUtil.blankToDefault(record.getAuditNode(), "ADMIN");
        if (!approve) {
            record.setAuditStatus(3);
            record.setAuditNode("REJECTED");
            record.setAuditUserId(current.getId());
            record.setAuditTime(LocalDateTime.now());
            record.setAuditRemark(remark);
            creditMapper.updateById(record);
            writeLog("CREDIT", id, "REJECT", current, StrUtil.blankToDefault(remark, "审批驳回"));
            notificationService.send(record.getUserId(), "学分补录被驳回",
                    "您申请的 " + record.getCreditValue() + " 学分补录被驳回。原因：" + StrUtil.blankToDefault(remark, "未填写"), "APPROVAL", id);
            return;
        }
        if ("LEADER".equals(node) && !admin) {
            record.setAuditNode("ADMIN");
            record.setAuditUserId(current.getId());
            record.setAuditTime(LocalDateTime.now());
            record.setAuditRemark(remark);
            creditMapper.updateById(record);
            writeLog("CREDIT", id, "APPROVE", current, StrUtil.blankToDefault(remark, "领导复核通过，转管理员终审"));
            notifyApprovers(record, current.getId(), record.getAuditNode());
            return;
        }
        record.setAuditStatus(2);
        record.setAuditNode("DONE");
        record.setAuditUserId(current.getId());
        record.setAuditTime(LocalDateTime.now());
        record.setAuditRemark(remark);
        creditMapper.updateById(record);
        writeLog("CREDIT", id, "APPROVE", current, StrUtil.blankToDefault(remark, "审批通过"));
        notificationService.send(record.getUserId(), "学分补录已通过",
                "您申请的 " + record.getCreditValue() + " 学分补录已通过并入账。", "APPROVAL", id);
    }

    @Transactional
    public void revoke(LoginUser current, Long id, String reason) {
        CreditRecord record = creditMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("学分记录不存在");
        }
        if (record.getAuditStatus() == null || record.getAuditStatus() != 2) {
            throw new BusinessException("仅已发放的学分可以撤销");
        }
        if (!current.isSuperAdmin()) {
            throw new BusinessException(403, "仅管理员可撤销学分");
        }
        record.setAuditStatus(4);
        record.setRevokeReason(reason);
        record.setRevokeUserId(current.getId());
        record.setRevokeTime(LocalDateTime.now());
        creditMapper.updateById(record);
        writeLog("CREDIT", id, "REVOKE", current, reason);
        notificationService.send(record.getUserId(), "学分已撤销",
                "您的 " + record.getCreditValue() + " 学分已被撤销。原因：" + reason, "WARNING", id);
    }

    public Map<String, Object> summary(LoginUser current) {
        List<CreditRecord> records = creditMapper.selectList(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, current.getId()));
        BigDecimal issued = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;
        for (CreditRecord r : records) {
            if (r.getAuditStatus() != null && r.getAuditStatus() == 2) {
                issued = issued.add(r.getCreditValue());
            } else if (r.getAuditStatus() != null && r.getAuditStatus() == 1) {
                pending = pending.add(r.getCreditValue());
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issued", issued);
        m.put("pending", pending);
        m.put("count", records.size());
        return m;
    }

    public List<Map<String, Object>> myCredits(LoginUser current) {
        List<CreditRecord> records = creditMapper.selectList(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, current.getId())
                .orderByDesc(CreditRecord::getCreateTime));
        return enrich(records);
    }

    public List<Map<String, Object>> pendingAudits(LoginUser current) {
        boolean admin = current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1);
        boolean leader = "leader".equals(current.getRoleCode());
        if (!admin && !leader) {
            return List.of();
        }
        List<CreditRecord> records = creditMapper.selectList(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getAuditStatus, 1)
                .orderByAsc(CreditRecord::getCreateTime));
        if (leader && !admin) {
            records = records.stream().filter(r -> "LEADER".equals(r.getAuditNode())).toList();
        }
        List<Long> visible = visibleUserIds(current);
        if (visible != null) {
            records = records.stream().filter(r -> visible.contains(r.getUserId())).toList();
        }
        return enrich(records);
    }

    private boolean needLeaderAudit() {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, "credit.needLeaderAudit"));
        return config != null && "true".equalsIgnoreCase(config.getConfigValue());
    }

    private void writeLog(String bizType, Long bizId, String action, LoginUser current, String opinion) {
        ApprovalLog log = new ApprovalLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setNode("CREDIT");
        log.setAuditorId(current.getId());
        log.setAuditorName(current.getRealName());
        log.setAction(action);
        log.setOpinion(opinion);
        approvalLogMapper.insert(log);
    }

    private String orgName(Long orgId) {
        if (orgId == null) {
            return null;
        }
        SysOrg org = orgMapper.selectById(orgId);
        return org == null ? null : org.getOrgName();
    }
}
