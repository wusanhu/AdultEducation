package com.adultedu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "请输入账号") String username,
            @NotBlank(message = "请输入密码") String password
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "请输入原密码") String oldPassword,
            @NotBlank(message = "请输入新密码")
            @Size(min = 8, max = 32, message = "密码长度需为 8-32 位")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).+$", message = "密码须同时包含字母与数字")
            String newPassword
    ) {
    }

    public record SmsSendRequest(@NotBlank(message = "请输入手机号") String phone) {
    }

    public record ProfileUpdateRequest(
            @NotBlank(message = "请输入姓名") @Size(max = 30, message = "姓名不能超过 30 个字符") String realName,
            @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            String avatar
    ) {
    }

    public record SmsLoginRequest(
            @NotBlank(message = "请输入手机号") String phone,
            @NotBlank(message = "请输入验证码") String code
    ) {
    }

    public record LoginResponse(
            String token,
            String tokenType,
            long expiresIn,
            Map<String, Object> user,
            String roleCode,
            String roleName,
            List<String> perms,
            List<MenuNode> menus,
            boolean mustChangePwd
    ) {
    }
}
