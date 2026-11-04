package com.adultedu.controller;

import com.adultedu.aspect.OperLog;
import com.adultedu.common.PageResult;
import com.adultedu.common.R;
import com.adultedu.dto.BizDtos.CreditApplyRequest;
import com.adultedu.dto.BizDtos.CreditAuditRequest;
import com.adultedu.dto.BizDtos.CreditRevokeRequest;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.CreditService;
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
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {
    private final CreditService creditService;

    @GetMapping
    @PreAuthorize("hasAuthority('credit:list')")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long userId,
                                                   @RequestParam(required = false) Long projectId,
                                                   @RequestParam(required = false) Long courseId,
                                                   @RequestParam(required = false) Integer auditStatus,
                                                   @RequestParam(required = false) Integer sourceType,
                                                   @RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate) {
        return R.ok(creditService.ledgerPage(SecurityUtils.current(), pageNum, pageSize, keyword, userId, projectId,
                courseId, auditStatus, sourceType, startDate, endDate));
    }

    @GetMapping("/my")
    public R<List<Map<String, Object>>> my() {
        return R.ok(creditService.myCredits(SecurityUtils.current()));
    }

    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        return R.ok(creditService.summary(SecurityUtils.current()));
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAuthority('credit:apply')")
    @OperLog(module = "学分管理", content = "发起学分补录")
    public R<Long> apply(@Valid @RequestBody CreditApplyRequest req) {
        return R.ok(creditService.apply(SecurityUtils.current(), req));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('credit:audit')")
    public R<List<Map<String, Object>>> pending() {
        return R.ok(creditService.pendingAudits(SecurityUtils.current()));
    }

    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('credit:audit')")
    @OperLog(module = "学分管理", content = "审批学分补录")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody CreditAuditRequest req) {
        creditService.audit(SecurityUtils.current(), id, Boolean.TRUE.equals(req.approve()), req.remark());
        return R.ok();
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('credit:revoke')")
    @OperLog(module = "学分管理", content = "撤销学分")
    public R<Void> revoke(@PathVariable Long id, @Valid @RequestBody CreditRevokeRequest req) {
        creditService.revoke(SecurityUtils.current(), id, req.reason());
        return R.ok();
    }
}
