package com.adultedu.controller;

import com.adultedu.aspect.OperLog;
import com.adultedu.common.R;
import com.adultedu.dto.AuthDtos.ProfileUpdateRequest;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping
    public R<Map<String, Object>> profile() {
        return R.ok(profileService.profile(SecurityUtils.current()));
    }

    @PutMapping
    @OperLog(module = "个人中心", content = "修改个人资料")
    public R<Map<String, Object>> update(@Valid @RequestBody ProfileUpdateRequest req) {
        return R.ok(profileService.update(SecurityUtils.current(), req));
    }
}
