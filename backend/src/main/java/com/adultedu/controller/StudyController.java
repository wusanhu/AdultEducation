package com.adultedu.controller;

import com.adultedu.common.R;
import com.adultedu.dto.BizDtos.HeartbeatRequest;
import com.adultedu.dto.BizDtos.PopupConfirmRequest;
import com.adultedu.dto.BizDtos.SmsVerifySubmitRequest;
import com.adultedu.dto.BizDtos.StudyStartRequest;
import com.adultedu.dto.BizDtos.VerifyStartRequest;
import com.adultedu.dto.BizDtos.VerifySubmitRequest;
import com.adultedu.aspect.OperLog;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.StudyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {
    private final StudyService studyService;

    @PostMapping("/start")
    public R<Map<String, Object>> start(@Valid @RequestBody StudyStartRequest req) {
        return R.ok(studyService.start(SecurityUtils.current(), req.courseId()));
    }

    @PostMapping("/heartbeat")
    public R<Map<String, Object>> heartbeat(@Valid @RequestBody HeartbeatRequest req) {
        return R.ok(studyService.heartbeat(SecurityUtils.current(), req));
    }

    @PostMapping("/pause")
    public R<Map<String, Object>> pause(@RequestParam Long courseId) {
        return R.ok(studyService.pause(SecurityUtils.current(), courseId));
    }

    @PostMapping("/popup/confirm")
    public R<Map<String, Object>> confirmPopup(@Valid @RequestBody PopupConfirmRequest req) {
        return R.ok(studyService.confirmPopup(SecurityUtils.current(), req.courseId(), req.sessionId()));
    }

    @PostMapping("/verify/start")
    public R<Map<String, Object>> startVerify(@Valid @RequestBody VerifyStartRequest req) {
        return R.ok(studyService.startVerify(SecurityUtils.current(), req));
    }

    @PostMapping("/verify/face")
    public R<Map<String, Object>> submitFace(@Valid @RequestBody VerifySubmitRequest req) {
        return R.ok(studyService.submitFace(SecurityUtils.current(), req));
    }

    @PostMapping("/verify/sms/send")
    public R<Map<String, Object>> sendSms(@RequestParam Long logId) {
        return R.ok(studyService.sendSmsCode(SecurityUtils.current(), logId));
    }

    @PostMapping("/verify/sms/submit")
    public R<Map<String, Object>> submitSms(@Valid @RequestBody SmsVerifySubmitRequest req) {
        return R.ok(studyService.submitSms(SecurityUtils.current(), req));
    }

    @GetMapping("/state")
    public R<Map<String, Object>> state(@RequestParam Long courseId) {
        return R.ok(studyService.learningState(SecurityUtils.current(), courseId));
    }

    @PostMapping("/records/{id}/unlock")
    @PreAuthorize("hasAuthority('study:monitor')")
    @OperLog(module = "学习监控", content = "解除学习限制")
    public R<Void> unlock(@PathVariable Long id,
                          @RequestParam(defaultValue = "true") boolean clearAbnormal,
                          @RequestParam(required = false) String remark) {
        studyService.unlockByManager(SecurityUtils.current(), id, clearAbnormal, remark);
        return R.ok();
    }
}
