package com.adultedu.service;

import com.adultedu.common.BusinessException;
import com.adultedu.common.PageResult;
import com.adultedu.entity.Notification;
import com.adultedu.entity.SysUser;
import com.adultedu.mapper.NotificationMapper;
import com.adultedu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final SysUserMapper userMapper;

    public void send(Long userId, String title, String content, String type, Long bizId) {
        if (userId == null) {
            return;
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setBizId(bizId);
        n.setIsRead(0);
        notificationMapper.insert(n);
        SysUser user = userMapper.selectById(userId);
        if (user != null && user.getOpenId() != null && !user.getOpenId().isBlank()) {
            log.info("[微信服务号模板消息-模拟] to={}, title={}", user.getOpenId(), title);
        }
    }

    @Async
    public void sendAsync(Long userId, String title, String content, String type, Long bizId) {
        send(userId, title, content, type, bizId);
    }

    public void sendBatch(List<Long> userIds, String title, String content, String type, Long bizId) {
        userIds.forEach(id -> send(id, title, content, type, bizId));
    }

    public int unread(Long userId) {
        return Math.toIntExact(notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId).eq(Notification::getIsRead, 0)));
    }

    public PageResult<Notification> page(Long userId, long pageNum, long pageSize, Integer isRead, String type) {
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(isRead != null, Notification::getIsRead, isRead)
                .eq(type != null && !type.isBlank(), Notification::getType, type)
                .orderByDesc(Notification::getId);
        return PageResult.of(notificationMapper.selectPage(new Page<>(pageNum, pageSize), qw));
    }

    public void markRead(Long userId, Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null || !n.getUserId().equals(userId)) {
            throw new BusinessException("通知不存在");
        }
        if (n.getIsRead() == null || n.getIsRead() == 0) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
    }

    public int markAllRead(Long userId) {
        Notification update = new Notification();
        update.setIsRead(1);
        return notificationMapper.update(update, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId).eq(Notification::getIsRead, 0));
    }

    public void delete(Long userId, Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null || !n.getUserId().equals(userId)) {
            throw new BusinessException("通知不存在");
        }
        notificationMapper.deleteById(id);
    }
}
