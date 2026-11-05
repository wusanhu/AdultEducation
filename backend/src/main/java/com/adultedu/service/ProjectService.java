package com.adultedu.service;

import cn.hutool.core.util.StrUtil;
import com.adultedu.common.BusinessException;
import com.adultedu.common.PageResult;
import com.adultedu.common.NotificationTemplates;
import com.adultedu.dto.BizDtos.MemberRequest;
import com.adultedu.dto.BizDtos.ProjectSaveRequest;
import com.adultedu.entity.Course;
import com.adultedu.entity.CreditRecord;
import com.adultedu.entity.ExamRecord;
import com.adultedu.entity.GraduateApply;
import com.adultedu.entity.ProjectMember;
import com.adultedu.entity.ProjectTeacher;
import com.adultedu.entity.StudyRecord;
import com.adultedu.entity.SysOrg;
import com.adultedu.entity.SysRole;
import com.adultedu.entity.SysUser;
import com.adultedu.entity.TrainingProject;
import com.adultedu.mapper.CourseMapper;
import com.adultedu.mapper.CreditRecordMapper;
import com.adultedu.mapper.ExamRecordMapper;
import com.adultedu.mapper.GraduateApplyMapper;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final TrainingProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final ProjectTeacherMapper teacherMapper;
    private final CourseMapper courseMapper;
    private final StudyRecordMapper studyRecordMapper;
    private final ExamRecordMapper examRecordMapper;
    private final CreditRecordMapper creditRecordMapper;
    private final GraduateApplyMapper graduateApplyMapper;
    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;
    private final SysRoleMapper roleMapper;
    private final DataScopeService dataScopeService;
    private final NotificationService notificationService;

    public PageResult<Map<String, Object>> projectPage(LoginUser current, long pageNum, long pageSize,
                                                       String keyword, Integer status) {
        Page<TrainingProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TrainingProject> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like(TrainingProject::getProjectName, keyword).or().like(TrainingProject::getProjectCode, keyword));
        }
        if (status != null) {
            qw.eq(TrainingProject::getStatus, status);
        }
        List<Long> visible = visibleProjectIds(current);
        if (visible != null) {
            if (visible.isEmpty()) {
                return PageResult.of(List.of(), 0, pageNum, pageSize);
            }
            qw.in(TrainingProject::getId, visible);
        }
        qw.orderByDesc(TrainingProject::getId);
        Page<TrainingProject> result = projectMapper.selectPage(page, qw);
        List<Map<String, Object>> records = result.getRecords().stream().map(p -> projectMap(p, current)).toList();
        return PageResult.of(records, result.getTotal(), pageNum, pageSize);
    }

    public List<Long> visibleProjectIds(LoginUser current) {
        if (current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1)) {
            return null;
        }
        if ("student".equals(current.getRoleCode())) {
            return memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                            .eq(ProjectMember::getUserId, current.getId()).eq(ProjectMember::getStatus, 1))
                    .stream().map(ProjectMember::getProjectId).distinct().toList();
        }
        if ("teacher".equals(current.getRoleCode())) {
            List<Long> assigned = teacherMapper.selectList(new LambdaQueryWrapper<ProjectTeacher>()
                            .eq(ProjectTeacher::getUserId, current.getId()))
                    .stream().map(ProjectTeacher::getProjectId).toList();
            List<Long> owned = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .eq(Course::getCreateUserId, current.getId()).select(Course::getProjectId))
                    .stream().map(Course::getProjectId).distinct().toList();
            List<Long> all = new ArrayList<>(assigned);
            all.addAll(owned);
            return all.stream().distinct().toList();
        }
        List<Long> orgIds = dataScopeService.accessibleOrgIds(current);
        if (orgIds == null) {
            return null;
        }
        if (orgIds.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .and(w -> w.in(SysUser::getOrgId, orgIds).or().in(SysUser::getDeptId, orgIds)))
                .stream().map(SysUser::getId).toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                        .in(ProjectMember::getUserId, userIds).eq(ProjectMember::getStatus, 1))
                .stream().map(ProjectMember::getProjectId).distinct().toList();
    }

    public Map<String, Object> projectMap(TrainingProject p, LoginUser current) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("projectName", p.getProjectName());
        map.put("projectCode", p.getProjectCode());
        map.put("description", p.getDescription());
        map.put("startTime", p.getStartTime());
        map.put("endTime", p.getEndTime());
        map.put("needCredit", p.getNeedCredit());
        map.put("status", p.getStatus());
        map.put("certificateTemplateUrl", p.getCertificateTemplateUrl());
        map.put("allowOverdue", p.getAllowOverdue());
        map.put("autoGraduate", p.getAutoGraduate());
        map.put("needTeacherAudit", p.getNeedTeacherAudit());
        map.put("needLeaderAudit", p.getNeedLeaderAudit());
        map.put("editable", !isArchived(p));
        map.put("createUserId", p.getCreateUserId());
        map.put("createTime", p.getCreateTime());
        map.put("memberCount", memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, p.getId()).eq(ProjectMember::getStatus, 1)));
        map.put("courseCount", courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getProjectId, p.getId()).eq(Course::getStatus, 1)));
        List<ProjectTeacher> teachers = teacherMapper.selectList(new LambdaQueryWrapper<ProjectTeacher>()
                .eq(ProjectTeacher::getProjectId, p.getId()));
        map.put("teachers", teachers.stream().map(t -> {
            SysUser u = userMapper.selectById(t.getUserId());
            return u == null ? Map.<String, Object>of() : Map.<String, Object>of("id", u.getId(), "realName", u.getRealName());
        }).filter(m -> !m.isEmpty()).toList());
        if (p.getCreateUserId() != null) {
            SysUser creator = userMapper.selectById(p.getCreateUserId());
            map.put("createUserName", creator == null ? null : creator.getRealName());
        }
        if (current != null && "student".equals(current.getRoleCode())) {
            map.put("credit", grantedCredits(current.getId(), p.getId()));
            map.put("progress", progress(grantedCredits(current.getId(), p.getId()), p.getNeedCredit()));
        }
        return map;
    }

    public Map<String, Object> projectDetail(Long id, LoginUser current) {
        TrainingProject p = projectMapper.selectById(id);
        if (p == null) {
            throw new BusinessException("培训班不存在");
        }
        assertCanViewProject(p, current);
        Map<String, Object> map = new LinkedHashMap<>(projectMap(p, current));
        // 学员/教务详情同样按周期推导展示状态
        map.put("status", effectiveStatus(p));
        List<Map<String, Object>> members = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, id).eq(ProjectMember::getStatus, 1))
                .stream().limit(20).map(m -> {
                    SysUser u = userMapper.selectById(m.getUserId());
                    if (u == null) {
                        return null;
                    }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("userId", u.getId());
                    item.put("realName", u.getRealName());
                    item.put("phone", u.getPhone());
                    item.put("orgName", orgName(u.getOrgId()));
                    item.put("credit", grantedCredits(u.getId(), id));
                    return item;
                }).filter(x -> x != null).toList();
        map.put("members", members);
        map.put("courses", courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getProjectId, id).orderByAsc(Course::getSort))
                .stream().map(c -> Map.<String, Object>of("id", c.getId(), "courseName", c.getCourseName(),
                        "credit", c.getCredit(), "status", c.getStatus(), "statusName", courseStatusName(c.getStatus())))
                .toList());
        return map;
    }

    private void assertCanViewProject(TrainingProject project, LoginUser user) {
        if (user.isSuperAdmin() || (user.getDataScope() != null && user.getDataScope() == 1)) {
            return;
        }
        List<Long> visible = visibleProjectIds(user);
        if (visible != null && !visible.contains(project.getId())) {
            throw new BusinessException(403, "无权查看该培训班数据");
        }
    }

    public boolean isArchived(TrainingProject project) {
        return project != null && project.getStatus() != null && project.getStatus() == 3;
    }

    /**
     * 展示用有效状态：归档优先；启用周期已过视为已结束；进行中但未到开始时间视为未开始。
     * status 是人工维护字段（0未开始/1进行中/2已结束/3已归档），周期到期不会自动翻转，
     * 读取展示时按时间推导，避免出现"已过期仍显示进行中"。
     */
    public int effectiveStatus(TrainingProject project) {
        Integer s = project.getStatus();
        if (s == null || s == 0) {
            return 0;
        }
        if (s == 3) {
            return 3;
        }
        LocalDateTime now = LocalDateTime.now();
        if (project.getEndTime() != null && now.isAfter(project.getEndTime())) {
            return 2;
        }
        if (s == 1 && project.getStartTime() != null && now.isBefore(project.getStartTime())) {
            return 0;
        }
        return s;
    }

    /**
     * 已归档培训班下的所有内容仅可查看，禁止任何新增、编辑、删除操作。
     */
    public void assertProjectEditable(Long projectId) {
        TrainingProject project = projectId == null ? null : projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        assertProjectEditable(project);
    }

    public void assertProjectEditable(TrainingProject project) {
        if (isArchived(project)) {
            throw new BusinessException(403, "培训班《" + project.getProjectName() + "》已归档，仅可查看，不能新增、编辑或删除");
        }
    }

    @Transactional
    public Long saveProject(ProjectSaveRequest req, LoginUser current) {
        if (!current.isSuperAdmin() && !"admin".equals(current.getRoleCode())) {
            throw new BusinessException(403, "仅管理员可维护培训班");
        }
        if (req.startTime().isAfter(req.endTime())) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }
        String code = StrUtil.trimToEmpty(req.projectCode());
        if (StrUtil.isBlank(code)) {
            throw new BusinessException("请填写项目编码");
        }
        if (!auditOn(req.needTeacherAudit()) && !auditOn(req.needLeaderAudit())) {
            throw new BusinessException("请至少保留一个毕业审批节点：教员初审或单位领导复审");
        }
        if (projectMapper.selectCount(new LambdaQueryWrapper<TrainingProject>()
                .eq(TrainingProject::getProjectCode, code)
                .ne(req.id() != null, TrainingProject::getId, req.id())) > 0) {
            throw new BusinessException("项目编码「" + code + "」已存在，请更换");
        }
        TrainingProject project = req.id() == null ? new TrainingProject() : projectMapper.selectById(req.id());
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        if (req.id() != null) {
            assertCanViewProject(project, current);
            assertProjectEditable(project);
            boolean reopening = req.status() != null && req.status() == 1
                    && (project.getStatus() == null || project.getStatus() != 1);
            if (reopening && !req.endTime().isAfter(LocalDateTime.now())) {
                throw new BusinessException("培训班启用周期已过期，重新启用需设置晚于当前时间的结束时间");
            }
        }
        project.setProjectName(req.projectName());
        project.setProjectCode(code);
        project.setDescription(req.description());
        project.setStartTime(req.startTime());
        project.setEndTime(req.endTime());
        project.setNeedCredit(req.needCredit());
        project.setStatus(req.status() == null ? 0 : req.status());
        project.setCertificateTemplateUrl(req.certificateTemplateUrl());
        project.setAllowOverdue(req.allowOverdue() == null ? 0 : req.allowOverdue());
        project.setAutoGraduate(req.autoGraduate() == null ? 0 : req.autoGraduate());
        project.setNeedTeacherAudit(auditOn(req.needTeacherAudit()) ? 1 : 0);
        project.setNeedLeaderAudit(auditOn(req.needLeaderAudit()) ? 1 : 0);
        try {
            if (req.id() == null) {
                project.setCreateUserId(current.getId());
                projectMapper.insert(project);
            } else {
                projectMapper.updateById(project);
            }
        } catch (DuplicateKeyException e) {
            // 逻辑删除的培训班仍占用 project_code 唯一索引，普通查重看不到它
            throw new BusinessException("项目编码「" + code + "」已被历史数据占用，请更换");
        }
        return project.getId();
    }

    private boolean auditOn(Integer flag) {
        return flag == null || flag == 1;
    }

    /** 新建培训班的默认编码：P + 年份 + '-' + 两位流水，按当年已用最大流水顺延 */
    public String nextProjectCode() {
        String prefix = "P" + java.time.LocalDate.now().getYear() + "-";
        int max = 0;
        for (TrainingProject p : projectMapper.selectList(new LambdaQueryWrapper<TrainingProject>()
                .select(TrainingProject::getProjectCode)
                .likeRight(TrainingProject::getProjectCode, prefix))) {
            String seq = p.getProjectCode().substring(prefix.length());
            if (seq.matches("\\d{1,4}")) {
                max = Math.max(max, Integer.parseInt(seq));
            }
        }
        return prefix + String.format("%02d", max + 1);
    }

    @Transactional
    public void deleteProject(Long id, LoginUser current) {
        TrainingProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        assertCanViewProject(project, current);
        assertProjectEditable(project);
        long courses = courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getProjectId, id));
        long members = memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>().eq(ProjectMember::getProjectId, id));
        if (courses > 0 || members > 0) {
            throw new BusinessException("培训班下已有课程或学员，禁止删除，请先归档");
        }
        teacherMapper.delete(new LambdaQueryWrapper<ProjectTeacher>().eq(ProjectTeacher::getProjectId, id));
        projectMapper.deleteById(id);
    }

    @Transactional
    public void updateProjectStatus(Long id, Integer status, LocalDateTime startTime, LocalDateTime endTime,
                                    LoginUser current) {
        TrainingProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        assertCanViewProject(project, current);
        if (status == null || status < 0 || status > 3) {
            throw new BusinessException("培训班状态不合法");
        }
        if (isArchived(project)) {
            throw new BusinessException(403, "培训班《" + project.getProjectName() + "》已归档，仅可查看，不能变更状态");
        }
        boolean reopening = status == 1 && (project.getStatus() == null || project.getStatus() != 1);
        boolean needNewPeriod = reopening && (project.getStatus() != null && project.getStatus() == 2
                || project.getEndTime() == null || project.getEndTime().isBefore(LocalDateTime.now()));
        if (needNewPeriod) {
            if (startTime == null || endTime == null) {
                throw new BusinessException("培训班已过期，重新启用需设置新的启用周期");
            }
            if (!startTime.isBefore(endTime)) {
                throw new BusinessException("启用开始时间不能晚于结束时间");
            }
            if (!endTime.isAfter(LocalDateTime.now())) {
                throw new BusinessException("启用结束时间必须晚于当前时间");
            }
            project.setStartTime(startTime);
            project.setEndTime(endTime);
        }
        project.setStatus(status);
        projectMapper.updateById(project);
        if (status == 1) {
            List<Long> userIds = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                            .eq(ProjectMember::getProjectId, id).eq(ProjectMember::getStatus, 1))
                    .stream().map(ProjectMember::getUserId).toList();
            notificationService.sendBatchByTemplate(NotificationTemplates.PROJECT_OPEN, userIds,
                    Map.of("projectName", project.getProjectName(), "projectId", id), id);
        }
    }

    // ==================== 学员 ====================
    public PageResult<Map<String, Object>> memberPage(Long projectId, long pageNum, long pageSize, String keyword,
                                                      LoginUser current) {
        TrainingProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        assertCanViewProject(project, current);
        List<ProjectMember> members = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getStatus, 1));
        List<Long> ids = members.stream().map(ProjectMember::getUserId).toList();
        if (ids.isEmpty()) {
            return PageResult.of(List.of(), 0, pageNum, pageSize);
        }
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>().in(SysUser::getId, ids);
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like(SysUser::getRealName, keyword).or().like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        qw.orderByAsc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Course::getProjectId, projectId))
                .stream().map(Course::getId).toList();
        List<Map<String, Object>> records = page.getRecords().stream().map(u -> memberMap(u, project, courseIds, members)).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    public Map<String, Object> memberMap(SysUser u, TrainingProject project, List<Long> courseIds, List<ProjectMember> members) {
        ProjectMember member = members.stream().filter(m -> m.getUserId().equals(u.getId())).findFirst().orElse(null);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("userId", u.getId());
        item.put("username", u.getUsername());
        item.put("realName", u.getRealName());
        item.put("phone", u.getPhone());
        item.put("idCard", u.getIdCard());
        item.put("orgName", orgName(u.getOrgId()));
        item.put("deptName", orgName(u.getDeptId()));
        item.put("joinTime", member == null ? null : member.getJoinTime());
        item.put("completionStatus", member == null ? 0 : member.getCompletionStatus());
        BigDecimal credit = grantedCredits(u.getId(), project.getId());
        item.put("credit", credit);
        item.put("needCredit", project.getNeedCredit());
        item.put("progress", progress(credit, project.getNeedCredit()));
        long completed = courseIds.isEmpty() ? 0 : studyRecordMapper.selectCount(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, u.getId()).in(StudyRecord::getCourseId, courseIds).eq(StudyRecord::getStatus, 2));
        item.put("completedCourse", completed);
        item.put("courseTotal", courseIds.size());
        long pendingCredit = creditRecordMapper.selectCount(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, u.getId()).eq(CreditRecord::getProjectId, project.getId())
                .eq(CreditRecord::getAuditStatus, 1));
        item.put("pendingCredit", pendingCredit);
        GraduateApply apply = latestApply(u.getId(), project.getId());
        item.put("graduateStatus", apply == null ? null : apply.getAuditStatus());
        item.put("graduateNode", apply == null ? null : apply.getCurrentNode());
        return item;
    }

    @Transactional
    public Map<String, Object> addMembers(Long projectId, List<Long> userIds, LoginUser current) {
        TrainingProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        assertCanViewProject(project, current);
        assertProjectEditable(project);
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("请选择学员");
        }
        int added = 0;
        for (Long userId : userIds) {
            SysUser user = userMapper.selectById(userId);
            if (user == null || (user.getUserType() != null && user.getUserType() != 4)) {
                continue;
            }
            if (!current.isSuperAdmin()
                    && !dataScopeService.canAccessOrg(current, user.getOrgId() == null ? user.getDeptId() : user.getOrgId())) {
                throw new BusinessException(403, "无权将其他单位的学员加入培训班");
            }
            ProjectMember existing = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, userId));
            if (existing != null) {
                if (existing.getStatus() != 1) {
                    existing.setStatus(1);
                    existing.setCompletionStatus(0);
                    memberMapper.updateById(existing);
                    notificationService.sendByTemplate(NotificationTemplates.PROJECT_REJOIN, userId,
                            Map.of("projectName", project.getProjectName(), "projectId", projectId), projectId);
                    added++;
                }
                continue;
            }
            ProjectMember member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(userId);
            member.setJoinTime(LocalDateTime.now());
            member.setStatus(1);
            member.setCompletionStatus(0);
            memberMapper.insert(member);
            notificationService.sendByTemplate(NotificationTemplates.PROJECT_JOIN, userId,
                    Map.of("projectName", project.getProjectName(), "projectId", projectId,
                            "needCredit", project.getNeedCredit()),
                    projectId);
            added++;
        }
        return Map.of("added", added);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, LoginUser current) {
        TrainingProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        assertCanViewProject(project, current);
        assertProjectEditable(project);
        ProjectMember member = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("该学员不在培训班中");
        }
        member.setStatus(0);
        memberMapper.updateById(member);
    }

    // ==================== 教员 ====================
    public List<Map<String, Object>> teacherList(Long projectId) {
        List<ProjectTeacher> links = teacherMapper.selectList(new LambdaQueryWrapper<ProjectTeacher>()
                .eq(ProjectTeacher::getProjectId, projectId));
        return links.stream().map(link -> {
            SysUser u = userMapper.selectById(link.getUserId());
            if (u == null) {
                return null;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("realName", u.getRealName());
            map.put("phone", u.getPhone());
            map.put("orgName", orgName(u.getOrgId()));
            map.put("courseCount", courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                    .eq(Course::getProjectId, projectId).eq(Course::getCreateUserId, u.getId())));
            return map;
        }).filter(x -> x != null).toList();
    }

    @Transactional
    public void assignTeachers(Long projectId, List<Long> userIds) {
        TrainingProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        assertProjectEditable(project);
        teacherMapper.delete(new LambdaQueryWrapper<ProjectTeacher>().eq(ProjectTeacher::getProjectId, projectId));
        if (userIds == null) {
            return;
        }
        for (Long userId : userIds) {
            SysUser user = userMapper.selectById(userId);
            if (user == null || (user.getUserType() != null && user.getUserType() != 3)) {
                continue;
            }
            ProjectTeacher link = new ProjectTeacher();
            link.setProjectId(projectId);
            link.setUserId(userId);
            link.setCreateTime(LocalDateTime.now());
            teacherMapper.insert(link);
            notificationService.sendByTemplate(NotificationTemplates.TEACHER_ASSIGN, userId,
                    Map.of("projectName", project.getProjectName(), "projectId", projectId), projectId);
        }
    }

    public boolean isProjectTeacher(Long projectId, Long userId) {
        return teacherMapper.selectCount(new LambdaQueryWrapper<ProjectTeacher>()
                .eq(ProjectTeacher::getProjectId, projectId).eq(ProjectTeacher::getUserId, userId)) > 0;
    }

    // ==================== 学员端 ====================
    public List<Map<String, Object>> myProjects(Long userId) {
        List<ProjectMember> members = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getUserId, userId).eq(ProjectMember::getStatus, 1)
                .orderByDesc(ProjectMember::getJoinTime));
        List<Map<String, Object>> list = new ArrayList<>();
        for (ProjectMember member : members) {
            TrainingProject project = projectMapper.selectById(member.getProjectId());
            if (project == null) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", project.getId());
            map.put("projectName", project.getProjectName());
            map.put("projectCode", project.getProjectCode());
            map.put("description", project.getDescription());
            map.put("startTime", project.getStartTime());
            map.put("endTime", project.getEndTime());
            map.put("needCredit", project.getNeedCredit());
            map.put("status", effectiveStatus(project));
            map.put("completionStatus", member.getCompletionStatus());
            BigDecimal credit = grantedCredits(userId, project.getId());
            map.put("credit", credit);
            map.put("progress", progress(credit, project.getNeedCredit()));
            long total = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                    .eq(Course::getProjectId, project.getId()).eq(Course::getStatus, 1));
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .eq(Course::getProjectId, project.getId()).eq(Course::getStatus, 1).select(Course::getId))
                    .stream().map(Course::getId).toList();
            long completed = courseIds.isEmpty() ? 0 : studyRecordMapper.selectCount(new LambdaQueryWrapper<StudyRecord>()
                    .eq(StudyRecord::getUserId, userId).in(StudyRecord::getCourseId, courseIds).eq(StudyRecord::getStatus, 2));
            // 学习中课程数：与 myDetail 的 studyStatus 同口径（StudyRecord.status=1）
            long learning = courseIds.isEmpty() ? 0 : studyRecordMapper.selectCount(new LambdaQueryWrapper<StudyRecord>()
                    .eq(StudyRecord::getUserId, userId).in(StudyRecord::getCourseId, courseIds).eq(StudyRecord::getStatus, 1));
            map.put("courseTotal", total);
            map.put("completedCourse", completed);
            map.put("learningCourse", learning);
            GraduateApply apply = latestApply(userId, project.getId());
            map.put("graduateStatus", apply == null ? null : apply.getAuditStatus());
            map.put("graduateNode", apply == null ? null : apply.getCurrentNode());
            BigDecimal needCredit = project.getNeedCredit();
            map.put("canApply", (needCredit == null || credit.compareTo(needCredit) >= 0)
                    && (apply == null || apply.getAuditStatus() == 2));
            list.add(map);
        }
        return list;
    }

    public Map<String, Object> myProjectDetail(Long projectId, Long userId) {
        ProjectMember member = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, userId).eq(ProjectMember::getStatus, 1));
        if (member == null) {
            throw new BusinessException(403, "您不在该培训班中");
        }
        TrainingProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("project", project);
        map.put("status", effectiveStatus(project));
        BigDecimal credit = grantedCredits(userId, projectId);
        map.put("credit", credit);
        map.put("needCredit", project.getNeedCredit());
        map.put("progress", progress(credit, project.getNeedCredit()));
        map.put("completionStatus", member.getCompletionStatus());
        GraduateApply apply = latestApply(userId, projectId);
        map.put("graduateApply", apply);
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getProjectId, projectId).eq(Course::getStatus, 1)
                .orderByAsc(Course::getIsRequired).orderByAsc(Course::getSort));
        List<Map<String, Object>> courseList = new ArrayList<>();
        for (Course course : courses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", course.getId());
            item.put("courseName", course.getCourseName());
            item.put("cover", course.getCover());
            item.put("credit", course.getCredit());
            item.put("studyHour", course.getStudyHour());
            item.put("isRequired", course.getIsRequired());
            item.put("needExam", course.getNeedExam());
            item.put("passScore", course.getPassScore());
            item.put("intro", course.getIntro());
            item.put("videoUrl", course.getVideoUrl());
            item.put("videoDuration", course.getVideoDuration());
            item.put("verifyGearCode", course.getVerifyGearCode());
            item.put("allowSeek", course.getAllowSeek());
            StudyRecord record = studyRecordMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                    .eq(StudyRecord::getUserId, userId).eq(StudyRecord::getCourseId, course.getId()));
            BigDecimal progress = record == null ? BigDecimal.ZERO : record.getProgress();
            item.put("progress", progress);
            item.put("studyStatus", record == null ? 0 : record.getStatus());
            item.put("lastPosition", record == null ? 0 : record.getLastPosition());
            ExamRecord exam = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getUserId, userId).eq(ExamRecord::getCourseId, course.getId())
                    .eq(ExamRecord::getIsPass, 1).orderByDesc(ExamRecord::getId).last("limit 1"));
            item.put("examPass", exam != null);
            item.put("examScore", exam == null ? null : exam.getScore());
            CreditRecord creditRecord = creditRecordMapper.selectOne(new LambdaQueryWrapper<CreditRecord>()
                    .eq(CreditRecord::getUserId, userId).eq(CreditRecord::getCourseId, course.getId())
                    .eq(CreditRecord::getAuditStatus, 2).last("limit 1"));
            item.put("creditIssued", creditRecord != null);
            courseList.add(item);
        }
        map.put("courses", courseList);
        return map;
    }

    // ==================== 工具 ====================
    public BigDecimal grantedCredits(Long userId, Long projectId) {
        List<CreditRecord> records = creditRecordMapper.selectList(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, userId).eq(CreditRecord::getProjectId, projectId)
                .eq(CreditRecord::getAuditStatus, 2));
        return records.stream().map(CreditRecord::getCreditValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal progress(BigDecimal credit, BigDecimal need) {
        if (need == null || need.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = credit.multiply(BigDecimal.valueOf(100)).divide(need, 2, RoundingMode.HALF_UP);
        return value.min(BigDecimal.valueOf(100));
    }

    public GraduateApply latestApply(Long userId, Long projectId) {
        return graduateApplyMapper.selectOne(new LambdaQueryWrapper<GraduateApply>()
                .eq(GraduateApply::getUserId, userId).eq(GraduateApply::getProjectId, projectId)
                .orderByDesc(GraduateApply::getId).last("limit 1"));
    }

    public String orgName(Long orgId) {
        if (orgId == null) {
            return null;
        }
        SysOrg org = orgMapper.selectById(orgId);
        return org == null ? null : org.getOrgName();
    }

    private String courseStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "已上架";
            default -> "已下架";
        };
    }

    public List<Map<String, Object>> projectOptions(LoginUser current) {
        LambdaQueryWrapper<TrainingProject> qw = new LambdaQueryWrapper<TrainingProject>()
                .ne(TrainingProject::getStatus, 3);
        List<Long> visible = visibleProjectIds(current);
        if (visible != null) {
            if (visible.isEmpty()) {
                return List.of();
            }
            qw.in(TrainingProject::getId, visible);
        }
        return projectMapper.selectList(qw.orderByDesc(TrainingProject::getId))
                .stream().map(p -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", p.getId());
                    map.put("projectName", p.getProjectName());
                    map.put("needCredit", p.getNeedCredit());
                    map.put("status", p.getStatus());
                    return map;
                }).toList();
    }

    public List<Map<String, Object>> teacherOptions(LoginUser current) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "teacher"));
        if (role == null) {
            return List.of();
        }
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, role.getId()).eq(SysUser::getStatus, 1);
        List<Long> orgIds = dataScopeService.accessibleOrgIds(current);
        if (orgIds != null) {
            if (orgIds.isEmpty()) {
                return List.of();
            }
            qw.and(w -> w.in(SysUser::getOrgId, orgIds).or().in(SysUser::getDeptId, orgIds));
        }
        return userMapper.selectList(qw)
                .stream().map(u -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", u.getId());
                    map.put("realName", u.getRealName());
                    map.put("username", u.getUsername());
                    map.put("orgName", orgName(u.getOrgId()));
                    return map;
                }).toList();
    }

    public List<Map<String, Object>> studentOptions(Long projectId, LoginUser current) {
        if (projectId != null) {
            TrainingProject project = projectMapper.selectById(projectId);
            if (project == null) {
                throw new BusinessException("培训班不存在");
            }
            assertCanViewProject(project, current);
        }
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserType, 4).eq(SysUser::getStatus, 1);
        // 教员按培训班授权：培训班可见性已校验，成员范围不再叠加组织数据权限
        boolean projectScoped = projectId != null && "teacher".equals(current.getRoleCode());
        List<Long> orgIds = dataScopeService.accessibleOrgIds(current);
        if (!projectScoped && orgIds != null) {
            if (orgIds.isEmpty()) {
                return List.of();
            }
            qw.and(w -> w.in(SysUser::getOrgId, orgIds).or().in(SysUser::getDeptId, orgIds));
        }
        if (projectId != null) {
            List<Long> ids = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                            .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getStatus, 1))
                    .stream().map(ProjectMember::getUserId).toList();
            if (ids.isEmpty()) {
                return List.of();
            }
            qw.in(SysUser::getId, ids);
        }
        return userMapper.selectList(qw).stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("realName", u.getRealName());
            map.put("username", u.getUsername());
            map.put("phone", u.getPhone());
            map.put("orgName", orgName(u.getOrgId()));
            return map;
        }).toList();
    }
}
