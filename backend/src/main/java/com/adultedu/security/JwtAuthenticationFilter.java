package com.adultedu.security;

import com.adultedu.common.R;
import com.adultedu.entity.SysRole;
import com.adultedu.entity.SysUser;
import com.adultedu.mapper.SysRoleMapper;
import com.adultedu.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String header = request.getHeader("Authorization");
        if (header == null && path.startsWith("/uploads/")) {
            // 浏览器 img/a 标签无法携带 Authorization 头，私有附件允许通过 token 查询参数访问
            String queryToken = request.getParameter("token");
            if (queryToken != null && !queryToken.isBlank()) {
                header = "Bearer " + queryToken.trim();
            }
        }
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            try {
                Long userId = jwtUtil.parseUserId(token);
                String cached = redisTemplate.opsForValue().get("token:" + userId);
                if (cached != null && cached.equals(token)) {
                    SysUser user = userMapper.selectById(userId);
                    SysRole role = user == null ? null : roleMapper.selectById(user.getRoleId());
                    if (user != null && role != null && user.getStatus() == 1 && role.getStatus() == 1) {
                        boolean superAdmin = "super_admin".equals(role.getRoleCode());
                        List<String> perms = permissionService.perms(role.getId(), superAdmin);
                        LoginUser loginUser = LoginUser.builder()
                                .id(user.getId()).username(user.getUsername()).realName(user.getRealName())
                                .roleCode(role.getRoleCode()).roleName(role.getRoleName())
                                .userType(user.getUserType()).roleId(role.getId())
                                .orgId(user.getOrgId()).deptId(user.getDeptId())
                                .dataScope(role.getDataScope()).superAdmin(superAdmin)
                                .mustChangePwd(user.getMustChangePwd() != null && user.getMustChangePwd() == 1)
                                .perms(perms).build();
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        perms.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode().toUpperCase()));
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        if (loginUser.isMustChangePwd() && !passwordChangeAllowed(path)) {
                            write(response, 403, 4030, "首次登录须修改初始密码");
                            return;
                        }
                    }
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // 无效 token 交由后续鉴权返回 401
            }
        }
        chain.doFilter(request, response);
    }

    private boolean passwordChangeAllowed(String path) {
        return path.startsWith("/api/auth/change-password")
                || path.startsWith("/api/auth/me")
                || path.startsWith("/api/auth/logout")
                || path.startsWith("/uploads/");
    }

    private void write(HttpServletResponse response, int httpStatus, int code, String msg) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(code, msg)));
    }
}
