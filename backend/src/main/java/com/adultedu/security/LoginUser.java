package com.adultedu.security;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginUser {
    private Long id;
    private String username;
    private String realName;
    private String roleCode;
    private String roleName;
    private Integer userType;
    private Long roleId;
    private Long orgId;
    private Long deptId;
    private Integer dataScope;
    private boolean superAdmin;
    private boolean mustChangePwd;
    private List<String> perms;
}
