package com.adultedu.service;

import cn.hutool.core.util.StrUtil;
import com.adultedu.common.BusinessException;
import com.adultedu.common.NotificationTemplates;
import com.adultedu.common.PageResult;
import com.adultedu.dto.BizDtos.VerifyReviewRequest;
import com.adultedu.entity.ApprovalLog;
import com.adultedu.entity.Course;
import com.adultedu.entity.CreditRecord;
import com.adultedu.entity.ProjectMember;
import com.adultedu.entity.ProjectTeacher;
import com.adultedu.entity.StudyHeartbeatLog;
import com.adultedu.entity.StudyRecord;
import com.adultedu.entity.SysOrg;
import com.adultedu.entity.SysUser;
import com.adultedu.entity.TrainingProject;
import com.adultedu.entity.UserVerifyLog;
import com.adultedu.mapper.ApprovalLogMapper;
import com.adultedu.mapper.CourseMapper;
import com.adultedu.mapper.CreditRecordMapper;
import com.adultedu.mapper.ProjectMemberMapper;
import com.adultedu.mapper.ProjectTeacherMapper;
import com.adultedu.mapper.StudyHeartbeatLogMapper;
import com.adultedu.mapper.StudyRecordMapper;
import com.adultedu.mapper.SysOrgMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.mapper.TrainingProjectMapper;
import com.adultedu.mapper.UserVerifyLogMapper;
import com.adultedu.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VerifyService {
    private final UserVerifyLogMapper verifyMapper;
    private final StudyRecordMapper studyMapper;
    private final StudyHeartbeatLogMapper heartbeatMapper;
    private final CourseMapper courseMapper;
    private final TrainingProjectMapper projectMapper;
    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;
    private final ProjectMemberMapper memberMapper;
    private final ProjectTeacherMapper teacherMapper;
    private final CreditRecordMapper creditMapper;
    private final ApprovalLogMapper approvalLogMapper;
    private final DataScopeService dataScopeService;
    private final StudyService studyService;
    private final NotificationService notificationService;

    public PageResult<Map<String, Object>> recordPage(LoginUser current, long pageNum, long pageSize,
                                                      Long projectId, Long courseId, Long userId,
                                                      Integer verifyResult, Integer status,
                                                      String startDate, String endDate) {
        LambdaQueryWrapper<UserVerifyLog> qw = new LambdaQueryWrapper<>();
        if (projectId != null) {
            qw.eq(UserVerifyLog::getProjectId, projectId);
        }
        if (courseId != null) {
            qw.eq(UserVerifyLog::getCourseId, courseId);
        }
        if (userId != null) {
            qw.eq(UserVerifyLog::getUserId, userId);
        }
        if (verifyResult != null) {
            qw.eq(UserVerifyLog::getVerifyResult, verifyResult);
        }
        if (status != null) {
            qw.eq(UserVerifyLog::getStatus, status);
        }
        if (StrUtil.isNotBlank(startDate)) {
            qw.ge(UserVerifyLog::getVerifyTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StrUtil.isNotBlank(endDate)) {
            qw.lt(UserVerifyLog::getVerifyTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        if (!applyScope(current, qw)) {
            return PageResult.of(List.of(), 0, pageNum, pageSize);
        }
        qw.orderByDesc(UserVerifyLog::getVerifyTime).orderByDesc(UserVerifyLog::getId);
        Page<UserVerifyLog> page = verifyMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(enrich(page.getRecords(), current), page.getTotal(), pageNum, pageSize);
    }

    /**
     * 按数据权限追加条件；返回 false 表示无可见数据。
     */
    private boolean applyScope(LoginUser current, LambdaQueryWrapper<UserVerifyLog> qw) {
        if (current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1)) {
            return true;
        }
        if ("student".equals(current.getRoleCode())) {
            qw.eq(UserVerifyLog::getUserId, current.getId());
            return true;
        }
        if ("teacher".equals(current.getRoleCode())) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .eq(Course::getCreateUserId, current.getId()).select(Course::getId))
                    .stream().map(Course::getId).toList();
            if (courseIds.isEmpty()) {
                return false;
            }
            qw.in(UserVerifyLog::getCourseId, courseIds);
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
        qw.in(UserVerifyLog::getUserId, userIds);
        return true;
    }

    private List<Map<String, Object>> enrich(List<UserVerifyLog> logs, LoginUser current) {
        if (logs.isEmpty()) {
            return List.of();
        }
        boolean hideImage = "leader".equals(current.getRoleCode()) || "student".equals(current.getRoleCode());
        List<Long> userIds = logs.stream().map(UserVerifyLog::getUserId).distinct().toList();
        List<Long> courseIds = logs.stream().map(UserVerifyLog::getCourseId).distinct().toList();
        List<Long> projectIds = logs.stream().map(UserVerifyLog::getProjectId).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        Map<Long, String> courses = courseIds.isEmpty() ? Map.of() : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getCourseName, (a, b) -> a));
        Map<Long, String> projects = projectIds.isEmpty() ? Map.of() : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(TrainingProject::getId, TrainingProject::getProjectName, (a, b) -> a));
        List<Map<String, Object>> result = new ArrayList<>(logs.size());
        for (UserVerifyLog log : logs) {
            SysUser u = users.get(log.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", log.getId());
            m.put("userId", log.getUserId());
            m.put("userName", u == null ? null : u.getRealName());
            m.put("username", u == null ? null : u.getUsername());
            m.put("orgName", u == null ? null : orgName(u.getOrgId() == null ? u.getDeptId() : u.getOrgId()));
            m.put("courseId", log.getCourseId());
            m.put("courseName", courses.get(log.getCourseId()));
            m.put("projectId", log.getProjectId());
            m.put("projectName", projects.get(log.getProjectId()));
            m.put("studyRecordId", log.getStudyRecordId());
            m.put("triggerPoint", log.getTriggerPoint());
            m.put("verifyTime", log.getVerifyTime());
            m.put("verifyType", log.getVerifyType());
            m.put("verifyTypeLabel", log.getVerifyType() != null && log.getVerifyType() == 2 ? "短信验证码" : "人脸比对");
            m.put("imgUrl", hideImage ? null : log.getImgUrl());
            m.put("verifyResult", log.getVerifyResult());
            m.put("verifyResultLabel", resultLabel(log.getVerifyResult()));
            m.put("status", log.getStatus());
            m.put("statusLabel", statusLabel(log.getStatus()));
            m.put("similarity", log.getSimilarity());
            m.put("failReason", log.getFailReason());
            m.put("reviewUserId", log.getReviewUserId());
            m.put("reviewTime", log.getReviewTime());
            m.put("reviewAction", log.getReviewAction());
            m.put("remark", log.getRemark());
            result.add(m);
        }
        return result;
    }

    private String resultLabel(Integer result) {
        if (result == null) {
            return "待核验";
        }
        return switch (result) {
            case 1 -> "通过";
            case 0 -> "失败";
            case 2 -> "超时";
            default -> "未知";
        };
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "待核验";
            case 1 -> "已通过";
            case 2 -> "未通过";
            case 3 -> "已超时";
            case 4 -> "已复核";
            default -> "未知";
        };
    }

    // ---------------- 学习监控 ----------------

    public PageResult<Map<String, Object>> monitorPage(LoginUser current, long pageNum, long pageSize,
                                                       Long projectId, Long courseId, Long userId, String keyword,
                                                       Integer abnormal, Integer status) {
        LambdaQueryWrapper<StudyRecord> qw = new LambdaQueryWrapper<>();
        if (projectId != null) {
            qw.eq(StudyRecord::getProjectId, projectId);
        }
        if (courseId != null) {
            qw.eq(StudyRecord::getCourseId, courseId);
        }
        if (userId != null) {
            qw.eq(StudyRecord::getUserId, userId);
        }
        if (abnormal != null) {
            qw.eq(StudyRecord::getIsAbnormal, abnormal);
        }
        if (status != null) {
            qw.eq(StudyRecord::getStatus, status);
        }
        if (StrUtil.isNotBlank(keyword)) {
            List<Long> matched = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, keyword).or().like(SysUser::getUsername, keyword))
                    .stream().map(SysUser::getId).toList();
            if (matched.isEmpty()) {
                return PageResult.of(List.of(), 0, pageNum, pageSize);
            }
            qw.in(StudyRecord::getUserId, matched);
        }
        if (!applyStudyScope(current, qw)) {
            return PageResult.of(List.of(), 0, pageNum, pageSize);
        }
        qw.orderByDesc(StudyRecord::getLastHeartbeat).orderByDesc(StudyRecord::getId);
        Page<StudyRecord> page = studyMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(enrichStudy(page.getRecords()), page.getTotal(), pageNum, pageSize);
    }

    private boolean applyStudyScope(LoginUser current, LambdaQueryWrapper<StudyRecord> qw) {
        if (current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1)) {
            return true;
        }
        if ("student".equals(current.getRoleCode())) {
            qw.eq(StudyRecord::getUserId, current.getId());
            return true;
        }
        if ("teacher".equals(current.getRoleCode())) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .eq(Course::getCreateUserId, current.getId()).select(Course::getId))
                    .stream().map(Course::getId).toList();
            if (courseIds.isEmpty()) {
                return false;
            }
            qw.in(StudyRecord::getCourseId, courseIds);
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
        qw.in(StudyRecord::getUserId, userIds);
        return true;
    }

    private List<Map<String, Object>> enrichStudy(List<StudyRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = records.stream().map(StudyRecord::getUserId).distinct().toList();
        List<Long> courseIds = records.stream().map(StudyRecord::getCourseId).distinct().toList();
        List<Long> projectIds = records.stream().map(StudyRecord::getProjectId).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        Map<Long, String> courses = courseIds.isEmpty() ? Map.of() : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getCourseName, (a, b) -> a));
        Map<Long, String> projects = projectIds.isEmpty() ? Map.of() : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(TrainingProject::getId, TrainingProject::getProjectName, (a, b) -> a));
        Map<Long, Integer> projectStatuses = projectIds.isEmpty() ? Map.of() : projectMapper.selectBatchIds(projectIds).stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.toMap(TrainingProject::getId, TrainingProject::getStatus, (a, b) -> a));
        LocalDateTime onlineLine = LocalDateTime.now().minusSeconds(120);
        List<Map<String, Object>> list = new ArrayList<>(records.size());
        for (StudyRecord r : records) {
            SysUser u = users.get(r.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            m.put("userName", u == null ? null : u.getRealName());
            m.put("username", u == null ? null : u.getUsername());
            m.put("orgName", u == null ? null : orgName(u.getOrgId() == null ? u.getDeptId() : u.getOrgId()));
            m.put("courseId", r.getCourseId());
            m.put("courseName", courses.get(r.getCourseId()));
            m.put("projectId", r.getProjectId());
            m.put("projectName", projects.get(r.getProjectId()));
            m.put("projectArchived", projectStatuses.get(r.getProjectId()) != null && projectStatuses.get(r.getProjectId()) == 3);
            m.put("startTime", r.getStartTime());
            m.put("endTime", r.getEndTime());
            m.put("validDuration", r.getValidDuration());
            m.put("progress", r.getProgress());
            m.put("status", r.getStatus());
            m.put("statusLabel", r.getStatus() != null && r.getStatus() == 2 ? "已完成" : "学习中");
            m.put("verifyCount", r.getVerifyCount());
            m.put("verifyTotal", r.getVerifyTotal());
            m.put("verifySuccess", r.getVerifySuccess());
            m.put("verifyFail", r.getVerifyFail());
            m.put("screenSwitchCount", r.getScreenSwitchCount());
            m.put("screenSwitchDuration", r.getScreenSwitchDuration());
            m.put("isAbnormal", r.getIsAbnormal());
            m.put("abnormalReason", r.getAbnormalReason());
            m.put("lastHeartbeat", r.getLastHeartbeat());
            m.put("online", r.getLastHeartbeat() != null && r.getLastHeartbeat().isAfter(onlineLine));
            m.put("lastPosition", r.getLastPosition());
            m.put("device", r.getDevice());
            m.put("ip", r.getIp());
            list.add(m);
        }
        return list;
    }

    public Map<String, Object> studyDetail(LoginUser current, Long studyRecordId) {
        StudyRecord record = studyMapper.selectById(studyRecordId);
        if (record == null) {
            throw new BusinessException("学习记录不存在");
        }
        if (!canViewStudy(current, record)) {
            throw new BusinessException(403, "无权查看该学习记录");
        }
        Course course = courseMapper.selectById(record.getCourseId());
        SysUser user = userMapper.selectById(record.getUserId());
        TrainingProject project = projectMapper.selectById(record.getProjectId());
        Map<String, Object> m = new LinkedHashMap<>();
        Map<String, Object> base = enrichStudy(List.of(record)).get(0);
        m.putAll(base);
        m.put("course", course);
        m.put("student", studentView(user));
        m.put("project", project);
        List<StudyHeartbeatLog> heartbeats = heartbeatMapper.selectList(new LambdaQueryWrapper<StudyHeartbeatLog>()
                .eq(StudyHeartbeatLog::getStudyRecordId, studyRecordId)
                .orderByDesc(StudyHeartbeatLog::getReportTime).last("limit 100"));
        m.put("heartbeats", heartbeats);
        List<UserVerifyLog> logs = verifyMapper.selectList(new LambdaQueryWrapper<UserVerifyLog>()
                .eq(UserVerifyLog::getStudyRecordId, studyRecordId)
                .orderByDesc(UserVerifyLog::getVerifyTime));
        m.put("verifyLogs", enrich(logs, current));
        m.put("creditRecords", creditMapper.selectList(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, record.getUserId())
                .eq(CreditRecord::getCourseId, record.getCourseId())
                .orderByDesc(CreditRecord::getCreateTime)));
        return m;
    }

    private boolean canViewStudy(LoginUser current, StudyRecord record) {
        if (current.getId().equals(record.getUserId())) {
            return true;
        }
        if (current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1)) {
            return true;
        }
        if ("teacher".equals(current.getRoleCode())) {
            Course course = courseMapper.selectById(record.getCourseId());
            return course != null && current.getId().equals(course.getCreateUserId());
        }
        SysUser user = userMapper.selectById(record.getUserId());
        return user != null && dataScopeService.canAccessOrg(current, user.getOrgId() == null ? user.getDeptId() : user.getOrgId());
    }

    // ---------------- 异常清单与复核 ----------------

    public PageResult<Map<String, Object>> abnormalPage(LoginUser current, long pageNum, long pageSize,
                                                        Long projectId, Long courseId, String keyword) {
        LambdaQueryWrapper<StudyRecord> qw = new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getIsAbnormal, 1);
        if (projectId != null) {
            qw.eq(StudyRecord::getProjectId, projectId);
        }
        if (courseId != null) {
            qw.eq(StudyRecord::getCourseId, courseId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            List<Long> matched = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, keyword).or().like(SysUser::getUsername, keyword))
                    .stream().map(SysUser::getId).toList();
            if (matched.isEmpty()) {
                return PageResult.of(List.of(), 0, pageNum, pageSize);
            }
            qw.in(StudyRecord::getUserId, matched);
        }
        if (!applyStudyScope(current, qw)) {
            return PageResult.of(List.of(), 0, pageNum, pageSize);
        }
        qw.orderByDesc(StudyRecord::getUpdateTime);
        Page<StudyRecord> page = studyMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> list = enrichStudy(page.getRecords());
        for (Map<String, Object> m : list) {
            Long recordId = (Long) m.get("id");
            UserVerifyLog latest = verifyMapper.selectOne(new LambdaQueryWrapper<UserVerifyLog>()
                    .eq(UserVerifyLog::getStudyRecordId, recordId)
                    .in(UserVerifyLog::getStatus, 0, 2, 3)
                    .orderByDesc(UserVerifyLog::getVerifyTime).last("limit 1"));
            m.put("latestVerify", latest);
            m.put("locked", studyService.isLockedPublic(recordId));
        }
        return PageResult.of(list, page.getTotal(), pageNum, pageSize);
    }

    @Transactional
    public void review(LoginUser current, Long logId, VerifyReviewRequest req) {
        UserVerifyLog log = verifyMapper.selectById(logId);
        if (log == null) {
            throw new BusinessException("核验记录不存在");
        }
        if (log.getStatus() != null && log.getStatus() == 4) {
            throw new BusinessException("该核验记录已复核，不能重复处理");
        }
        if (log.getStatus() == null || log.getStatus() == 0) {
            throw new BusinessException("该核验记录尚未完成，不能复核");
        }
        Course course = courseMapper.selectById(log.getCourseId());
        boolean admin = current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1);
        if (!admin && !("teacher".equals(current.getRoleCode()) && course != null
                && current.getId().equals(course.getCreateUserId()))) {
            throw new BusinessException(403, "仅管理员或该课程教员可复核核验记录");
        }
        TrainingProject project = course == null ? null : projectMapper.selectById(course.getProjectId());
        if (project != null && project.getStatus() != null && project.getStatus() == 3) {
            throw new BusinessException(403, "培训班《" + project.getProjectName() + "》已归档，仅可查看，不能复核核验记录");
        }
        String action = req.action() == null ? "" : req.action().trim().toLowerCase();
        if (!List.of("allow", "invalidate", "violate").contains(action)) {
            throw new BusinessException("复核操作仅支持 allow / invalidate / violate");
        }
        log.setStatus(4);
        log.setReviewUserId(current.getId());
        log.setReviewTime(LocalDateTime.now());
        log.setReviewAction(action);
        log.setRemark(req.remark());
        verifyMapper.updateById(log);
        StudyRecord record = log.getStudyRecordId() == null ? null : studyMapper.selectById(log.getStudyRecordId());
        String actionName = switch (action) {
            case "allow" -> "允许本次学时有效";
            case "invalidate" -> "作废本次学时";
            default -> "标记学员违规并取消课程学分";
        };
        if (record != null) {
            if ("allow".equals(action)) {
                studyService.unlock(record.getId(), true);
            } else if ("invalidate".equals(action)) {
                studyService.unlock(record.getId(), true);
                record.setIsAbnormal(0);
                record.setAbnormalReason(null);
                record.setValidDuration(0);
                record.setProgress(BigDecimal.ZERO);
                record.setLastPosition(0);
                record.setStatus(1);
                record.setEndTime(null);
                studyMapper.updateById(record);
                revokeCourseCredit(record, current, actionName, req.remark());
            } else {
                studyService.unlock(record.getId(), false);
                record.setIsAbnormal(1);
                studyMapper.updateById(record);
                // 违规结论需要重新锁定学习，避免学员继续刷课
                studyService.markAbnormal(record, "复核判定违规：" + StrUtil.blankToDefault(req.remark(), actionName));
                revokeCourseCredit(record, current, actionName, req.remark());
            }
        }
        ApprovalLog approval = new ApprovalLog();
        approval.setBizType("VERIFY");
        approval.setBizId(log.getId());
        approval.setNode("VERIFY");
        approval.setAuditorId(current.getId());
        approval.setAuditorName(current.getRealName());
        approval.setAction(action.toUpperCase());
        approval.setOpinion(StrUtil.blankToDefault(req.remark(), actionName));
        approvalLogMapper.insert(approval);
        notificationService.sendByTemplate(NotificationTemplates.VERIFY_REVIEWED, log.getUserId(),
                Map.of("courseName", course == null ? "" : course.getCourseName(),
                        "result", actionName + StrUtil.blankToDefault(req.remark(), "")),
                log.getId());
    }

    private void revokeCourseCredit(StudyRecord record, LoginUser current, String action, String remark) {
        CreditRecord credit = creditMapper.selectOne(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, record.getUserId())
                .eq(CreditRecord::getCourseId, record.getCourseId())
                .eq(CreditRecord::getAuditStatus, 2).last("limit 1"));
        if (credit == null) {
            return;
        }
        credit.setAuditStatus(4);
        credit.setRevokeReason(action + (StrUtil.isBlank(remark) ? "" : "：" + remark));
        credit.setRevokeUserId(current.getId());
        credit.setRevokeTime(LocalDateTime.now());
        creditMapper.updateById(credit);
    }

    public List<Map<String, Object>> myVerifyLogs(LoginUser current, Long courseId) {
        LambdaQueryWrapper<UserVerifyLog> qw = new LambdaQueryWrapper<UserVerifyLog>()
                .eq(UserVerifyLog::getUserId, current.getId());
        if (courseId != null) {
            qw.eq(UserVerifyLog::getCourseId, courseId);
        }
        qw.orderByDesc(UserVerifyLog::getVerifyTime);
        return enrich(verifyMapper.selectList(qw), current);
    }

    private String orgName(Long orgId) {
        if (orgId == null) {
            return null;
        }
        SysOrg org = orgMapper.selectById(orgId);
        return org == null ? null : org.getOrgName();
    }

    private Map<String, Object> studentView(SysUser user) {
        if (user == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("realName", user.getRealName());
        m.put("phone", maskPhone(user.getPhone()));
        m.put("idCard", maskIdCard(user.getIdCard()));
        m.put("orgId", user.getOrgId());
        m.put("deptId", user.getDeptId());
        m.put("orgName", orgName(user.getOrgId() == null ? user.getDeptId() : user.getOrgId()));
        m.put("userType", user.getUserType());
        return m;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }
}
