package com.adultedu.controller;

import com.adultedu.aspect.OperLog;
import com.adultedu.common.R;
import com.adultedu.entity.NotificationTemplate;
import com.adultedu.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/notice-templates")
@RequiredArgsConstructor
public class NoticeTemplateController {
    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:notice')")
    public R<List<NotificationTemplate>> list(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String type) {
        return R.ok(notificationService.templates(keyword, type));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('system:notice')")
    public R<Map<String, Long>> categories() {
        return R.ok(notificationService.categoryCounts());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:notice:edit')")
    @OperLog(module = "消息模板", content = "新增模板")
    public R<Long> create(@Valid @RequestBody NotificationTemplate req) {
        req.setId(null);
        return R.ok(notificationService.saveTemplate(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:notice:edit')")
    @OperLog(module = "消息模板", content = "编辑模板")
    public R<Long> update(@PathVariable Long id, @Valid @RequestBody NotificationTemplate req) {
        req.setId(id);
        return R.ok(notificationService.saveTemplate(req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:notice:edit')")
    @OperLog(module = "消息模板", content = "删除模板")
    public R<Void> delete(@PathVariable Long id) {
        notificationService.deleteTemplate(id);
        return R.ok();
    }
}
