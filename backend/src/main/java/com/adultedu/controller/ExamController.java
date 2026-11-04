package com.adultedu.controller;

import com.adultedu.aspect.OperLog;
import com.adultedu.common.PageResult;
import com.adultedu.common.R;
import com.adultedu.dto.BizDtos.ExamSubmitRequest;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.ExamService;
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
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;

    @GetMapping("/{courseId}/paper")
    public R<Map<String, Object>> paper(@PathVariable Long courseId) {
        return R.ok(examService.paper(SecurityUtils.current(), courseId));
    }

    @PostMapping("/{courseId}/submit")
    @OperLog(module = "课后测验", content = "提交测验答卷")
    public R<Map<String, Object>> submit(@PathVariable Long courseId, @Valid @RequestBody ExamSubmitRequest req) {
        return R.ok(examService.submit(SecurityUtils.current(), courseId, req));
    }

    @GetMapping("/my")
    public R<List<Map<String, Object>>> my(@RequestParam(required = false) Long courseId) {
        return R.ok(examService.myRecords(SecurityUtils.current(), courseId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('study:monitor')")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize,
                                                   @RequestParam(required = false) Long courseId,
                                                   @RequestParam(required = false) Long projectId,
                                                   @RequestParam(required = false) Long userId,
                                                   @RequestParam(required = false) Integer isPass) {
        return R.ok(examService.recordPage(SecurityUtils.current(), pageNum, pageSize, courseId, projectId, userId, isPass));
    }
}
