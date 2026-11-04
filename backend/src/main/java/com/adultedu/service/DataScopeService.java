package com.adultedu.service;

import com.adultedu.entity.SysOrg;
import com.adultedu.entity.SysRole;
import com.adultedu.entity.SysRoleOrg;
import com.adultedu.entity.SysUser;
import com.adultedu.mapper.SysOrgMapper;
import com.adultedu.mapper.SysRoleMapper;
import com.adultedu.mapper.SysRoleOrgMapper;
import com.adultedu.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DataScopeService {
    private final SysOrgMapper orgMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleOrgMapper roleOrgMapper;

    /**
     * 返回可访问的组织 ID 集合。null 表示不受限（全部数据）。
     */
    public List<Long> accessibleOrgIds(LoginUser user) {
        if (user.isSuperAdmin() || (user.getDataScope() != null && user.getDataScope() == 1)) {
            return null;
        }
        int scope = user.getDataScope() == null ? 4 : user.getDataScope();
        if (scope == 2) {
            return descendants(user.getOrgId());
        }
        if (scope == 3) {
            return user.getDeptId() == null ? List.of() : List.of(user.getDeptId());
        }
        if (scope == 5) {
            List<SysRoleOrg> links = roleOrgMapper.selectList(new LambdaQueryWrapper<SysRoleOrg>().eq(SysRoleOrg::getRoleId, user.getRoleId()));
            if (links.isEmpty()) {
                return List.of();
            }
            List<Long> result = new ArrayList<>();
            for (SysRoleOrg link : links) {
                result.addAll(descendants(link.getOrgId()));
            }
            return result.stream().distinct().toList();
        }
        return List.of();
    }

    public List<Long> descendants(Long rootId) {
        if (rootId == null) {
            return List.of();
        }
        List<SysOrg> all = orgMapper.selectList(new LambdaQueryWrapper<SysOrg>().eq(SysOrg::getStatus, 1));
        Set<Long> result = new HashSet<>();
        result.add(rootId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (SysOrg org : all) {
                if (org.getParentId() != null && result.contains(org.getParentId()) && result.add(org.getId())) {
                    changed = true;
                }
            }
        }
        return new ArrayList<>(result);
    }

    public boolean canAccessOrg(LoginUser user, Long orgId) {
        List<Long> ids = accessibleOrgIds(user);
        return ids == null || (orgId != null && ids.contains(orgId));
    }

    public boolean sameOrgOrChild(LoginUser user, Long orgId) {
        List<Long> ids = accessibleOrgIds(user);
        return ids == null || (orgId != null && ids.contains(orgId));
    }

    public boolean isLeader(LoginUser user) {
        return "leader".equals(user.getRoleCode());
    }

    public boolean isTeacher(LoginUser user) {
        return "teacher".equals(user.getRoleCode());
    }

    public boolean isStudent(LoginUser user) {
        return "student".equals(user.getRoleCode());
    }
}
