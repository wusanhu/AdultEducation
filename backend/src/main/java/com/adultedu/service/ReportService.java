package com.adultedu.service;

import cn.hutool.core.util.StrUtil;
import com.adultedu.common.BusinessException;
import com.adultedu.entity.Course;
import com.adultedu.entity.CreditRecord;
import com.adultedu.entity.ExamRecord;
import com.adultedu.entity.GraduateApply;
import com.adultedu.entity.GraduateFile;
import com.adultedu.entity.ProjectMember;
import com.adultedu.entity.StudyRecord;
import com.adultedu.entity.SysOrg;
import com.adultedu.entity.SysUser;
import com.adultedu.entity.TrainingProject;
import com.adultedu.entity.UserVerifyLog;
import com.adultedu.mapper.CourseMapper;
import com.adultedu.mapper.CreditRecordMapper;
import com.adultedu.mapper.ExamRecordMapper;
import com.adultedu.mapper.GraduateApplyMapper;
import com.adultedu.mapper.GraduateFileMapper;
import com.adultedu.mapper.ProjectMemberMapper;
import com.adultedu.mapper.StudyHeartbeatLogMapper;
import com.adultedu.mapper.StudyRecordMapper;
import com.adultedu.mapper.SysOrgMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.mapper.TrainingProjectMapper;
import com.adultedu.mapper.UserVerifyLogMapper;
import com.adultedu.security.LoginUser;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final TrainingProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final StudyRecordMapper studyMapper;
    private final StudyHeartbeatLogMapper heartbeatMapper;
    private final CreditRecordMapper creditMapper;
    private final GraduateApplyMapper graduateApplyMapper;
    private final GraduateFileMapper graduateFileMapper;
    private final CourseMapper courseMapper;
    private final ExamRecordMapper examMapper;
    private final UserVerifyLogMapper verifyMapper;
    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;
    private final ProjectService projectService;
    private final CreditService creditService;
    private final NotificationService notificationService;
    private final DataScopeService dataScope;

    // ---------------- 工作台 ----------------

    public Map<String, Object> overview(LoginUser current) {
        List<Long> projectIds = projectService.visibleProjectIds(current);
        List<Long> userIds = visibleUserIds(current);
        List<Long> courseIds = visibleCourseIds(current, projectIds);
        Map<String, Object> m = new LinkedHashMap<>();
        boolean unrestricted = projectIds == null;

        List<TrainingProject> projects = projectMapper.selectList(new LambdaQueryWrapper<TrainingProject>()
                .eq(!unrestricted, TrainingProject::getId, -1));
        if (projectIds != null) {
            projects = projectIds.isEmpty() ? List.of() : projectMapper.selectBatchIds(projectIds);
        }
        m.put("projectCount", projects.size());
        m.put("activeProjectCount", projects.stream().filter(p -> p.getStatus() != null && p.getStatus() == 1).count());

        LambdaQueryWrapper<SysUser> userQw = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserType, 4).eq(SysUser::getStatus, 1);
        if (userIds != null) {
            if (userIds.isEmpty()) {
                userQw.eq(SysUser::getId, -1);
            } else {
                userQw.in(SysUser::getId, userIds);
            }
        }
        long studentCount = userMapper.selectCount(userQw);
        m.put("studentCount", studentCount);

        LambdaQueryWrapper<Course> courseQw = new LambdaQueryWrapper<>();
        if (courseIds != null) {
            if (courseIds.isEmpty()) {
                courseQw.eq(Course::getId, -1);
            } else {
                courseQw.in(Course::getId, courseIds);
            }
        }
        m.put("courseCount", courseMapper.selectCount(courseQw));

        List<StudyRecord> studies = studyRecords(userIds, projectIds, courseIds);
        m.put("studyCount", studies.size());
        m.put("completedStudyCount", studies.stream().filter(s -> s.getStatus() != null && s.getStatus() == 2).count());
        m.put("validHours", BigDecimal.valueOf(studies.stream()
                        .mapToInt(s -> s.getValidDuration() == null ? 0 : s.getValidDuration()).sum())
                .divide(BigDecimal.valueOf(3600), 1, RoundingMode.HALF_UP));

        List<CreditRecord> credits = creditRecords(userIds, projectIds, courseIds);
        BigDecimal total = credits.stream().filter(c -> c.getAuditStatus() != null && c.getAuditStatus() == 2)
                .map(CreditRecord::getCreditValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put("creditTotal", total);
        m.put("creditIssuedCount", credits.stream().filter(c -> c.getAuditStatus() != null && c.getAuditStatus() == 2).count());
        m.put("pendingCreditCount", credits.stream().filter(c -> c.getAuditStatus() != null && c.getAuditStatus() == 1).count());
        m.put("graduateCount", graduateFiles(userIds, projectIds).size());

        LambdaQueryWrapper<GraduateApply> applyQw = new LambdaQueryWrapper<GraduateApply>()
                .eq(GraduateApply::getAuditStatus, 0);
        if (userIds != null) {
            if (userIds.isEmpty()) {
                applyQw.eq(GraduateApply::getUserId, -1);
            } else {
                applyQw.in(GraduateApply::getUserId, userIds);
            }
        }
        if (projectIds != null) {
            if (projectIds.isEmpty()) {
                applyQw.eq(GraduateApply::getProjectId, -1);
            } else {
                applyQw.in(GraduateApply::getProjectId, projectIds);
            }
        }
        m.put("pendingGraduateCount", graduateApplyMapper.selectCount(applyQw));

        List<UserVerifyLog> verifyLogs = verifyLogs(userIds, projectIds, courseIds);
        long done = verifyLogs.stream().filter(v -> v.getVerifyResult() != null).count();
        long passed = verifyLogs.stream().filter(v -> v.getVerifyResult() != null && v.getVerifyResult() == 1).count();
        m.put("verifyTotal", done);
        m.put("verifyPassRate", done == 0 ? 0 : BigDecimal.valueOf(passed * 100.0 / done).setScale(1, RoundingMode.HALF_UP));
        m.put("abnormalCount", studies.stream().filter(s -> s.getIsAbnormal() != null && s.getIsAbnormal() == 1).count());

        m.put("unreadCount", notificationService.unread(current.getId()));
        m.put("trend", trend(userIds, projectIds, courseIds));
        m.put("recentGraduate", recentGraduate(userIds, projectIds));
        m.put("recentCredits", credits.stream()
                .filter(c -> c.getAuditStatus() != null && c.getAuditStatus() == 2)
                .sorted((a, b) -> {
                    LocalDateTime x = a.getAuditTime() == null ? a.getCreateTime() : a.getAuditTime();
                    LocalDateTime y = b.getAuditTime() == null ? b.getCreateTime() : b.getAuditTime();
                    return y == null ? 1 : (x == null ? -1 : y.compareTo(x));
                }).limit(6)
                .map(this::creditLite).toList());
        return m;
    }

    private Map<String, Object> creditLite(CreditRecord c) {
        SysUser u = userMapper.selectById(c.getUserId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("userName", u == null ? null : u.getRealName());
        m.put("creditValue", c.getCreditValue());
        m.put("auditTime", c.getAuditTime() == null ? c.getCreateTime() : c.getAuditTime());
        m.put("sourceLabel", c.getSourceType() != null && c.getSourceType() == 1 ? "课程自动" : "人工补录");
        return m;
    }

    private List<Map<String, Object>> trend(List<Long> userIds, List<Long> projectIds, List<Long> courseIds) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<CreditRecord> credits = creditRecords(userIds, projectIds, courseIds);
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            BigDecimal creditSum = credits.stream()
                    .filter(c -> c.getAuditStatus() != null && c.getAuditStatus() == 2)
                    .filter(c -> c.getAuditTime() != null && c.getAuditTime().toLocalDate().equals(day))
                    .map(CreditRecord::getCreditValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", day.toString());
            row.put("credits", creditSum);
            result.add(row);
        }
        QueryWrapper<StudyRecord> qw = new QueryWrapper<StudyRecord>()
                .select("DATE(last_heartbeat) AS d", "SUM(valid_duration) AS sec")
                .ge("last_heartbeat", today.minusDays(6).atStartOfDay())
                .groupBy("DATE(last_heartbeat)");
        if (userIds != null) {
            if (userIds.isEmpty()) {
                qw.eq("user_id", -1);
            } else {
                qw.in("user_id", userIds);
            }
        }
        if (projectIds != null) {
            if (projectIds.isEmpty()) {
                qw.eq("project_id", -1);
            } else {
                qw.in("project_id", projectIds);
            }
        }
        Map<String, Long> studyMap = new HashMap<>();
        for (Map<String, Object> row : studyMapper.selectMaps(qw)) {
            Object d = row.get("d");
            Object sec = row.get("sec");
            if (d != null) {
                studyMap.put(d.toString(), sec == null ? 0L : ((Number) sec).longValue() / 60);
            }
        }
        for (Map<String, Object> row : result) {
            row.put("studyMinutes", studyMap.getOrDefault(row.get("date").toString(), 0L));
        }
        return result;
    }

    private List<Map<String, Object>> recentGraduate(List<Long> userIds, List<Long> projectIds) {
        List<GraduateFile> files = graduateFiles(userIds, projectIds);
        return files.stream().sorted((a, b) -> {
                    LocalDateTime x = a.getGraduateTime();
                    LocalDateTime y = b.getGraduateTime();
                    return y == null ? 1 : (x == null ? -1 : y.compareTo(x));
                }).limit(6).map(f -> {
                    SysUser u = userMapper.selectById(f.getUserId());
                    TrainingProject p = projectMapper.selectById(f.getProjectId());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userName", u == null ? null : u.getRealName());
                    m.put("projectName", p == null ? null : p.getProjectName());
                    m.put("graduateTime", f.getGraduateTime());
                    m.put("certificateNo", f.getCertificateNo());
                    return m;
                }).toList();
    }

    // ---------------- 单位汇总 ----------------

    public List<Map<String, Object>> orgSummary(LoginUser current, Long projectId) {
        List<Long> orgIds = current.isSuperAdmin() ? null : orgScopeIds(current);
        List<SysOrg> orgs = orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getStatus, 1).orderByAsc(SysOrg::getSort));
        List<Long> projectIds = projectService.visibleProjectIds(current);
        List<Long> userIds = visibleUserIds(current);
        List<StudyRecord> studies = studyRecords(userIds, projectIds, null);
        List<CreditRecord> credits = creditRecords(userIds, projectIds, null);
        List<GraduateFile> files = graduateFiles(userIds, projectIds);
        List<SysUser> students = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserType, 4).eq(SysUser::getStatus, 1));
        Map<Long, Long> orgStudentCount = new HashMap<>();
        for (SysUser u : students) {
            Long orgId = u.getOrgId() == null ? u.getDeptId() : u.getOrgId();
            if (orgId != null) {
                orgStudentCount.merge(orgId, 1L, Long::sum);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysOrg org : orgs) {
            if (orgIds != null && !orgIds.contains(org.getId())) {
                continue;
            }
            List<SysUser> orgStudents = students.stream()
                    .filter(u -> Objects.equals(u.getOrgId() == null ? u.getDeptId() : u.getOrgId(), org.getId()))
                    .toList();
            List<Long> ids = orgStudents.stream().map(SysUser::getId).toList();
            List<StudyRecord> orgStudies = studies.stream().filter(s -> ids.contains(s.getUserId())).toList();
            List<CreditRecord> orgCredits = credits.stream()
                    .filter(c -> ids.contains(c.getUserId()) && c.getAuditStatus() != null && c.getAuditStatus() == 2).toList();
            BigDecimal total = orgCredits.stream().map(CreditRecord::getCreditValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            long completed = orgStudies.stream().filter(s -> s.getStatus() != null && s.getStatus() == 2).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orgId", org.getId());
            m.put("orgName", org.getOrgName());
            m.put("studentCount", orgStudentCount.getOrDefault(org.getId(), 0L));
            m.put("studyCount", orgStudies.size());
            m.put("completedCount", completed);
            m.put("studyHours", BigDecimal.valueOf(orgStudies.stream()
                            .mapToInt(s -> s.getValidDuration() == null ? 0 : s.getValidDuration()).sum())
                    .divide(BigDecimal.valueOf(3600), 1, RoundingMode.HALF_UP));
            m.put("creditTotal", total);
            m.put("avgCredit", ids.isEmpty() ? BigDecimal.ZERO
                    : total.divide(BigDecimal.valueOf(ids.size()), 2, RoundingMode.HALF_UP));
            m.put("graduateCount", files.stream().filter(f -> ids.contains(f.getUserId())).count());
            m.put("completionRate", orgStudies.isEmpty() ? 0
                    : BigDecimal.valueOf(completed * 100.0 / orgStudies.size()).setScale(1, RoundingMode.HALF_UP));
            rows.add(m);
        }
        return rows;
    }

    // ---------------- 学分统计 ----------------

    public List<Map<String, Object>> creditReport(LoginUser current, Long projectId) {
        List<Long> projectIds = projectService.visibleProjectIds(current);
        List<Long> userIds = visibleUserIds(current);
        List<TrainingProject> projects = projectId != null ? List.of(projectMapper.selectById(projectId))
                : (projectIds == null ? projectMapper.selectList(new LambdaQueryWrapper<TrainingProject>()
                .orderByDesc(TrainingProject::getId)) : projectMapper.selectBatchIds(projectIds));
        projects = projects.stream().filter(Objects::nonNull).toList();
        List<CreditRecord> credits = creditRecords(userIds,
                projects.stream().map(TrainingProject::getId).toList(), null);
        List<GraduateFile> files = graduateFiles(userIds, projects.stream().map(TrainingProject::getId).toList());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TrainingProject p : projects) {
            List<CreditRecord> issued = credits.stream()
                    .filter(c -> Objects.equals(c.getProjectId(), p.getId()) && c.getAuditStatus() != null && c.getAuditStatus() == 2)
                    .toList();
            BigDecimal total = issued.stream().map(CreditRecord::getCreditValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            long members = memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, p.getId()).eq(ProjectMember::getStatus, 1));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("projectId", p.getId());
            m.put("projectName", p.getProjectName());
            m.put("status", p.getStatus());
            m.put("needCredit", p.getNeedCredit());
            m.put("memberCount", members);
            m.put("creditTotal", total);
            m.put("avgCredit", members == 0 ? BigDecimal.ZERO
                    : total.divide(BigDecimal.valueOf(members), 2, RoundingMode.HALF_UP));
            m.put("issuedCount", issued.size());
            m.put("graduateCount", files.stream().filter(f -> Objects.equals(f.getProjectId(), p.getId())).count());
            m.put("autoCreditTotal", issued.stream().filter(c -> c.getSourceType() != null && c.getSourceType() == 1)
                    .map(CreditRecord::getCreditValue).reduce(BigDecimal.ZERO, BigDecimal::add));
            m.put("manualCreditTotal", issued.stream().filter(c -> c.getSourceType() != null && c.getSourceType() == 2)
                    .map(CreditRecord::getCreditValue).reduce(BigDecimal.ZERO, BigDecimal::add));
            rows.add(m);
        }
        return rows;
    }

    // ---------------- 课程学习报表 ----------------

    public List<Map<String, Object>> courseReport(LoginUser current, Long projectId) {
        List<Long> courseIds = visibleCourseIds(current, projectService.visibleProjectIds(current));
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<>();
        if (projectId != null) {
            qw.eq(Course::getProjectId, projectId);
        }
        if (courseIds != null) {
            if (courseIds.isEmpty()) {
                return List.of();
            }
            qw.in(Course::getId, courseIds);
        }
        List<Course> courses = courseMapper.selectList(qw.orderByDesc(Course::getId));
        List<StudyRecord> studies = studyRecords(null, projectId == null ? null : List.of(projectId), courseIds);
        List<ExamRecord> exams = examMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(projectId != null, ExamRecord::getProjectId, projectId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Course c : courses) {
            List<StudyRecord> cs = studies.stream().filter(s -> Objects.equals(s.getCourseId(), c.getId())).toList();
            List<ExamRecord> ce = exams.stream().filter(e -> Objects.equals(e.getCourseId(), c.getId())).toList();
            long completed = cs.stream().filter(s -> s.getStatus() != null && s.getStatus() == 2).count();
            long passed = ce.stream().filter(e -> e.getIsPass() != null && e.getIsPass() == 1).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("courseId", c.getId());
            m.put("courseName", c.getCourseName());
            m.put("projectId", c.getProjectId());
            m.put("needExam", c.getNeedExam());
            m.put("studyCount", cs.size());
            m.put("completedCount", completed);
            m.put("completionRate", cs.isEmpty() ? 0
                    : BigDecimal.valueOf(completed * 100.0 / cs.size()).setScale(1, RoundingMode.HALF_UP));
            m.put("examCount", ce.size());
            m.put("examPassCount", passed);
            m.put("examPassRate", ce.isEmpty() ? 0
                    : BigDecimal.valueOf(passed * 100.0 / ce.size()).setScale(1, RoundingMode.HALF_UP));
            m.put("avgScore", ce.isEmpty() ? BigDecimal.ZERO
                    : ce.stream().map(ExamRecord::getScore).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(ce.size()), 1, RoundingMode.HALF_UP));
            m.put("avgProgress", cs.isEmpty() ? BigDecimal.ZERO
                    : cs.stream().map(StudyRecord::getProgress).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(cs.size()), 1, RoundingMode.HALF_UP));
            rows.add(m);
        }
        return rows;
    }

    // ---------------- 预警 ----------------

    public List<Map<String, Object>> warnings(LoginUser current, Long projectId, String type) {
        List<Long> projectIds = projectService.visibleProjectIds(current);
        List<Long> userIds = visibleUserIds(current);
        List<Map<String, Object>> rows = new ArrayList<>();
        String t = StrUtil.blankToDefault(type, "ALL").toUpperCase();
        LocalDate today = LocalDate.now();
        if ("ALL".equals(t) || "CREDIT".equals(t)) {
            rows.addAll(creditWarnings(projectId, projectIds, userIds));
        }
        if ("ALL".equals(t) || "EXPIRE".equals(t)) {
            rows.addAll(expireWarnings(projectId, projectIds));
        }
        if ("ALL".equals(t) || "INACTIVE".equals(t)) {
            rows.addAll(inactiveWarnings(projectId, projectIds, userIds, today));
        }
        return rows.stream().limit(500).toList();
    }

    private List<Map<String, Object>> creditWarnings(Long projectId, List<Long> projectIds, List<Long> userIds) {
        List<TrainingProject> projects = projectId != null ? List.of(projectMapper.selectById(projectId))
                : (projectIds == null ? projectMapper.selectList(new LambdaQueryWrapper<TrainingProject>()
                .in(TrainingProject::getStatus, 1, 2)) : projectMapper.selectBatchIds(projectIds));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TrainingProject p : projects.stream().filter(Objects::nonNull).toList()) {
            List<ProjectMember> members = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, p.getId()).eq(ProjectMember::getStatus, 1));
            if (userIds != null) {
                members = members.stream().filter(m -> userIds.contains(m.getUserId())).toList();
            }
            for (ProjectMember member : members) {
                BigDecimal credits = creditService.projectCreditTotal(member.getUserId(), p.getId());
                if (credits.compareTo(p.getNeedCredit()) >= 0) {
                    continue;
                }
                SysUser u = userMapper.selectById(member.getUserId());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("type", "CREDIT");
                m.put("typeLabel", "学分不足");
                m.put("userId", member.getUserId());
                m.put("userName", u == null ? null : u.getRealName());
                m.put("phone", u == null ? null : u.getPhone());
                m.put("orgName", u == null ? null : orgName(u.getOrgId() == null ? u.getDeptId() : u.getOrgId()));
                m.put("projectId", p.getId());
                m.put("projectName", p.getProjectName());
                m.put("needCredit", p.getNeedCredit());
                m.put("currentCredit", credits);
                m.put("deficit", p.getNeedCredit().subtract(credits));
                m.put("message", "学分不足，还差 " + p.getNeedCredit().subtract(credits) + " 学分");
                rows.add(m);
            }
        }
        return rows;
    }

    private List<Map<String, Object>> expireWarnings(Long projectId, List<Long> projectIds) {
        LocalDate today = LocalDate.now();
        List<TrainingProject> projects = projectId != null ? List.of(projectMapper.selectById(projectId))
                : (projectIds == null ? projectMapper.selectList(new LambdaQueryWrapper<TrainingProject>()
                .eq(TrainingProject::getStatus, 1)) : projectMapper.selectBatchIds(projectIds));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TrainingProject p : projects.stream().filter(Objects::nonNull).toList()) {
            if (p.getEndTime() == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, p.getEndTime().toLocalDate());
            if (days < 0 || days > 30) {
                continue;
            }
            long members = memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, p.getId()).eq(ProjectMember::getStatus, 1));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "EXPIRE");
            m.put("typeLabel", "即将到期");
            m.put("projectId", p.getId());
            m.put("projectName", p.getProjectName());
            m.put("endTime", p.getEndTime());
            m.put("daysLeft", days);
            m.put("memberCount", members);
            m.put("message", "培训班将在 " + days + " 天后结束，共 " + members + " 名学员");
            rows.add(m);
        }
        return rows;
    }

    private List<Map<String, Object>> inactiveWarnings(Long projectId, List<Long> projectIds, List<Long> userIds, LocalDate today) {
        List<TrainingProject> projects = projectId != null ? List.of(projectMapper.selectById(projectId))
                : (projectIds == null ? projectMapper.selectList(new LambdaQueryWrapper<TrainingProject>()
                .in(TrainingProject::getStatus, 1, 2)) : projectMapper.selectBatchIds(projectIds));
        LocalDateTime line = today.minusDays(30).atStartOfDay();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TrainingProject p : projects.stream().filter(Objects::nonNull).toList()) {
            List<ProjectMember> members = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, p.getId()).eq(ProjectMember::getStatus, 1));
            for (ProjectMember member : members) {
                if (userIds != null && !userIds.contains(member.getUserId())) {
                    continue;
                }
                SysUser u = userMapper.selectById(member.getUserId());
                if (u == null) {
                    continue;
                }
                StudyRecord lastStudy = studyMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                        .eq(StudyRecord::getUserId, member.getUserId()).eq(StudyRecord::getProjectId, p.getId())
                        .orderByDesc(StudyRecord::getLastHeartbeat).last("limit 1"));
                boolean noStudy = lastStudy == null || lastStudy.getLastHeartbeat() == null
                        || lastStudy.getLastHeartbeat().isBefore(line);
                boolean noLogin = u.getLastLoginTime() == null || u.getLastLoginTime().isBefore(line);
                if (!noStudy && !noLogin) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("type", "INACTIVE");
                m.put("typeLabel", "长期未学习");
                m.put("userId", u.getId());
                m.put("userName", u.getRealName());
                m.put("phone", u.getPhone());
                m.put("orgName", orgName(u.getOrgId() == null ? u.getDeptId() : u.getOrgId()));
                m.put("projectId", p.getId());
                m.put("projectName", p.getProjectName());
                m.put("lastLoginTime", u.getLastLoginTime());
                m.put("lastStudyTime", lastStudy == null ? null : lastStudy.getLastHeartbeat());
                m.put("message", "超过 30 天未登录或未学习");
                rows.add(m);
            }
        }
        return rows;
    }

    public int notifyWarnings(LoginUser current, Long projectId, String type, List<Long> userIds) {
        List<Map<String, Object>> warnings = warnings(current, projectId, type);
        List<Long> targets = warnings.stream().map(w -> (Long) w.get("userId"))
                .filter(Objects::nonNull).distinct().toList();
        if (userIds != null && !userIds.isEmpty()) {
            targets = targets.stream().filter(userIds::contains).toList();
        }
        for (Long userId : targets) {
            notificationService.send(userId, "学习提醒",
                    "您所在的培训班存在学分不足或长期未学习情况，请及时登录平台完成学习任务。", "WARNING", projectId);
        }
        return targets.size();
    }

    // ---------------- 导出 ----------------

    public void export(LoginUser current, String type, Long projectId, HttpServletResponse response) throws IOException {
        String name = "数据导出";
        List<List<String>> head = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        switch (StrUtil.blankToDefault(type, "credit")) {
            case "org" -> {
                name = "单位汇总";
                head = heads("单位名称", "学员数", "学习记录数", "完成数", "学习学时", "学分合计", "人均学分", "毕业人数", "完成率(%)");
                for (Map<String, Object> m : orgSummary(current, projectId)) {
                    rows.add(row(m.get("orgName"), m.get("studentCount"), m.get("studyCount"), m.get("completedCount"),
                            m.get("studyHours"), m.get("creditTotal"), m.get("avgCredit"), m.get("graduateCount"), m.get("completionRate")));
                }
            }
            case "course" -> {
                name = "课程学习报表";
                head = heads("课程名称", "学习人数", "完成人数", "完成率(%)", "测验次数", "通过人数", "通过率(%)", "平均分", "平均进度(%)");
                for (Map<String, Object> m : courseReport(current, projectId)) {
                    rows.add(row(m.get("courseName"), m.get("studyCount"), m.get("completedCount"), m.get("completionRate"),
                            m.get("examCount"), m.get("examPassCount"), m.get("examPassRate"), m.get("avgScore"), m.get("avgProgress")));
                }
            }
            case "warning" -> {
                name = "学习预警";
                head = heads("预警类型", "学员", "联系电话", "单位", "培训班", "所需学分", "当前学分", "差额", "最后登录", "预警说明");
                for (Map<String, Object> m : warnings(current, projectId, null)) {
                    rows.add(row(m.get("typeLabel"), m.get("userName"), m.get("phone"), m.get("orgName"),
                            m.get("projectName"), m.get("needCredit"), m.get("currentCredit"), m.get("deficit"),
                            m.get("lastLoginTime"), m.get("message")));
                }
            }
            case "graduate" -> {
                name = "毕业档案";
                head = heads("证书编号", "学员", "单位", "培训班", "学分", "毕业时间");
                for (Map<String, Object> m : graduateFileRows(current, projectId)) {
                    rows.add(row(m.get("certificateNo"), m.get("userName"), m.get("orgName"),
                            m.get("projectName"), m.get("creditValue"), m.get("graduateTime")));
                }
            }
            case "credit" -> {
                name = "学分台账";
                head = heads("学员", "账号", "单位", "培训班", "课程", "学分", "来源", "状态", "发放时间", "备注");
                var page = creditService.ledgerPage(current, 1, 10000, null, null, projectId, null, null, null, null, null);
                for (Map<String, Object> m : page.getRecords()) {
                    rows.add(row(m.get("userName"), m.get("username"), m.get("orgName"), m.get("projectName"),
                            m.get("courseName"), m.get("creditValue"), m.get("sourceLabel"), m.get("auditStatusLabel"),
                            m.get("auditTime"), m.get("remark")));
                }
            }
            case "creditReport" -> {
                name = "培训班学分统计";
                head = heads("培训班", "状态", "人数", "所需学分", "学分合计", "人均学分", "已发放人数", "毕业人数", "自动录入", "手工补录");
                for (Map<String, Object> m : creditReport(current, projectId)) {
                    rows.add(row(m.get("projectName"), m.get("status"), m.get("memberCount"), m.get("needCredit"),
                            m.get("creditTotal"), m.get("avgCredit"), m.get("issuedCount"), m.get("graduateCount"),
                            m.get("autoCreditTotal"), m.get("manualCreditTotal")));
                }
            }
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String filename = URLEncoder.encode(name + "-" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + filename);
        EasyExcel.write(response.getOutputStream()).head(head).sheet(name).doWrite(rows);
    }

    private List<Map<String, Object>> graduateFileRows(LoginUser current, Long projectId) {
        List<Long> projectIds = projectService.visibleProjectIds(current);
        List<Long> userIds = visibleUserIds(current);
        List<GraduateFile> files = graduateFiles(userIds, projectId == null ? projectIds : List.of(projectId));
        return files.stream().map(f -> {
            SysUser u = userMapper.selectById(f.getUserId());
            TrainingProject p = projectMapper.selectById(f.getProjectId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("certificateNo", f.getCertificateNo());
            m.put("userName", u == null ? null : u.getRealName());
            m.put("orgName", u == null ? null : orgName(u.getOrgId() == null ? u.getDeptId() : u.getOrgId()));
            m.put("projectName", p == null ? null : p.getProjectName());
            m.put("creditValue", f.getCreditValue());
            m.put("graduateTime", f.getGraduateTime());
            return m;
        }).toList();
    }

    private List<List<String>> heads(String... names) {
        List<List<String>> head = new ArrayList<>();
        for (String name : names) {
            List<String> col = new ArrayList<>();
            col.add(name);
            head.add(col);
        }
        return head;
    }

    private List<Object> row(Object... values) {
        List<Object> row = new ArrayList<>();
        for (Object v : values) {
            row.add(v == null ? "" : v);
        }
        return row;
    }

    // ---------------- 通用查询与范围 ----------------

    private List<Long> visibleUserIds(LoginUser current) {
        return creditService.visibleUserIds(current);
    }

    private List<Long> orgScopeIds(LoginUser current) {
        return dataScope.accessibleOrgIds(current);
    }

    private List<Long> visibleCourseIds(LoginUser current, List<Long> projectIds) {
        if (current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1)) {
            return null;
        }
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<>();
        if ("teacher".equals(current.getRoleCode())) {
            List<Long> own = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .eq(Course::getCreateUserId, current.getId()).select(Course::getId))
                    .stream().map(Course::getId).toList();
            if (projectIds != null && !projectIds.isEmpty()) {
                own = new ArrayList<>(own);
                own.addAll(courseMapper.selectList(new LambdaQueryWrapper<Course>()
                                .in(Course::getProjectId, projectIds).select(Course::getId))
                        .stream().map(Course::getId).toList());
            }
            return own.stream().distinct().toList();
        }
        if (projectIds == null) {
            return null;
        }
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return courseMapper.selectList(new LambdaQueryWrapper<Course>().in(Course::getProjectId, projectIds)
                        .select(Course::getId))
                .stream().map(Course::getId).toList();
    }

    private List<StudyRecord> studyRecords(List<Long> userIds, List<Long> projectIds, List<Long> courseIds) {
        LambdaQueryWrapper<StudyRecord> qw = new LambdaQueryWrapper<>();
        if (userIds != null) {
            if (userIds.isEmpty()) {
                return List.of();
            }
            qw.in(StudyRecord::getUserId, userIds);
        }
        if (projectIds != null) {
            if (projectIds.isEmpty()) {
                return List.of();
            }
            qw.in(StudyRecord::getProjectId, projectIds);
        }
        if (courseIds != null) {
            if (courseIds.isEmpty()) {
                return List.of();
            }
            qw.in(StudyRecord::getCourseId, courseIds);
        }
        return studyMapper.selectList(qw);
    }

    private List<CreditRecord> creditRecords(List<Long> userIds, List<Long> projectIds, List<Long> courseIds) {
        LambdaQueryWrapper<CreditRecord> qw = new LambdaQueryWrapper<>();
        if (userIds != null) {
            if (userIds.isEmpty()) {
                return List.of();
            }
            qw.in(CreditRecord::getUserId, userIds);
        }
        if (projectIds != null) {
            if (projectIds.isEmpty()) {
                return List.of();
            }
            qw.in(CreditRecord::getProjectId, projectIds);
        }
        if (courseIds != null) {
            if (courseIds.isEmpty()) {
                return List.of();
            }
            qw.in(CreditRecord::getCourseId, courseIds);
        }
        return creditMapper.selectList(qw);
    }

    private List<GraduateFile> graduateFiles(List<Long> userIds, List<Long> projectIds) {
        LambdaQueryWrapper<GraduateFile> qw = new LambdaQueryWrapper<>();
        if (userIds != null) {
            if (userIds.isEmpty()) {
                return List.of();
            }
            qw.in(GraduateFile::getUserId, userIds);
        }
        if (projectIds != null) {
            if (projectIds.isEmpty()) {
                return List.of();
            }
            qw.in(GraduateFile::getProjectId, projectIds);
        }
        return graduateFileMapper.selectList(qw);
    }

    private List<UserVerifyLog> verifyLogs(List<Long> userIds, List<Long> projectIds, List<Long> courseIds) {
        LambdaQueryWrapper<UserVerifyLog> qw = new LambdaQueryWrapper<>();
        if (userIds != null) {
            if (userIds.isEmpty()) {
                return List.of();
            }
            qw.in(UserVerifyLog::getUserId, userIds);
        }
        if (projectIds != null) {
            if (projectIds.isEmpty()) {
                return List.of();
            }
            qw.in(UserVerifyLog::getProjectId, projectIds);
        }
        if (courseIds != null) {
            if (courseIds.isEmpty()) {
                return List.of();
            }
            qw.in(UserVerifyLog::getCourseId, courseIds);
        }
        return verifyMapper.selectList(qw);
    }

    private String orgName(Long orgId) {
        if (orgId == null) {
            return null;
        }
        SysOrg org = orgMapper.selectById(orgId);
        return org == null ? null : org.getOrgName();
    }
}
