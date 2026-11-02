package com.adultedu.controller;

import com.adultedu.common.R;
import com.adultedu.dto.AuthDtos.ChangePasswordRequest;
import com.adultedu.dto.AuthDtos.LoginRequest;
import com.adultedu.dto.AuthDtos.LoginResponse;
import com.adultedu.dto.AuthDtos.SmsLoginRequest;
import com.adultedu.dto.AuthDtos.SmsSendRequest;
import com.adultedu.entity.SysUser;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.security.LoginUser;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final SysUserMapper userMapper;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        return R.ok(authService.login(req, request));
    }

    @PostMapping("/sms/send")
    public R<Map<String, Object>> sendSms(@Valid @RequestBody SmsSendRequest req) {
        return R.ok(authService.sendLoginSms(req.phone()));
    }

    @PostMapping("/sms/login")
    public R<LoginResponse> smsLogin(@Valid @RequestBody SmsLoginRequest req, HttpServletRequest request) {
        return R.ok(authService.smsLogin(req, request));
    }

    @GetMapping("/me")
    public R<LoginResponse> me() {
        LoginUser current = SecurityUtils.current();
        SysUser user = userMapper.selectById(current.getId());
        return R.ok(authService.currentInfo(user));
    }

    @PostMapping("/change-password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        LoginUser current = SecurityUtils.current();
        SysUser user = userMapper.selectById(current.getId());
        authService.changePassword(user, req);
        return R.ok();
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout(SecurityUtils.userId());
        return R.ok();
    }
}
