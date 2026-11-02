package com.adultedu.service;

import com.adultedu.common.BusinessException;
import com.adultedu.dto.AuthDtos.ProfileUpdateRequest;
import com.adultedu.entity.SysOrg;
import com.adultedu.entity.SysUser;
import com.adultedu.mapper.SysOrgMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;

    public Map<String, Object> profile(LoginUser current) {
        SysUser user = userMapper.selectById(current.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("realName", user.getRealName());
        m.put("phone", user.getPhone());
        m.put("avatar", user.getAvatar());
        m.put("idCard", maskIdCard(user.getIdCard()));
        m.put("orgId", user.getOrgId());
        m.put("orgName", orgName(user.getOrgId()));
        m.put("roleCode", current.getRoleCode());
        m.put("roleName", current.getRoleName());
        m.put("userType", user.getUserType());
        m.put("lastLoginTime", user.getLastLoginTime());
        m.put("mustChangePwd", user.getMustChangePwd() != null && user.getMustChangePwd() == 1);
        return m;
    }

    public Map<String, Object> update(LoginUser current, ProfileUpdateRequest req) {
        SysUser user = userMapper.selectById(current.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String phone = req.phone() == null ? "" : req.phone().trim();
        if (!phone.isEmpty()) {
            SysUser same = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, phone).ne(SysUser::getId, user.getId()));
            if (same != null) {
                throw new BusinessException("该手机号已被其他账号使用");
            }
        }
        String avatar = req.avatar() == null ? "" : req.avatar().trim();
        if (!avatar.isEmpty() && !avatar.startsWith("/uploads/") && !avatar.startsWith("https://")) {
            throw new BusinessException("头像地址不合法");
        }
        user.setRealName(req.realName().trim());
        user.setPhone(phone);
        user.setAvatar(avatar.isEmpty() ? null : avatar);
        userMapper.updateById(user);
        return profile(current);
    }

    private String orgName(Long orgId) {
        if (orgId == null) {
            return null;
        }
        SysOrg org = orgMapper.selectById(orgId);
        return org == null ? null : org.getOrgName();
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard == null ? null : "****";
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }
}
