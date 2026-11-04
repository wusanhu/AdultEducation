package com.adultedu.service;

import com.adultedu.common.NotificationTemplates;
import com.adultedu.entity.Course;
import com.adultedu.entity.CreditRecord;
import com.adultedu.entity.GraduateApply;
import com.adultedu.entity.ProjectTeacher;
import com.adultedu.entity.SysRole;
import com.adultedu.entity.SysUser;
import com.adultedu.entity.TrainingProject;
import com.adultedu.entity.UserVerifyLog;
import com.adultedu.mapper.CourseMapper;
import com.adultedu.mapper.CreditRecordMapper;
import com.adultedu.mapper.GraduateApplyMapper;
import com.adultedu.mapper.ProjectTeacherMapper;
import com.adultedu.mapper.SysRoleMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.mapper.TrainingProjectMapper;
import com.adultedu.mapper.UserVerifyLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {
    private final GraduateApplyMapper graduateApplyMapper;
    private final CreditRecordMapper creditMapper;
    private final TrainingProjectMapper projectMapper;
    private final ProjectTeacherMapper teacherMapper;
    private final CourseMapper courseMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final UserVerifyLogMapper verifyMapper;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.upload.dir:./data/uploads}")
    private String uploadDir;

    /** 每天 02:00 自动结束到期培训班。 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void expireProjects() {
        LocalDateTime now = LocalDateTime.now();
        int rows = projectMapper.update(null, new LambdaUpdateWrapper<TrainingProject>()
                .set(TrainingProject::getStatus, 2)
                .eq(TrainingProject::getStatus, 1)
                .lt(TrainingProject::getEndTime, now));
        if (rows > 0) {
            log.info("自动结束到期培训班 {} 个", rows);
        }
    }

    /** 每天 02:30 清理超过 180 天留存期的核验影像。 */
    @Scheduled(cron = "0 30 2 * * ?")
    public void cleanVerifyImages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(180);
        List<UserVerifyLog> logs = verifyMapper.selectList(new LambdaQueryWrapper<UserVerifyLog>()
                .isNotNull(UserVerifyLog::getImgUrl)
                .lt(UserVerifyLog::getVerifyTime, cutoff)
                .last("limit 2000"));
        int cleaned = 0;
        for (UserVerifyLog item : logs) {
            deleteUploadFile(item.getImgUrl());
            verifyMapper.update(null, new LambdaUpdateWrapper<UserVerifyLog>()
                    .eq(UserVerifyLog::getId, item.getId())
                    .set(UserVerifyLog::getImgUrl, null)
                    .set(UserVerifyLog::getRemark, "影像资料已过 180 天留存期，系统自动清理"));
            cleaned++;
        }
        if (cleaned > 0) {
            log.info("清理过期核验影像 {} 条", cleaned);
        }
    }

    /** 每天 09:00 对待审超过 24 小时的毕业申请与学分补录做汇总提醒。 */
    @Scheduled(cron = "0 0 9 * * ?")
    public void remindPendingApprovals() {
        String guard = "job:remind:" + LocalDate.now();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(guard, "1", Duration.ofHours(20));
        if (!Boolean.TRUE.equals(first)) {
            return;
        }
        LocalDateTime before = LocalDateTime.now().minusHours(24);
        Map<Long, Integer> graduateCounts = new HashMap<>();
        Map<Long, Integer> creditCounts = new HashMap<>();

        List<GraduateApply> applies = graduateApplyMapper.selectList(new LambdaQueryWrapper<GraduateApply>()
                .eq(GraduateApply::getAuditStatus, 0).lt(GraduateApply::getCreateTime, before));
        for (GraduateApply apply : applies) {
            for (Long userId : graduateNodeUsers(apply)) {
                graduateCounts.merge(userId, 1, Integer::sum);
            }
        }
        List<CreditRecord> credits = creditMapper.selectList(new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getAuditStatus, 1).lt(CreditRecord::getCreateTime, before));
        for (CreditRecord credit : credits) {
            for (Long userId : roleUsers("LEADER".equals(credit.getAuditNode()) ? "leader" : "super_admin")) {
                creditCounts.merge(userId, 1, Integer::sum);
            }
        }

        Map<Long, Integer> merged = new HashMap<>(graduateCounts);
        creditCounts.forEach((k, v) -> merged.merge(k, v, Integer::sum));
        merged.forEach((userId, count) -> {
            List<String> parts = new ArrayList<>();
            if (graduateCounts.containsKey(userId)) {
                parts.add("毕业申请待审 " + graduateCounts.get(userId) + " 项");
            }
            if (creditCounts.containsKey(userId)) {
                parts.add("学分补录待审 " + creditCounts.get(userId) + " 项");
            }
            notificationService.sendByTemplate(NotificationTemplates.APPROVAL_TIMEOUT, userId,
                    Map.of("count", count, "detail", String.join("、", parts)), null);
        });
        if (!merged.isEmpty()) {
            log.info("发送审批超时提醒 {} 人", merged.size());
        }
    }

    private List<Long> graduateNodeUsers(GraduateApply apply) {
        String node = apply.getCurrentNode();
        if ("TEACHER".equals(node)) {
            List<Long> ids = new ArrayList<>(teacherMapper.selectList(new LambdaQueryWrapper<ProjectTeacher>()
                            .eq(ProjectTeacher::getProjectId, apply.getProjectId()))
                    .stream().map(ProjectTeacher::getUserId).toList());
            ids.addAll(courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .eq(Course::getProjectId, apply.getProjectId()).isNotNull(Course::getCreateUserId))
                    .stream().map(Course::getCreateUserId).toList());
            return ids.stream().distinct().toList();
        }
        return roleUsers("LEADER".equals(node) ? "leader" : "super_admin");
    }

    private List<Long> roleUsers(String roleCode) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role == null) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleId, role.getId()).eq(SysUser::getStatus, 1))
                .stream().map(SysUser::getId).toList();
    }

    private void deleteUploadFile(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return;
        }
        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path file = root.resolve(url.substring("/uploads/".length())).normalize();
            if (file.startsWith(root)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("核验影像清理失败: {}", url, e);
        }
    }
}
