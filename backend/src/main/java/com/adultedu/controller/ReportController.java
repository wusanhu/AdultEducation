package com.adultedu.controller;

import com.adultedu.aspect.OperLog;
import com.adultedu.common.R;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('report:view')")
    public R<Map<String, Object>> overview() {
        return R.ok(reportService.overview(SecurityUtils.current()));
    }

    @GetMapping("/org")
    @PreAuthorize("hasAuthority('report:org')")
    public R<List<Map<String, Object>>> org(@RequestParam(required = false) Long projectId) {
        return R.ok(reportService.orgSummary(SecurityUtils.current(), projectId));
    }

    @GetMapping("/credit")
    @PreAuthorize("hasAuthority('report:credit')")
    public R<List<Map<String, Object>>> credit(@RequestParam(required = false) Long projectId) {
        return R.ok(reportService.creditReport(SecurityUtils.current(), projectId));
    }

    @GetMapping("/course")
    @PreAuthorize("hasAuthority('report:course')")
    public R<List<Map<String, Object>>> course(@RequestParam(required = false) Long projectId) {
        return R.ok(reportService.courseReport(SecurityUtils.current(), projectId));
    }

    @GetMapping("/warnings")
    @PreAuthorize("hasAuthority('report:view')")
    public R<List<Map<String, Object>>> warnings(@RequestParam(required = false) Long projectId,
                                                @RequestParam(required = false) String type) {
        return R.ok(reportService.warnings(SecurityUtils.current(), projectId, type));
    }

    @PostMapping("/warnings/notify")
    @PreAuthorize("hasAuthority('report:view')")
    @OperLog(module = "统计报表", content = "发送学习预警")
    public R<Integer> notifyWarnings(@RequestParam(required = false) Long projectId,
                                     @RequestParam(required = false) String type,
                                     @RequestParam(required = false) List<Long> userIds) {
        return R.ok(reportService.notifyWarnings(SecurityUtils.current(), projectId, type, userIds));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('report:export')")
    @OperLog(module = "统计报表", content = "导出报表")
    public void export(@RequestParam(defaultValue = "credit") String type,
                       @RequestParam(required = false) Long projectId,
                       HttpServletResponse response) throws IOException {
        reportService.export(SecurityUtils.current(), type, projectId, response);
    }
}
