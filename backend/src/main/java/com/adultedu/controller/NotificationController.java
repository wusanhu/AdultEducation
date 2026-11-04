package com.adultedu.controller;

import com.adultedu.common.PageResult;
import com.adultedu.common.R;
import com.adultedu.entity.Notification;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public R<PageResult<Notification>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) Integer isRead,
                                            @RequestParam(required = false) String type) {
        return R.ok(notificationService.page(SecurityUtils.userId(), pageNum, pageSize, isRead, type));
    }

    @GetMapping("/unread-count")
    public R<Integer> unreadCount() {
        return R.ok(notificationService.unread(SecurityUtils.userId()));
    }

    @PostMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        notificationService.markRead(SecurityUtils.userId(), id);
        return R.ok();
    }

    @PostMapping("/read-all")
    public R<Integer> readAll() {
        return R.ok(notificationService.markAllRead(SecurityUtils.userId()));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        notificationService.delete(SecurityUtils.userId(), id);
        return R.ok();
    }
}
