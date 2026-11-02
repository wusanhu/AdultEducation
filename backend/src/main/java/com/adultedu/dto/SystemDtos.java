package com.adultedu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class SystemDtos {
    private SystemDtos() {
    }

    public record OrgSaveRequest(
            Long id,
            Long parentId,
            @NotBlank(message = "请填写组织/部门名称") String orgName,
            @NotNull(message = "请选择类型") Integer orgType,
            @NotBlank(message = "请填写组织编码") String orgCode,
            Long leaderUserId,
            Integer sort,
            Integer status
    ) {
    }

    public record UserSaveRequest(
            Long id,
            @NotBlank(message = "请填写账号") @Size(max = 50, message = "账号过长") String username,
            @NotBlank(message = "请填写姓名") String realName,
            String phone,
            String idCard,
            Long orgId,
            Long deptId,
            @NotNull(message = "请选择角色") Long roleId,
            Integer userType,
            Integer status,
            String remark
    ) {
    }

    public record RoleSaveRequest(
            Long id,
            @NotBlank(message = "请填写角色名称") String roleName,
            @NotBlank(message = "请填写角色编码") String roleCode,
            String roleDesc,
            @NotNull(message = "请选择数据范围") Integer dataScope,
            Integer status,
            List<Long> menuIds,
            List<Long> orgIds
    ) {
    }

    public record MenuSaveRequest(
            Long id,
            Long parentId,
            @NotBlank(message = "请填写菜单名称") String menuName,
            @NotNull(message = "请选择菜单类型") Integer menuType,
            String path,
            String component,
            String icon,
            String perms,
            Integer sort,
            Integer hidden,
            Integer mobileVisible,
            Integer status
    ) {
    }

    public record ConfigSaveRequest(
            @NotBlank(message = "参数键不能为空") String configKey,
            String configName,
            String configValue,
            String configType,
            String remark
    ) {
    }

    public record DictSaveRequest(
            Long id,
            @NotBlank(message = "请填写字典名称") String dictName,
            @NotBlank(message = "请填写字典类型") String dictType,
            @NotBlank(message = "请填写字典值") String dictValue,
            @NotBlank(message = "请填写字典标签") String dictLabel,
            Integer sort,
            Integer status
    ) {
    }

    public record StatusRequest(@NotNull(message = "请选择状态") Integer status) {
    }
}
