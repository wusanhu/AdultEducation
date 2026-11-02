package com.adultedu.controller;

import com.adultedu.aspect.OperLog;
import com.adultedu.common.PageResult;
import com.adultedu.common.R;
import com.adultedu.dto.BizDtos.VerifyReviewRequest;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.VerifyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
public class VerifyController {
    private final VerifyService verifyService;

    @GetMapping("/records")
    @PreAuthorize("hasAuthority('verify:list')")
    public R<PageResult<Map<String, Object>>> records(@RequestParam(defaultValue = "1") long pageNum,
                                                      @RequestParam(defaultValue = "10") long pageSize,
                                                      @RequestParam(required = false) Long projectId,
                                                      @RequestParam(required = false) Long courseId,
                                                      @RequestParam(required = false) Long userId,
                                                      @RequestParam(required = false) Integer verifyResult,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) String startDate,
                                                      @RequestParam(required = false) String endDate) {
        return R.ok(verifyService.recordPage(SecurityUtils.current(), pageNum, pageSize, projectId, courseId,
                userId, verifyResult, status, startDate, endDate));
    }

    @GetMapping("/abnormal")
    @PreAuthorize("hasAuthority('verify:abnormal')")
    public R<PageResult<Map<String, Object>>> abnormal(@RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize,
                                                       @RequestParam(required = false) Long projectId,
                                                       @RequestParam(required = false) Long courseId,
                                                       @RequestParam(required = false) String keyword) {
        return R.ok(verifyService.abnormalPage(SecurityUtils.current(), pageNum, pageSize, projectId, courseId, keyword));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('verify:abnormal')")
    @OperLog(module = "身份核验", content = "复核心验异常")
    public R<Void> review(@PathVariable Long id, @Valid @RequestBody VerifyReviewRequest req) {
        verifyService.review(SecurityUtils.current(), id, req);
        return R.ok();
    }

    @GetMapping("/my")
    public R<List<Map<String, Object>>> my(@RequestParam(required = false) Long courseId) {
        return R.ok(verifyService.myVerifyLogs(SecurityUtils.current(), courseId));
    }

    @GetMapping("/study-records")
    @PreAuthorize("hasAuthority('study:monitor')")
    public R<PageResult<Map<String, Object>>> studyRecords(@RequestParam(defaultValue = "1") long pageNum,
                                                           @RequestParam(defaultValue = "10") long pageSize,
                                                           @RequestParam(required = false) Long projectId,
                                                           @RequestParam(required = false) Long courseId,
                                                           @RequestParam(required = false) Long userId,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Integer abnormal,
                                                           @RequestParam(required = false) Integer status) {
        return R.ok(verifyService.monitorPage(SecurityUtils.current(), pageNum, pageSize, projectId, courseId,
                userId, keyword, abnormal, status));
    }

    @GetMapping("/study-records/{id}")
    @PreAuthorize("hasAuthority('study:monitor')")
    public R<Map<String, Object>> studyDetail(@PathVariable Long id) {
        return R.ok(verifyService.studyDetail(SecurityUtils.current(), id));
    }
}
