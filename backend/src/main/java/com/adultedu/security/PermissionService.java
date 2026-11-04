package com.adultedu.security;

import com.adultedu.dto.MenuNode;
import com.adultedu.entity.SysMenu;
import com.adultedu.entity.SysRoleMenu;
import com.adultedu.mapper.SysMenuMapper;
import com.adultedu.mapper.SysRoleMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private static final String CACHE_PREFIX = "perms:role:";
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<String> perms(Long roleId, boolean superAdmin) {
        String key = CACHE_PREFIX + roleId;
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<String>>() {
                });
            }
        } catch (Exception ignored) {
        }
        List<SysMenu> menus = grantedMenus(roleId, superAdmin);
        List<String> perms = menus.stream().map(SysMenu::getPerms)
                .filter(p -> p != null && !p.isBlank()).distinct().toList();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(perms), Duration.ofMinutes(10));
        } catch (Exception ignored) {
        }
        return perms;
    }

    public List<MenuNode> menuTree(Long roleId, boolean superAdmin) {
        // 按钮（menuType=3）也随菜单下发，前端据此同时校验权限标识与隐藏状态
        List<SysMenu> menus = grantedMenus(roleId, superAdmin).stream()
                .filter(m -> m.getMenuType() != null)
                .sorted(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        Map<Long, MenuNode> nodes = menus.stream().collect(Collectors.toMap(SysMenu::getId, this::toNode, (a, b) -> a, java.util.LinkedHashMap::new));
        List<MenuNode> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            MenuNode node = nodes.get(menu.getId());
            if (menu.getParentId() == null || menu.getParentId() == 0 || !nodes.containsKey(menu.getParentId())) {
                roots.add(node);
            } else {
                nodes.get(menu.getParentId()).getChildren().add(node);
            }
        }
        roots.sort(Comparator.comparing(MenuNode::getSort, Comparator.nullsLast(Integer::compareTo)));
        return roots;
    }

    public void invalidate(Long roleId) {
        redisTemplate.delete(CACHE_PREFIX + roleId);
    }

    public void invalidateAll() {
        Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private List<SysMenu> grantedMenus(Long roleId, boolean superAdmin) {
        LambdaQueryWrapper<SysMenu> qw = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getSort);
        if (superAdmin) {
            return menuMapper.selectList(qw);
        }
        List<SysRoleMenu> links = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        Set<Long> ids = links.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return List.of();
        }
        qw.in(SysMenu::getId, ids);
        return menuMapper.selectList(qw);
    }

    private MenuNode toNode(SysMenu menu) {
        MenuNode node = new MenuNode();
        node.setId(menu.getId());
        node.setParentId(menu.getParentId());
        node.setMenuName(menu.getMenuName());
        node.setMenuType(menu.getMenuType());
        node.setPath(menu.getPath());
        node.setComponent(menu.getComponent());
        node.setIcon(menu.getIcon());
        node.setPerms(menu.getPerms());
        node.setSort(menu.getSort());
        node.setHidden(menu.getHidden());
        node.setMobileVisible(menu.getMobileVisible());
        node.setStatus(menu.getStatus());
        return node;
    }
}
