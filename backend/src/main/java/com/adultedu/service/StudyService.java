package com.adultedu.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.adultedu.common.BusinessException;
import com.adultedu.common.NotificationTemplates;
import com.adultedu.dto.BizDtos.HeartbeatRequest;
import com.adultedu.dto.BizDtos.SmsVerifySubmitRequest;
import com.adultedu.dto.BizDtos.VerifyStartRequest;
import com.adultedu.dto.BizDtos.VerifySubmitRequest;
import com.adultedu.entity.Course;
import com.adultedu.entity.CourseAttachment;
import com.adultedu.entity.ExamRecord;
import com.adultedu.entity.ProjectMember;
import com.adultedu.entity.StudyHeartbeatLog;
import com.adultedu.entity.StudyRecord;
import com.adultedu.entity.SysConfig;
import com.adultedu.entity.SysUser;
import com.adultedu.entity.TrainingProject;
import com.adultedu.entity.UserVerifyLog;
import com.adultedu.entity.VerifyGear;
import com.adultedu.mapper.CourseAttachmentMapper;
import com.adultedu.mapper.CourseMapper;
import com.adultedu.mapper.ExamRecordMapper;
import com.adultedu.mapper.ProjectMemberMapper;
import com.adultedu.mapper.StudyHeartbeatLogMapper;
import com.adultedu.mapper.StudyRecordMapper;
import com.adultedu.mapper.SysConfigMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.mapper.TrainingProjectMapper;
import com.adultedu.mapper.UserVerifyLogMapper;
import com.adultedu.mapper.VerifyGearMapper;
import com.adultedu.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyService {
    private final CourseMapper courseMapper;
    private final CourseAttachmentMapper attachmentMapper;
    private final TrainingProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final StudyRecordMapper studyMapper;
    private final StudyHeartbeatLogMapper heartbeatMapper;
    private final UserVerifyLogMapper verifyMapper;
    private final VerifyGearMapper gearMapper;
    private final ExamRecordMapper examMapper;
    private final SysUserMapper userMapper;
    private final SysConfigMapper configMapper;
    private final CreditService creditService;
    private final NotificationService notificationService;
    private final StringRedisTemplate redis;

    @Value("${app.study.heartbeat-seconds:30}")
    private int heartbeatSeconds;
    @Value("${app.study.popup-min-seconds:300}")
    private int popupMinSeconds;
    @Value("${app.study.popup-max-seconds:600}")
    private int popupMaxSeconds;
    @Value("${app.study.popup-timeout-seconds:60}")
    private int popupTimeoutSeconds;
    @Value("${app.face.mock:true}")
    private boolean faceMock;
    @Value("${app.face.threshold:0.75}")
    private double faceThreshold;
    @Value("${app.face.mock-pass-similarity:0.92}")
    private double mockPassSimilarity;
    @Value("${app.face.mock-fail-similarity:0.32}")
    private double mockFailSimilarity;
    @Value("${app.sms.code-expire-seconds:300}")
    private int smsExpireSeconds;
    @Value("${app.sms.resend-seconds:60}")
    private int smsResendSeconds;
    @Value("${app.sms.daily-limit:10}")
    private int smsDailyLimit;
    @Value("${app.sms.mock:true}")
    private boolean smsMock;
    @Value("${app.upload.dir:./data/uploads}")
    private String uploadDir;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ---------------- 学习会话 ----------------

    @Transactional
    public Map<String, Object> start(LoginUser current, Long courseId) {
        Course course = requireCourse(courseId, true);
        TrainingProject project = projectMapper.selectById(course.getProjectId());
        if (!current.isSuperAdmin()) {
            ensureMember(current.getId(), course.getProjectId());
            ensureProjectOpen(project);
        }
        StudyRecord record = studyMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, current.getId()).eq(StudyRecord::getCourseId, courseId));
        if (record == null) {
            record = new StudyRecord();
            record.setUserId(current.getId());
            record.setCourseId(courseId);
            record.setProjectId(course.getProjectId());
            record.setStartTime(LocalDateTime.now());
            record.setValidDuration(0);
            record.setVerifyCount(0);
            record.setScreenSwitchCount(0);
            record.setScreenSwitchDuration(0);
            record.setVerifyTotal(0);
            record.setVerifySuccess(0);
            record.setVerifyFail(0);
            record.setIsAbnormal(0);
            record.setProgress(BigDecimal.ZERO);
            record.setLastPosition(0);
            record.setStatus(1);
            record.setLastHeartbeat(LocalDateTime.now());
            record.setDevice(currentDevice());
            record.setIp(currentIp());
            try {
                studyMapper.insert(record);
            } catch (DuplicateKeyException e) {
                // 双击/并发首次进入时唯一键冲突，复用已存在的学习记录
                record = studyMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                        .eq(StudyRecord::getUserId, current.getId()).eq(StudyRecord::getCourseId, courseId));
                if (record == null) {
                    throw e;
                }
            }
        } else {
            if (record.getStatus() != null && record.getStatus() == 2) {
                return state(record, course, gearOf(course), Map.of("completed", true));
            }
            String session = redis.opsForValue().get(sessionKey(current.getId(), courseId));
            boolean active = session != null && record.getLastHeartbeat() != null
                    && record.getLastHeartbeat().isAfter(LocalDateTime.now().minusSeconds(heartbeatSeconds * 3L));
            if (active) {
                throw new BusinessException(409, "该课程正在其他设备学习，请先在原设备退出学习");
            }
            if (record.getStartTime() == null) {
                record.setStartTime(LocalDateTime.now());
            }
            record.setStatus(1);
            record.setDevice(currentDevice());
            record.setIp(currentIp());
            record.setLastHeartbeat(LocalDateTime.now());
            studyMapper.updateById(record);
        }
        String sessionId = IdUtil.fastSimpleUUID();
        redis.opsForValue().set(sessionKey(current.getId(), courseId), sessionId, Duration.ofSeconds(heartbeatSeconds * 4L));
        record.setSessionId(sessionId);
        studyMapper.updateById(record);
        GearConfig gear = gearOf(course);
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("sessionId", sessionId);
        if (gear.gearCode() > 0 && isLocked(record.getId())) {
            // 学习异常后重新进入：要求先完成一次核验，通过后才恢复计时
            UserVerifyLog resume = createVerifyLog(record, "resume", gear);
            extra.put("resumeVerifyId", resume.getId());
        }
        return state(record, course, gear, extra);
    }

    @Transactional
    public Map<String, Object> heartbeat(LoginUser current, HeartbeatRequest req) {
        Course course = requireCourse(req.courseId(), true);
        ensureProjectWritable(course.getProjectId());
        StudyRecord record = studyMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, current.getId()).eq(StudyRecord::getCourseId, req.courseId()));
        if (record == null) {
            throw new BusinessException("请先开始学习");
        }
        String session = redis.opsForValue().get(sessionKey(current.getId(), req.courseId()));
        if (session == null || !session.equals(req.sessionId())) {
            throw new BusinessException(409, "学习会话已失效，请重新进入课程");
        }
        redis.expire(sessionKey(current.getId(), req.courseId()), Duration.ofSeconds(heartbeatSeconds * 4L));
        record.setSessionId(req.sessionId());

        GearConfig gear = gearOf(course);
        LocalDateTime now = LocalDateTime.now();
        boolean paused = false;
        String pauseReason = null;
        boolean verifyTimeout = false;

        UserVerifyLog pending = pendingVerify(record.getId());
        if (pending != null) {
            int timeout = gear.timeoutSecond();
            if (pending.getVerifyTime() != null && pending.getVerifyTime().plusSeconds(timeout).isBefore(now)) {
                pending.setStatus(3);
                pending.setVerifyResult(2);
                pending.setFailReason("核验超时，未在规定时间内完成");
                verifyMapper.updateById(pending);
                record.setVerifyFail(nz(record.getVerifyFail()) + 1);
                record.setIsAbnormal(1);
                registerVerifyFailure(record, gear);
                pending = null;
                verifyTimeout = true;
            }
        }
        if (pending != null) {
            paused = true;
            pauseReason = "请先完成身份核验";
        }

        Map<String, Object> popupState = checkPopup(record, now);
        boolean popupPending = Boolean.TRUE.equals(popupState.get("popup"));
        if (popupPending) {
            paused = true;
            pauseReason = "请完成随机防刷确认";
        }
        if (isLocked(record.getId())) {
            paused = true;
            pauseReason = "学习记录已标记异常，请重新进入课程完成核验或联系教员复核";
        }

        boolean screenSwitch = req.screenSwitch() != null && req.screenSwitch() == 1;
        int switchDuration = req.screenSwitchDuration() == null ? 0 : Math.max(0, req.screenSwitchDuration());
        if (screenSwitch) {
            record.setScreenSwitchCount(nz(record.getScreenSwitchCount()) + 1);
        }
        // 前后台切换时长可能在下一次心跳回传，只要上报了时长就累计
        if (switchDuration > 0) {
            record.setScreenSwitchDuration(nz(record.getScreenSwitchDuration()) + switchDuration);
        }
        if (screenSwitch || switchDuration > 0) {
            if (record.getScreenSwitchCount() >= 3 || record.getScreenSwitchDuration() >= 30) {
                markAbnormal(record, "切屏次数或时长超过阈值（次数 " + record.getScreenSwitchCount()
                        + "，累计 " + record.getScreenSwitchDuration() + " 秒）");
                paused = true;
                pauseReason = "检测到频繁切屏，学习已暂停";
            }
        }

        LocalDateTime last = record.getLastHeartbeat();
        long gap = last == null ? 0 : Math.max(0, Duration.between(last, now).getSeconds());
        boolean heartbeatBroken = gap > heartbeatSeconds * 3L;
        if (heartbeatBroken && !paused) {
            paused = true;
            pauseReason = "心跳中断超过 " + heartbeatSeconds * 3 + " 秒，学习计时已暂停";
        }

        int effective = 0;
        if (!paused && req.inForeground() != null && req.inForeground() == 1 && !screenSwitch
                && record.getStatus() != null && record.getStatus() != 2) {
            int reported = req.deltaSeconds() == null ? 0 : Math.max(0, req.deltaSeconds());
            reported = Math.min(reported, heartbeatSeconds * 2);
            effective = (int) Math.min(reported, gap + 3);
            effective = Math.max(0, effective);
        }

        int oldPosition = nz(record.getLastPosition());
        int reportedPosition = req.position() == null ? oldPosition : Math.max(0, req.position());
        boolean seekViolation = false;
        if (!paused && course.getAllowSeek() != null && course.getAllowSeek() == 0
                && reportedPosition > oldPosition + effective + 5) {
            seekViolation = true;
            markAbnormal(record, "检测到拖拽快进，进度已锁定");
            paused = true;
            pauseReason = "检测到拖拽快进，学习已暂停";
            effective = 0;
            reportedPosition = oldPosition;
        }
        // 暂停期间（弹题/核验/切屏/心跳中断）学时与进度均不计（见上方 effective/progress 逻辑），
        // 但 lastPosition 如实记录：视频在被暂停前已播放到当前位置，若冻结回旧位置，
        // 恢复播放后的第一次心跳位置领先量会超过 effective + 5 秒，被误判为拖拽快进锁定记录
        record.setLastPosition(reportedPosition);
        record.setValidDuration(nz(record.getValidDuration()) + effective);

        BigDecimal oldProgress = record.getProgress() == null ? BigDecimal.ZERO : record.getProgress();
        BigDecimal progress = oldProgress;
        if (!paused && record.getStatus() != null && record.getStatus() != 2) {
            if (course.getVideoDuration() != null && course.getVideoDuration() > 0) {
                BigDecimal byPosition = BigDecimal.valueOf(Math.min(reportedPosition, course.getVideoDuration()))
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(course.getVideoDuration()), 2, RoundingMode.HALF_UP);
                progress = byPosition.max(oldProgress);
            }
            // 未配置视频时长时不再信任客户端上报的 progress，避免一次请求刷到 100%
            progress = progress.min(BigDecimal.valueOf(100));
        }
        record.setProgress(progress);
        record.setLastHeartbeat(now);

        Map<String, Object> verifyState = null;
        if (!paused && !isLocked(record.getId()) && record.getStatus() != null && record.getStatus() != 2) {
            verifyState = maybeTriggerVerify(record, gear);
            if (verifyState != null) {
                paused = true;
                pauseReason = "请完成本人身份核验";
            }
        }

        Map<String, Object> completion = null;
        if (!paused && record.getStatus() != null && record.getStatus() == 1 && progress.compareTo(BigDecimal.valueOf(99.5)) >= 0) {
            completion = complete(record, course);
            paused = true;
            pauseReason = "课程学习已完成";
        }

        if (record.getStatus() == null) {
            record.setStatus(1);
        }
        studyMapper.updateById(record);

        StudyHeartbeatLog log = new StudyHeartbeatLog();
        log.setStudyRecordId(record.getId());
        log.setUserId(record.getUserId());
        log.setCourseId(record.getCourseId());
        log.setReportTime(now);
        log.setDeltaSeconds(effective);
        log.setProgress(progress);
        log.setInForeground(req.inForeground() == null ? 1 : req.inForeground());
        log.setScreenSwitch(screenSwitch ? 1 : 0);
        log.setIp(currentIp());
        heartbeatMapper.insert(log);

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("sessionId", req.sessionId());
        extra.put("paused", paused);
        extra.put("pauseReason", pauseReason);
        extra.put("verifyTimeout", verifyTimeout);
        extra.put("popup", popupState.get("popup"));
        extra.put("popupNonce", popupState.get("nonce"));
        extra.put("popupRemaining", popupState.get("remaining"));
        extra.put("popupTimeout", popupState.get("timeout"));
        extra.put("needVerify", verifyState != null || pending != null);
        extra.put("verify", verifyState != null ? verifyState : (pending == null ? null : verifyMap(pending, gear, false)));
        extra.put("locked", isLocked(record.getId()));
        extra.put("seekViolation", seekViolation);
        if (completion != null) {
            extra.putAll(completion);
        }
        return state(record, course, gear, extra);
    }

    public Map<String, Object> learningState(LoginUser current, Long courseId) {
        Course course = requireCourse(courseId, false);
        StudyRecord record = findRecord(current.getId(), courseId);
        if (record == null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("courseId", course.getId());
            m.put("courseName", course.getCourseName());
            m.put("cover", course.getCover());
            m.put("videoUrl", course.getVideoUrl());
            m.put("videoDuration", course.getVideoDuration());
            m.put("credit", course.getCredit());
            m.put("needExam", course.getNeedExam());
            m.put("passScore", course.getPassScore());
            m.put("examLimit", course.getExamLimit());
            m.put("allowSeek", course.getAllowSeek());
            m.put("status", 0);
            m.put("progress", BigDecimal.ZERO);
            m.put("validDuration", 0);
            m.put("lastPosition", 0);
            m.put("started", false);
            m.put("attachments", attachmentMapper.selectList(new LambdaQueryWrapper<CourseAttachment>()
                    .eq(CourseAttachment::getCourseId, courseId).orderByAsc(CourseAttachment::getId)));
            return m;
        }
        Map<String, Object> extra = new LinkedHashMap<>();
        String session = redis.opsForValue().get(sessionKey(current.getId(), courseId));
        extra.put("sessionId", session);
        extra.put("started", true);
        UserVerifyLog pending = pendingVerify(record.getId());
        if (pending != null) {
            extra.put("needVerify", true);
            extra.put("verify", verifyMap(pending, gearOf(course), true));
        }
        Map<String, Object> m = state(record, course, gearOf(course), extra);
        m.put("activeSession", session != null);
        return m;
    }

    public Map<String, Object> pause(LoginUser current, Long courseId) {
        StudyRecord record = findRecord(current.getId(), courseId);
        if (record == null) {
            return Map.of();
        }
        ensureProjectWritable(record.getProjectId());
        record.setLastHeartbeat(LocalDateTime.now());
        studyMapper.updateById(record);
        redis.delete(sessionKey(current.getId(), courseId));
        return Map.of("validDuration", nz(record.getValidDuration()), "progress", record.getProgress());
    }

    // ---------------- 随机防刷弹窗 ----------------

    private Map<String, Object> checkPopup(StudyRecord record, LocalDateTime now) {
        Map<String, Object> result = new LinkedHashMap<>();
        String pendingKey = "study:popup:pending:" + record.getId();
        String pending = redis.opsForValue().get(pendingKey);
        long epoch = now.toEpochSecond(java.time.ZoneOffset.ofHours(8));
        if (pending != null) {
            String[] parts = pending.split("\\|");
            long expire = parts.length > 1 ? Long.parseLong(parts[1]) : epoch;
            if (epoch >= expire) {
                redis.delete(pendingKey);
                String timeoutKey = "study:popup:timeouts:" + record.getId();
                Long times = redis.opsForValue().increment(timeoutKey);
                redis.expire(timeoutKey, Duration.ofHours(24));
                resetPopupNext(record.getId(), epoch);
                result.put("timeout", true);
                if (times != null && times >= 3) {
                    markAbnormal(record, "随机防刷弹窗超时 3 次，疑似作弊");
                }
                return result;
            }
            result.put("popup", true);
            result.put("nonce", parts[0]);
            result.put("remaining", Math.max(0, expire - epoch));
            return result;
        }
        String nextKey = "study:popup:next:" + record.getId();
        String next = redis.opsForValue().get(nextKey);
        if (next == null) {
            resetPopupNext(record.getId(), epoch);
            return result;
        }
        if (epoch >= Long.parseLong(next)) {
            String nonce = IdUtil.fastSimpleUUID().substring(0, 8);
            redis.opsForValue().set(pendingKey, nonce + "|" + (epoch + popupTimeoutSeconds), Duration.ofSeconds(popupTimeoutSeconds + 120L));
            record.setVerifyCount(nz(record.getVerifyCount()) + 1);
            studyMapper.updateById(record);
            result.put("popup", true);
            result.put("nonce", nonce);
            result.put("remaining", popupTimeoutSeconds);
        }
        return result;
    }

    private void resetPopupNext(Long recordId, long epoch) {
        int min = Math.max(10, Math.min(popupMinSeconds, popupMaxSeconds));
        int max = Math.max(min, popupMaxSeconds);
        int next = min + RandomUtil.randomInt(0, Math.max(1, max - min + 1));
        redis.opsForValue().set("study:popup:next:" + recordId, String.valueOf(epoch + next), Duration.ofDays(2));
    }

    public Map<String, Object> confirmPopup(LoginUser current, Long courseId, String sessionId) {
        StudyRecord record = requireActiveSession(current, courseId, sessionId);
        ensureProjectWritable(record.getProjectId());
        String pendingKey = "study:popup:pending:" + record.getId();
        String pending = redis.opsForValue().get(pendingKey);
        if (pending == null) {
            throw new BusinessException("当前没有待确认的随机验证");
        }
        String[] parts = pending.split("\\|");
        long now = LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.ofHours(8));
        if (parts.length > 1 && now >= Long.parseLong(parts[1])) {
            redis.delete(pendingKey);
            throw new BusinessException("随机验证已超时");
        }
        redis.delete(pendingKey);
        resetPopupNext(record.getId(), now);
        return Map.of("confirmed", true);
    }

    // ---------------- 身份核验 ----------------

    @Transactional
    public Map<String, Object> startVerify(LoginUser current, VerifyStartRequest req) {
        Course course = requireCourse(req.courseId(), true);
        ensureProjectWritable(course.getProjectId());
        StudyRecord record = findRecord(current.getId(), req.courseId());
        if (record == null) {
            throw new BusinessException("请先开始学习");
        }
        GearConfig gear = gearOf(course);
        if (gear.gearCode() <= 0) {
            throw new BusinessException("该课程未开启身份核验");
        }
        UserVerifyLog pending = pendingVerify(record.getId());
        if (pending != null) {
            return verifyMap(pending, gear, true);
        }
        String trigger = StrUtil.isBlank(req.triggerPoint()) ? "manual" : req.triggerPoint().trim();
        UserVerifyLog log = createVerifyLog(record, trigger, gear);
        Map<String, Object> map = verifyMap(log, gear, true);
        map.put("faceMock", faceMock);
        map.put("faceThreshold", faceThreshold);
        map.put("message", "为确认是本人学习，请完成人脸核验，非本人核验将不计入学时学分。");
        return map;
    }

    @Transactional
    public Map<String, Object> submitFace(LoginUser current, VerifySubmitRequest req) {
        UserVerifyLog log = requireVerifyLog(current.getId(), req.logId());
        if (log.getStatus() != null && log.getStatus() != 0) {
            throw new BusinessException("该核验记录已处理");
        }
        Course course = courseMapper.selectById(log.getCourseId());
        ensureProjectWritable(course == null ? null : course.getProjectId());
        GearConfig gear = gearOf(course);
        StudyRecord record = studyMapper.selectById(log.getStudyRecordId());
        if (timedOut(log, gear)) {
            handleVerifyTimeout(record, log, gear);
            Map<String, Object> map = verifyResult(log, false, "核验已超时，本次核验不通过");
            map.put("timeout", true);
            return map;
        }
        String base64 = req.imageBase64();
        boolean validImage = StrUtil.isNotBlank(base64) && base64.length() > 200;
        if (!faceMock) {
            throw new BusinessException(503, "人脸核验服务未配置，请联系管理员");
        }
        String data = base64 == null ? "" : base64;
        int comma = data.indexOf(',');
        if (data.startsWith("data:") && comma > 0) {
            data = data.substring(comma + 1);
        }
        byte[] bytes;
        try {
            bytes = validImage ? Base64.getDecoder().decode(data) : new byte[0];
        } catch (IllegalArgumentException e) {
            bytes = new byte[0];
        }
        double similarity = (validImage && bytes.length > 0) ? mockPassSimilarity : mockFailSimilarity;
        boolean passed = bytes.length > 0 && similarity >= faceThreshold;
        if (bytes.length > 0) {
            String url = saveVerifyImage(record.getUserId(), bytes);
            log.setImgUrl(url);
        }
        if (passed) {
            handleVerifyPass(record, log, similarity, gear);
            return verifyResult(log, true, "核验通过，继续学习");
        }
        handleVerifyFailure(record, log, similarity, bytes.length == 0 ? "未获取到有效人脸照片" : "人脸相似度低于阈值", gear);
        return verifyResult(log, false, "核验失败，请正对摄像头重试");
    }

    @Transactional
    public Map<String, Object> sendSmsCode(LoginUser current, Long logId) {
        UserVerifyLog log = requireVerifyLog(current.getId(), logId);
        if (log.getStatus() != null && log.getStatus() != 0) {
            throw new BusinessException("该核验记录已处理");
        }
        SysUser user = userMapper.selectById(current.getId());
        if (user == null || StrUtil.isBlank(user.getPhone())) {
            throw new BusinessException("账号未绑定手机号，无法使用短信核验");
        }
        String resendKey = "sms:verify:resend:" + logId;
        if (Boolean.TRUE.equals(redis.hasKey(resendKey))) {
            Long ttl = redis.getExpire(resendKey, java.util.concurrent.TimeUnit.SECONDS);
            throw new BusinessException("验证码发送过于频繁，请 " + (ttl == null ? smsResendSeconds : ttl) + " 秒后再试");
        }
        String dayKey = "sms:verify:daily:" + current.getId() + ":" + LocalDate.now().format(DAY);
        String countStr = redis.opsForValue().get(dayKey);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        int limit = configInt("sms.dailyLimit", smsDailyLimit);
        if (count >= limit) {
            throw new BusinessException("今日短信验证码发送次数已达上限");
        }
        String code = RandomUtil.randomNumbers(6);
        redis.opsForValue().set("sms:verify:code:" + logId, code, Duration.ofSeconds(smsExpireSeconds));
        redis.opsForValue().set(resendKey, "1", Duration.ofSeconds(smsResendSeconds));
        redis.opsForValue().set(dayKey, String.valueOf(count + 1), Duration.ofDays(1));
        log.setVerifyType(2);
        log.setSmsPhone(maskPhone(user.getPhone()));
        verifyMapper.updateById(log);
        StudyService.log.info("[短信服务-模拟] 发送核验验证码至 {}，code={}", maskPhone(user.getPhone()), code);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sent", true);
        map.put("phone", maskPhone(user.getPhone()));
        map.put("expireSeconds", smsExpireSeconds);
        map.put("resendSeconds", smsResendSeconds);
        if (smsMock) {
            map.put("mockCode", code);
        }
        return map;
    }

    @Transactional
    public Map<String, Object> submitSms(LoginUser current, SmsVerifySubmitRequest req) {
        UserVerifyLog log = requireVerifyLog(current.getId(), req.logId());
        if (log.getStatus() != null && log.getStatus() != 0) {
            throw new BusinessException("该核验记录已处理");
        }
        Course course = courseMapper.selectById(log.getCourseId());
        ensureProjectWritable(course == null ? null : course.getProjectId());
        GearConfig gear = gearOf(course);
        StudyRecord record = studyMapper.selectById(log.getStudyRecordId());
        if (timedOut(log, gear)) {
            handleVerifyTimeout(record, log, gear);
            Map<String, Object> map = verifyResult(log, false, "核验已超时，本次核验不通过");
            map.put("timeout", true);
            return map;
        }
        String key = "sms:verify:code:" + log.getId();
        String code = redis.opsForValue().get(key);
        if (code == null) {
            handleVerifyFailure(record, log, null, "验证码已过期", gear);
            return verifyResult(log, false, "验证码已过期，请重新获取");
        }
        if (!code.equals(req.code().trim())) {
            handleVerifyFailure(record, log, null, "验证码错误", gear);
            return verifyResult(log, false, "验证码错误");
        }
        redis.delete(key);
        handleVerifyPass(record, log, null, gear);
        return verifyResult(log, true, "核验通过，继续学习");
    }

    private void handleVerifyPass(StudyRecord record, UserVerifyLog log, Double similarity, GearConfig gear) {
        log.setStatus(1);
        log.setVerifyResult(1);
        log.setSimilarity(similarity == null ? null : BigDecimal.valueOf(similarity).setScale(4, RoundingMode.HALF_UP));
        log.setFailReason(null);
        verifyMapper.updateById(log);
        if (record != null) {
            record.setVerifySuccess(nz(record.getVerifySuccess()) + 1);
            record.setLastHeartbeat(LocalDateTime.now());
            studyMapper.updateById(record);
            redis.delete("study:verify:fail:" + record.getId());
            redis.delete("study:lock:" + record.getId());
            if (gear.intervalMinute() > 0) {
                redis.opsForValue().set("study:verify:last:" + record.getId(),
                        String.valueOf(LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.ofHours(8))),
                        Duration.ofHours(6));
            }
        }
    }

    private void handleVerifyFailure(StudyRecord record, UserVerifyLog log, Double similarity, String reason, GearConfig gear) {
        log.setStatus(2);
        log.setVerifyResult(0);
        log.setSimilarity(similarity == null ? null : BigDecimal.valueOf(similarity).setScale(4, RoundingMode.HALF_UP));
        log.setFailReason(reason);
        verifyMapper.updateById(log);
        if (record != null) {
            record.setVerifyFail(nz(record.getVerifyFail()) + 1);
            studyMapper.updateById(record);
            registerVerifyFailure(record, gear);
        }
    }

    private void handleVerifyTimeout(StudyRecord record, UserVerifyLog log, GearConfig gear) {
        log.setStatus(3);
        log.setVerifyResult(2);
        log.setFailReason("核验超时");
        verifyMapper.updateById(log);
        if (record != null) {
            record.setVerifyFail(nz(record.getVerifyFail()) + 1);
            studyMapper.updateById(record);
            registerVerifyFailure(record, gear);
        }
    }

    private void registerVerifyFailure(StudyRecord record, GearConfig gear) {
        String key = "study:verify:fail:" + record.getId();
        Long fails = redis.opsForValue().increment(key);
        redis.expire(key, Duration.ofHours(24));
        if (fails != null && fails >= gear.maxFailCount()) {
            markAbnormal(record, "身份核验连续失败/超时 " + fails + " 次，超过上限 " + gear.maxFailCount() + " 次");
        }
    }

    private boolean timedOut(UserVerifyLog log, GearConfig gear) {
        return log.getVerifyTime() != null && log.getVerifyTime().plusSeconds(gear.timeoutSecond()).isBefore(LocalDateTime.now());
    }

    private UserVerifyLog createVerifyLog(StudyRecord record, String triggerPoint, GearConfig gear) {
        UserVerifyLog log = new UserVerifyLog();
        log.setUserId(record.getUserId());
        log.setCourseId(record.getCourseId());
        log.setProjectId(record.getProjectId());
        log.setStudyRecordId(record.getId());
        log.setTriggerPoint(triggerPoint);
        log.setVerifyTime(LocalDateTime.now());
        log.setVerifyType(1);
        log.setStatus(0);
        verifyMapper.insert(log);
        record.setVerifyTotal(nz(record.getVerifyTotal()) + 1);
        studyMapper.updateById(record);
        return log;
    }

    private UserVerifyLog pendingVerify(Long recordId) {
        return verifyMapper.selectOne(new LambdaQueryWrapper<UserVerifyLog>()
                .eq(UserVerifyLog::getStudyRecordId, recordId).eq(UserVerifyLog::getStatus, 0)
                .orderByDesc(UserVerifyLog::getId).last("limit 1"));
    }

    private UserVerifyLog requireVerifyLog(Long userId, Long logId) {
        UserVerifyLog log = verifyMapper.selectById(logId);
        if (log == null || !log.getUserId().equals(userId)) {
            throw new BusinessException(404, "核验记录不存在");
        }
        return log;
    }

    private Map<String, Object> verifyMap(UserVerifyLog log, GearConfig gear, boolean withMessage) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("logId", log.getId());
        m.put("triggerPoint", log.getTriggerPoint());
        m.put("verifyType", log.getVerifyType());
        m.put("timeoutSeconds", gear.timeoutSecond());
        long elapsed = log.getVerifyTime() == null ? 0
                : Math.max(0, Duration.between(log.getVerifyTime(), LocalDateTime.now()).getSeconds());
        m.put("remainingSeconds", Math.max(0, gear.timeoutSecond() - elapsed));
        m.put("smsAvailable", true);
        if (withMessage) {
            m.put("message", "为确认是本人学习，请完成本人核验，非本人核验将不计入学时学分。");
        }
        return m;
    }

    private Map<String, Object> verifyResult(UserVerifyLog log, boolean passed, String message) {
        StudyRecord record = log.getStudyRecordId() == null ? null : studyMapper.selectById(log.getStudyRecordId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("passed", passed);
        m.put("message", message);
        m.put("logId", log.getId());
        m.put("similarity", log.getSimilarity());
        m.put("failReason", log.getFailReason());
        m.put("verifySuccess", record == null ? 0 : nz(record.getVerifySuccess()));
        m.put("verifyFail", record == null ? 0 : nz(record.getVerifyFail()));
        if (record != null) {
            m.put("locked", isLocked(record.getId()));
            String failStr = redis.opsForValue().get("study:verify:fail:" + record.getId());
            m.put("failCount", failStr == null ? 0 : Integer.parseInt(failStr));
        }
        return m;
    }

    private Map<String, Object> maybeTriggerVerify(StudyRecord record, GearConfig gear) {
        if (gear.gearCode() <= 0 || isLocked(record.getId())) {
            return null;
        }
        UserVerifyLog pending = pendingVerify(record.getId());
        if (pending != null) {
            return verifyMap(pending, gear, true);
        }
        BigDecimal progress = record.getProgress() == null ? BigDecimal.ZERO : record.getProgress();
        for (Integer point : gear.triggerPoints()) {
            if (progress.compareTo(BigDecimal.valueOf(point)) >= 0) {
                String trigger = "pct:" + point;
                long passed = verifyMapper.selectCount(new LambdaQueryWrapper<UserVerifyLog>()
                        .eq(UserVerifyLog::getStudyRecordId, record.getId())
                        .eq(UserVerifyLog::getTriggerPoint, trigger)
                        .eq(UserVerifyLog::getStatus, 1));
                if (passed == 0) {
                    UserVerifyLog log = createVerifyLog(record, trigger, gear);
                    return verifyMap(log, gear, true);
                }
            }
        }
        if (gear.intervalMinute() > 0) {
            int seconds = gear.intervalMinute() * 60;
            if (nz(record.getValidDuration()) >= seconds) {
                String lastKey = "study:verify:last:" + record.getId();
                String last = redis.opsForValue().get(lastKey);
                long now = LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.ofHours(8));
                long lastEpoch = last == null
                        ? (record.getStartTime() == null ? now : record.getStartTime().toEpochSecond(java.time.ZoneOffset.ofHours(8)))
                        : Long.parseLong(last);
                if (now - lastEpoch >= seconds) {
                    UserVerifyLog log = createVerifyLog(record, "int:" + gear.intervalMinute(), gear);
                    return verifyMap(log, gear, true);
                }
            }
        }
        return null;
    }

    // ---------------- 完成与学分 ----------------

    private Map<String, Object> complete(StudyRecord record, Course course) {
        record.setStatus(2);
        record.setEndTime(LocalDateTime.now());
        record.setProgress(BigDecimal.valueOf(100));
        studyMapper.updateById(record);
        boolean needExam = course.getNeedExam() != null && course.getNeedExam() == 1;
        boolean passed = !needExam || examMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, record.getUserId()).eq(ExamRecord::getCourseId, course.getId())
                .eq(ExamRecord::getIsPass, 1)) > 0;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("completed", true);
        if (needExam && !passed) {
            m.put("nextAction", "EXAM");
            m.put("examUrl", "/m/exam/" + course.getId());
            m.put("creditAwarded", false);
            notificationService.sendByTemplate(NotificationTemplates.STUDY_FINISHED, record.getUserId(),
                    Map.of("courseName", course.getCourseName(), "courseId", course.getId()), course.getId());
        } else {
            creditService.awardCourseCredit(record.getUserId(), record.getProjectId(), course.getId(),
                    course.getCredit(), "完成课程《" + course.getCourseName() + "》自动发放");
            m.put("nextAction", "CREDIT");
            m.put("creditAwarded", true);
            updateMemberCompletion(record);
        }
        return m;
    }

    public void updateMemberCompletion(StudyRecord record) {
        TrainingProject project = projectMapper.selectById(record.getProjectId());
        if (project == null) {
            return;
        }
        BigDecimal total = creditService.projectCreditTotal(record.getUserId(), record.getProjectId());
        if (total.compareTo(project.getNeedCredit()) >= 0) {
            ProjectMember member = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, record.getProjectId())
                    .eq(ProjectMember::getUserId, record.getUserId()));
            if (member != null && (member.getCompletionStatus() == null || member.getCompletionStatus() == 0)) {
                member.setCompletionStatus(1);
                memberMapper.updateById(member);
            }
        }
    }

    // ---------------- 状态与辅助 ----------------

    public Map<String, Object> state(StudyRecord record, Course course, GearConfig gear, Map<String, Object> extra) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("courseId", course.getId());
        m.put("courseName", course.getCourseName());
        m.put("cover", course.getCover());
        m.put("videoUrl", course.getVideoUrl());
        m.put("videoDuration", course.getVideoDuration());
        m.put("credit", course.getCredit());
        m.put("needExam", course.getNeedExam());
        m.put("passScore", course.getPassScore());
        m.put("examLimit", course.getExamLimit());
        m.put("allowSeek", course.getAllowSeek());
        m.put("attachments", attachmentMapper.selectList(new LambdaQueryWrapper<CourseAttachment>()
                .eq(CourseAttachment::getCourseId, course.getId()).orderByAsc(CourseAttachment::getId)));
        m.put("studyRecordId", record.getId());
        m.put("status", record.getStatus());
        m.put("validDuration", nz(record.getValidDuration()));
        m.put("progress", record.getProgress());
        m.put("lastPosition", nz(record.getLastPosition()));
        m.put("verifyCount", nz(record.getVerifyCount()));
        m.put("verifyTotal", nz(record.getVerifyTotal()));
        m.put("verifySuccess", nz(record.getVerifySuccess()));
        m.put("verifyFail", nz(record.getVerifyFail()));
        m.put("screenSwitchCount", nz(record.getScreenSwitchCount()));
        m.put("abnormal", record.getIsAbnormal() != null && record.getIsAbnormal() == 1);
        m.put("abnormalReason", record.getAbnormalReason());
        m.put("gearCode", gear.gearCode());
        m.put("gearName", gear.gearName());
        m.put("triggerPoints", gear.triggerPoints());
        m.put("intervalMinute", gear.intervalMinute());
        m.put("heartbeatSeconds", heartbeatSeconds);
        m.put("locked", isLocked(record.getId()));
        if (extra != null) {
            m.putAll(extra);
        }
        return m;
    }

    public GearConfig gearOf(Course course) {
        VerifyGear gear = course == null ? null : gearMapper.selectOne(new LambdaQueryWrapper<VerifyGear>()
                .eq(VerifyGear::getGearCode, course.getVerifyGearCode() == null ? 0 : course.getVerifyGearCode()));
        int gearCode = course == null || course.getVerifyGearCode() == null ? 0 : course.getVerifyGearCode();
        String name = gear == null ? "不验证" : gear.getGearName();
        List<Integer> points = new ArrayList<>();
        int interval = gear == null || gear.getIntervalMinute() == null ? 0 : gear.getIntervalMinute();
        int timeout = gear == null || gear.getTimeoutSecond() == null ? 60 : gear.getTimeoutSecond();
        int maxFail = gear == null || gear.getMaxFailCount() == null ? 3 : gear.getMaxFailCount();
        int strategy = gear == null || gear.getFailStrategy() == null ? 1 : gear.getFailStrategy();
        String json = course == null ? null : course.getVerifyGearJson();
        if (StrUtil.isNotBlank(json)) {
            try {
                if (json.trim().startsWith("{")) {
                    JSONObject obj = JSONUtil.parseObj(json);
                    JSONArray arr = obj.getJSONArray("triggerPoints");
                    if (arr != null) {
                        arr.forEach(v -> points.add(Integer.parseInt(String.valueOf(v))));
                    }
                    if (obj.containsKey("intervalMinute")) {
                        interval = obj.getInt("intervalMinute", interval);
                    }
                    if (obj.containsKey("timeoutSecond")) {
                        timeout = obj.getInt("timeoutSecond", timeout);
                    }
                    if (obj.containsKey("maxFailCount")) {
                        maxFail = obj.getInt("maxFailCount", maxFail);
                    }
                    if (obj.containsKey("failStrategy")) {
                        strategy = obj.getInt("failStrategy", strategy);
                    }
                } else {
                    JSONArray arr = JSONUtil.parseArray(json);
                    arr.forEach(v -> points.add(Integer.parseInt(String.valueOf(v))));
                }
            } catch (Exception e) {
                log.warn("解析课程核验档位配置失败: courseId={}, json={}", course == null ? null : course.getId(), json);
            }
        }
        if (points.isEmpty() && gear != null && StrUtil.isNotBlank(gear.getTriggerPoints())) {
            try {
                JSONArray arr = JSONUtil.parseArray(gear.getTriggerPoints());
                arr.forEach(v -> points.add(Integer.parseInt(String.valueOf(v))));
            } catch (Exception ignored) {
            }
        }
        return new GearConfig(gearCode, name, points, interval, Math.max(10, timeout), Math.max(1, maxFail), strategy);
    }

    public record GearConfig(int gearCode, String gearName, List<Integer> triggerPoints,
                             int intervalMinute, int timeoutSecond, int maxFailCount, int failStrategy) {
    }

    private Course requireCourse(Long courseId, boolean mustOnline) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (mustOnline && (course.getStatus() == null || course.getStatus() != 1)) {
            throw new BusinessException("课程未上架或已下架");
        }
        return course;
    }

    private void ensureMember(Long userId, Long projectId) {
        if (memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, userId)
                .eq(ProjectMember::getStatus, 1)) == 0) {
            throw new BusinessException(403, "您不在该培训班中，无法学习该课程");
        }
    }

    private void ensureProjectOpen(TrainingProject project) {
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (project.getStartTime() != null && now.isBefore(project.getStartTime())) {
            throw new BusinessException("培训班尚未开始");
        }
        if (project.getEndTime() != null && now.isAfter(project.getEndTime())
                && (project.getAllowOverdue() == null || project.getAllowOverdue() == 0)) {
            throw new BusinessException("培训班已结束，无法继续学习");
        }
        if (project.getStatus() != null && project.getStatus() == 3) {
            throw new BusinessException("培训班已归档");
        }
    }

    private void ensureProjectWritable(Long projectId) {
        if (projectId == null) {
            return;
        }
        TrainingProject project = projectMapper.selectById(projectId);
        if (project != null && project.getStatus() != null && project.getStatus() == 3) {
            throw new BusinessException("培训班已归档，仅可查看");
        }
    }

    private StudyRecord findRecord(Long userId, Long courseId) {
        return studyMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, userId).eq(StudyRecord::getCourseId, courseId));
    }

    private StudyRecord requireActiveSession(LoginUser current, Long courseId, String sessionId) {
        StudyRecord record = findRecord(current.getId(), courseId);
        if (record == null) {
            throw new BusinessException("请先开始学习");
        }
        String session = redis.opsForValue().get(sessionKey(current.getId(), courseId));
        if (session == null || !session.equals(sessionId)) {
            throw new BusinessException(409, "学习会话已失效，请重新进入课程");
        }
        return record;
    }

    private String sessionKey(Long userId, Long courseId) {
        return "study:session:" + userId + ":" + courseId;
    }

    private boolean isLocked(Long recordId) {
        return Boolean.TRUE.equals(redis.hasKey("study:lock:" + recordId));
    }

    public boolean isLockedPublic(Long recordId) {
        return isLocked(recordId);
    }

    public void markAbnormal(StudyRecord record, String reason) {
        record.setIsAbnormal(1);
        String old = record.getAbnormalReason();
        String merged = StrUtil.isBlank(old) ? reason : (old.contains(reason) ? old : old + "；" + reason);
        if (merged.length() > 480) {
            merged = merged.substring(merged.length() - 480);
        }
        record.setAbnormalReason(merged);
        studyMapper.updateById(record);
        redis.opsForValue().set("study:lock:" + record.getId(), "1", Duration.ofHours(24));
        Course course = courseMapper.selectById(record.getCourseId());
        Long teacherId = course == null ? null : course.getCreateUserId();
        if (teacherId != null) {
            SysUser student = userMapper.selectById(record.getUserId());
            notificationService.sendByTemplate(NotificationTemplates.STUDY_ABNORMAL, teacherId,
                    Map.of("studentName", student == null ? "" : student.getRealName(),
                            "courseName", course == null ? "" : course.getCourseName(),
                            "reason", StrUtil.nullToEmpty(reason)),
                    record.getId());
        }
    }

    public void unlock(Long recordId, boolean clearAbnormal) {
        redis.delete("study:lock:" + recordId);
        redis.delete("study:verify:fail:" + recordId);
        if (clearAbnormal) {
            StudyRecord record = studyMapper.selectById(recordId);
            if (record != null) {
                record.setIsAbnormal(0);
                studyMapper.updateById(record);
            }
        }
    }

    public void unlockByManager(LoginUser current, Long recordId, boolean clearAbnormal, String remark) {
        StudyRecord record = studyMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("学习记录不存在");
        }
        Course course = courseMapper.selectById(record.getCourseId());
        boolean admin = current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1);
        boolean owner = "teacher".equals(current.getRoleCode()) && course != null
                && current.getId().equals(course.getCreateUserId());
        if (!admin && !owner) {
            throw new BusinessException(403, "仅管理员或该课程教员可解除学习限制");
        }
        ensureProjectWritable(record.getProjectId());
        unlock(recordId, clearAbnormal);
        log.info("解除学习限制: recordId={}, operator={}, remark={}", recordId, current.getUsername(), remark);
    }

    private String saveVerifyImage(Long userId, byte[] bytes) {
        try {
            String day = LocalDate.now().format(DAY);
            Path dir = Paths.get(uploadDir, "verify", day);
            Files.createDirectories(dir);
            String filename = userId + "_" + IdUtil.fastSimpleUUID() + ".jpg";
            Files.write(dir.resolve(filename), bytes);
            return "/uploads/verify/" + day + "/" + filename;
        } catch (IOException e) {
            log.error("保存核验抓拍图失败", e);
            return null;
        }
    }

    private int configInt(String key, int defaultValue) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (config == null || StrUtil.isBlank(config.getConfigValue())) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(config.getConfigValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String currentDevice() {
        try {
            String ua = ((org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest()
                    .getHeader("User-Agent");
            return StrUtil.maxLength(ua, 250);
        } catch (Exception e) {
            return null;
        }
    }

    private String currentIp() {
        try {
            return ((org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest()
                    .getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
