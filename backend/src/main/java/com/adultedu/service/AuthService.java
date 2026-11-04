package com.adultedu.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.adultedu.common.BusinessException;
import com.adultedu.dto.AuthDtos.ChangePasswordRequest;
import com.adultedu.dto.AuthDtos.LoginRequest;
import com.adultedu.dto.AuthDtos.LoginResponse;
import com.adultedu.dto.AuthDtos.SmsLoginRequest;
import com.adultedu.entity.SysLoginLog;
import com.adultedu.entity.SysOrg;
import com.adultedu.entity.SysRole;
import com.adultedu.entity.SysUser;
import com.adultedu.mapper.SysLoginLogMapper;
import com.adultedu.mapper.SysOrgMapper;
import com.adultedu.mapper.SysRoleMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.security.ClientUtils;
import com.adultedu.security.JwtUtil;
import com.adultedu.security.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysOrgMapper orgMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;
    private final StringRedisTemplate redisTemplate;
    private final ClientUtils clientUtils;

    @Value("${app.security.max-login-fail:5}")
    private int maxLoginFail;
    @Value("${app.security.lock-minutes:15}")
    private int lockMinutes;
    @Value("${app.sms.mock:true}")
    private boolean smsMock;
    @Value("${app.sms.code-expire-seconds:300}")
    private int smsExpire;
    @Value("${app.sms.resend-seconds:60}")
    private int smsResend;
    @Value("${app.sms.daily-limit:10}")
    private int smsDailyLimit;

    public LoginResponse login(LoginRequest req, HttpServletRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.username()));
        String ip = clientUtils.ip(request);
        String device = clientUtils.device(request);
        if (user == null) {
            writeLoginLog(null, req.username(), ip, device, 0, "账号不存在");
            throw new BusinessException(400, "账号或密码错误");
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            writeLoginLog(user.getId(), req.username(), ip, device, 0, "账号锁定中");
            throw new BusinessException(400, "账号已锁定，请于 " + user.getLockUntil() + " 后重试");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            writeLoginLog(user.getId(), req.username(), ip, device, 0, "账号已停用");
            throw new BusinessException(400, "账号已停用，请联系管理员");
        }
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            int fails = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            user.setLoginFailCount(fails);
            String msg = "密码错误";
            if (fails >= maxLoginFail) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                user.setLoginFailCount(0);
                msg = "连续输错密码，账号锁定 " + lockMinutes + " 分钟";
            }
            userMapper.updateById(user);
            writeLoginLog(user.getId(), req.username(), ip, device, 0, msg);
            throw new BusinessException(400, fails >= maxLoginFail ? msg : "账号或密码错误，还可尝试 " + (maxLoginFail - fails) + " 次");
        }
        return completeLogin(user, request);
    }

    public Map<String, Object> sendLoginSms(String phone) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));
        if (user == null) {
            throw new BusinessException(400, "手机号未绑定账号");
        }
        String cooldownKey = "sms:login:cooldown:" + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new BusinessException(400, "验证码发送过于频繁，请稍后再试");
        }
        String dailyKey = "sms:login:daily:" + phone + ":" + java.time.LocalDate.now();
        String countStr = redisTemplate.opsForValue().get(dailyKey);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        if (count >= smsDailyLimit) {
            throw new BusinessException(400, "今日验证码发送次数已达上限");
        }
        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().set("sms:login:code:" + phone, code, Duration.ofSeconds(smsExpire));
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(smsResend));
        redisTemplate.opsForValue().set(dailyKey, String.valueOf(count + 1), Duration.ofDays(1));
        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        result.put("expireSeconds", smsExpire);
        if (smsMock) {
            result.put("mockCode", code);
            result.put("tip", "本地演示模式：验证码直接返回");
        }
        return result;
    }

    @Transactional
    public LoginResponse smsLogin(SmsLoginRequest req, HttpServletRequest request) {
        String key = "sms:login:code:" + req.phone();
        String code = redisTemplate.opsForValue().get(key);
        if (code == null || !code.equals(req.code())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        redisTemplate.delete(key);
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, req.phone()));
        if (user == null) {
            throw new BusinessException(400, "手机号未绑定账号");
        }
        return completeLogin(user, request);
    }

    private LoginResponse completeLogin(SysUser user, HttpServletRequest request) {
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException(400, "账号已停用");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role == null || role.getStatus() == null || role.getStatus() == 0) {
            throw new BusinessException(400, "账号角色已停用");
        }
        String ip = clientUtils.ip(request);
        String device = clientUtils.device(request);
        user.setLoginFailCount(0);
        user.setLockUntil(null);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userMapper.updateById(user);
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        redisTemplate.opsForValue().set("token:" + user.getId(), token, jwtUtil.getExpireSeconds(), TimeUnit.SECONDS);
        writeLoginLog(user.getId(), user.getUsername(), ip, device, 1, "登录成功");

        boolean superAdmin = "super_admin".equals(role.getRoleCode());
        List<String> perms = permissionService.perms(role.getId(), superAdmin);
        return new LoginResponse(token, "Bearer", jwtUtil.getExpireSeconds(), userInfo(user, role),
                role.getRoleCode(), role.getRoleName(), perms,
                permissionService.menuTree(role.getId(), superAdmin), user.getMustChangePwd() != null && user.getMustChangePwd() == 1);
    }

    public Map<String, Object> userInfo(SysUser user, SysRole role) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("realName", user.getRealName());
        map.put("phone", user.getPhone());
        map.put("idCard", StrUtil.hide(user.getIdCard(), 6, user.getIdCard() == null ? 0 : Math.max(0, user.getIdCard().length() - 4)));
        map.put("avatar", user.getAvatar());
        map.put("orgId", user.getOrgId());
        map.put("deptId", user.getDeptId());
        map.put("roleId", user.getRoleId());
        map.put("roleCode", role == null ? null : role.getRoleCode());
        map.put("roleName", role == null ? null : role.getRoleName());
        map.put("userType", user.getUserType());
        map.put("status", user.getStatus());
        map.put("mustChangePwd", user.getMustChangePwd());
        map.put("lastLoginTime", user.getLastLoginTime());
        if (user.getOrgId() != null) {
            SysOrg org = orgMapper.selectById(user.getOrgId());
            map.put("orgName", org == null ? null : org.getOrgName());
        }
        if (user.getDeptId() != null) {
            SysOrg dept = orgMapper.selectById(user.getDeptId());
            map.put("deptName", dept == null ? null : dept.getOrgName());
        }
        return map;
    }

    public LoginResponse currentInfo(SysUser user) {
        SysRole role = roleMapper.selectById(user.getRoleId());
        boolean superAdmin = role != null && "super_admin".equals(role.getRoleCode());
        List<String> perms = role == null ? List.of() : permissionService.perms(role.getId(), superAdmin);
        return new LoginResponse(null, "Bearer", jwtUtil.getExpireSeconds(), userInfo(user, role),
                role == null ? null : role.getRoleCode(), role == null ? null : role.getRoleName(), perms,
                role == null ? List.of() : permissionService.menuTree(role.getId(), superAdmin),
                user.getMustChangePwd() != null && user.getMustChangePwd() == 1);
    }

    @Transactional
    public void changePassword(SysUser user, ChangePasswordRequest req) {
        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setMustChangePwd(0);
        userMapper.updateById(user);
        redisTemplate.delete("token:" + user.getId());
    }

    public void logout(Long userId) {
        redisTemplate.delete("token:" + userId);
    }

    private void writeLoginLog(Long userId, String username, String ip, String device, int status, String msg) {
        SysLoginLog log = new SysLoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIp(ip);
        log.setDevice(device);
        log.setStatus(status);
        log.setMsg(msg);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }
}
