package com.adultedu.service;

import cn.hutool.core.util.StrUtil;
import com.adultedu.common.BusinessException;
import com.adultedu.common.PageResult;
import com.adultedu.entity.Notification;
import com.adultedu.entity.NotificationTemplate;
import com.adultedu.entity.SysUser;
import com.adultedu.mapper.NotificationMapper;
import com.adultedu.mapper.NotificationTemplateMapper;
import com.adultedu.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    /** 消息类别，管理端与移动端的分类标签、图标都以这套取值为准 */
    public static final List<String> CATEGORIES = List.of(
            "COURSE", "CREDIT", "GRADUATE", "APPROVAL", "WARNING", "VERIFY", "ACCOUNT", "SYSTEM");
    private static final Set<String> CATEGORY_SET = Set.copyOf(CATEGORIES);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    private final NotificationMapper notificationMapper;
    private final NotificationTemplateMapper templateMapper;
    private final SysUserMapper userMapper;

    /**
     * 按模板编码发送站内信；模板被停用或不存在时静默跳过，不影响业务动作本身。
     */
    public void sendByTemplate(String templateCode, Long userId, Map<String, Object> vars, Long bizId) {
        NotificationTemplate template = enabledTemplate(templateCode);
        if (template == null || userId == null) {
            return;
        }
        Map<String, Object> safeVars = vars == null ? Map.of() : vars;
        doSend(userId, render(template.getTitle(), safeVars), render(template.getContent(), safeVars),
                template.getType(), render(template.getMobileLink(), safeVars),
                render(template.getAdminLink(), safeVars), bizId);
    }

    public void sendBatchByTemplate(String templateCode, List<Long> userIds, Map<String, Object> vars, Long bizId) {
        NotificationTemplate template = enabledTemplate(templateCode);
        if (template == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        Map<String, Object> safeVars = vars == null ? Map.of() : vars;
        String title = render(template.getTitle(), safeVars);
        String content = render(template.getContent(), safeVars);
        String link = render(template.getMobileLink(), safeVars);
        String adminLink = render(template.getAdminLink(), safeVars);
        for (Long userId : userIds) {
            doSend(userId, title, content, template.getType(), link, adminLink, bizId);
        }
    }

    private NotificationTemplate enabledTemplate(String templateCode) {
        if (StrUtil.isBlank(templateCode)) {
            return null;
        }
        NotificationTemplate template = templateMapper.selectOne(new LambdaQueryWrapper<NotificationTemplate>()
                .eq(NotificationTemplate::getTemplateCode, templateCode).last("limit 1"));
        if (template == null) {
            log.warn("站内信模板缺失，消息未发送: code={}", templateCode);
            return null;
        }
        if (template.getEnabled() == null || template.getEnabled() != 1) {
            return null;
        }
        return template;
    }

    private String render(String template, Map<String, Object> vars) {
        if (StrUtil.isBlank(template)) {
            return null;
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Object value = vars.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        m.appendTail(sb);
        return StrUtil.trimToNull(sb.toString());
    }

    private void doSend(Long userId, String title, String content, String type, String link, String adminLink, Long bizId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(StrUtil.blankToDefault(type, "SYSTEM"));
        n.setBizId(bizId);
        n.setLink(link);
        n.setAdminLink(adminLink);
        n.setIsRead(0);
        notificationMapper.insert(n);
        SysUser user = userMapper.selectById(userId);
        if (user != null && StrUtil.isNotBlank(user.getOpenId())) {
            log.info("[微信服务号模板消息-模拟] to={}, title={}", user.getOpenId(), title);
        }
    }

    public List<NotificationTemplate> templates(String keyword, String type) {
        return templateMapper.selectList(new LambdaQueryWrapper<NotificationTemplate>()
                .eq(StrUtil.isNotBlank(type), NotificationTemplate::getType, type)
                .and(StrUtil.isNotBlank(keyword), w -> w.like(NotificationTemplate::getTemplateName, keyword)
                        .or().like(NotificationTemplate::getTemplateCode, keyword))
                .orderByAsc(NotificationTemplate::getType).orderByAsc(NotificationTemplate::getId));
    }

    /** 各类别已配置的模板数，未配置的类别返回 0，前端按此渲染分类标签 */
    public Map<String, Long> categoryCounts() {
        Map<String, Long> result = new LinkedHashMap<>();
        CATEGORIES.forEach(category -> result.put(category, 0L));
        for (NotificationTemplate t : templateMapper.selectList(null)) {
            result.merge(t.getType(), 1L, Long::sum);
        }
        return result;
    }

    public Long saveTemplate(NotificationTemplate req) {
        if (StrUtil.isBlank(req.getTemplateCode())) {
            throw new BusinessException("请填写模板编码");
        }
        if (!req.getTemplateCode().matches("[a-z][a-z0-9_]{1,63}")) {
            throw new BusinessException("模板编码只能由小写字母、数字与下划线组成，且以字母开头");
        }
        if (StrUtil.isBlank(req.getTemplateName())) {
            throw new BusinessException("请填写模板名称");
        }
        if (!CATEGORY_SET.contains(req.getType())) {
            throw new BusinessException("消息类别不合法");
        }
        if (StrUtil.isBlank(req.getTitle()) || StrUtil.isBlank(req.getContent())) {
            throw new BusinessException("标题与内容模板不能为空");
        }
        checkPlaceholders(req);
        NotificationTemplate same = templateMapper.selectOne(new LambdaQueryWrapper<NotificationTemplate>()
                .eq(NotificationTemplate::getTemplateCode, req.getTemplateCode()).last("limit 1"));
        if (same != null && !same.getId().equals(req.getId())) {
            throw new BusinessException("模板编码「" + req.getTemplateCode() + "」已存在");
        }
        req.setTemplateCode(req.getTemplateCode().trim());
        req.setMobileLink(StrUtil.trimToNull(req.getMobileLink()));
        req.setAdminLink(StrUtil.trimToNull(req.getAdminLink()));
        req.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        if (req.getId() == null) {
            templateMapper.insert(req);
        } else {
            if (templateMapper.selectById(req.getId()) == null) {
                throw new BusinessException("模板不存在");
            }
            templateMapper.updateById(req);
        }
        return req.getId();
    }

    /**
     * 链接模板必须整段是一条路由，避免运营把变量写成 http 外链造成越权跳转。
     */
    private void checkPlaceholders(NotificationTemplate req) {
        for (String link : List.of(StrUtil.emptyToDefault(req.getMobileLink(), ""), StrUtil.emptyToDefault(req.getAdminLink(), ""))) {
            if (StrUtil.isBlank(link)) {
                continue;
            }
            if (!link.startsWith("/") || link.startsWith("//") || link.contains("://")) {
                throw new BusinessException("跳转链接需填写站内路由，例如 /project/{projectId}");
            }
        }
    }

    public void deleteTemplate(Long id) {
        if (templateMapper.selectById(id) == null) {
            throw new BusinessException("模板不存在");
        }
        templateMapper.deleteById(id);
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
