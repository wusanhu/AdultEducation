package com.adultedu.controller;

import com.adultedu.aspect.OperLog;
import com.adultedu.common.PageResult;
import com.adultedu.common.R;
import com.adultedu.dto.BizDtos.AuditRequest;
import com.adultedu.dto.BizDtos.GraduateApplyRequest;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.GraduateService;
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
@RequestMapping("/api/graduate")
@RequiredArgsConstructor
public class GraduateController {
    private final GraduateService graduateService;

    @GetMapping("/eligibility")
    public R<Map<String, Object>> eligibility(@RequestParam Long projectId) {
        return R.ok(graduateService.eligibility(SecurityUtils.current(), projectId));
    }

    @PostMapping("/apply")
    @OperLog(module = "毕业审核", content = "提交毕业申请")
    public R<Long> apply(@Valid @RequestBody GraduateApplyRequest req) {
        return R.ok(graduateService.apply(SecurityUtils.current(), req));
    }

    @GetMapping("/my")
    public R<Map<String, Object>> my(@RequestParam(required = false) Long projectId) {
        return R.ok(graduateService.myApplication(SecurityUtils.current(), projectId));
    }

    @GetMapping("/my/list")
    public R<List<Map<String, Object>>> myList() {
        return R.ok(graduateService.myApplications(SecurityUtils.current()));
    }

    @GetMapping("/my/certificate")
    public R<Map<String, Object>> myCertificate(@RequestParam(required = false) Long projectId) {
        return R.ok(graduateService.myCertificate(SecurityUtils.current(), projectId));
    }

    @GetMapping("/applies")
    @PreAuthorize("hasAuthority('graduate:list')")
    public R<PageResult<Map<String, Object>>> applies(@RequestParam(defaultValue = "1") long pageNum,
                                                      @RequestParam(defaultValue = "10") long pageSize,
                                                      @RequestParam(required = false) Long projectId,
                                                      @RequestParam(required = false) Integer auditStatus,
                                                      @RequestParam(required = false) String currentNode,
                                                      @RequestParam(required = false) String keyword) {
        return R.ok(graduateService.applyPage(SecurityUtils.current(), pageNum, pageSize, projectId,
                auditStatus, currentNode, keyword));
    }

    @GetMapping("/applies/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(graduateService.detail(SecurityUtils.current(), id));
    }

    @PostMapping("/applies/{id}/audit")
    @PreAuthorize("hasAuthority('graduate:list')")
    @OperLog(module = "毕业审核", content = "审批毕业申请")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditRequest req) {
        graduateService.audit(SecurityUtils.current(), id, req);
        return R.ok();
    }

    @GetMapping("/files")
    @PreAuthorize("hasAuthority('graduate:file') or hasAuthority('graduate:list')")
    public R<PageResult<Map<String, Object>>> files(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) Long projectId,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String year) {
        return R.ok(graduateService.filePage(SecurityUtils.current(), pageNum, pageSize, projectId, keyword, year));
    }
}
