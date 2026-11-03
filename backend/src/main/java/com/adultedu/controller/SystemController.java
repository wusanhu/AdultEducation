package com.adultedu.controller;

import com.adultedu.aspect.OperLog;
import com.adultedu.common.PageResult;
import com.adultedu.common.R;
import com.adultedu.dto.OrgNode;
import com.adultedu.dto.SystemDtos.ConfigSaveRequest;
import com.adultedu.dto.SystemDtos.DictSaveRequest;
import com.adultedu.dto.SystemDtos.MenuSaveRequest;
import com.adultedu.dto.SystemDtos.OrgSaveRequest;
import com.adultedu.dto.SystemDtos.RoleSaveRequest;
import com.adultedu.dto.SystemDtos.StatusRequest;
import com.adultedu.dto.SystemDtos.UserSaveRequest;
import com.adultedu.entity.SysConfig;
import com.adultedu.entity.SysDict;
import com.adultedu.dto.MenuNode;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.SystemService;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {
    private final SystemService systemService;

    // ---------- 组织 ----------
    @GetMapping("/org/tree")
    public R<List<OrgNode>> orgTree() {
        return R.ok(systemService.orgTree());
    }

    @PostMapping("/org")
    @PreAuthorize("hasAuthority('system:org:edit')")
    @OperLog(module = "组织架构", content = "新增组织")
    public R<Long> createOrg(@Valid @RequestBody OrgSaveRequest req) {
        return R.ok(systemService.saveOrg(req));
    }

    @PutMapping("/org/{id}")
    @PreAuthorize("hasAuthority('system:org:edit')")
    @OperLog(module = "组织架构", content = "编辑组织")
    public R<Long> updateOrg(@PathVariable Long id, @Valid @RequestBody OrgSaveRequest req) {
        return R.ok(systemService.saveOrg(new OrgSaveRequest(id, req.parentId(), req.orgName(), req.orgType(),
                req.orgCode(), req.leaderUserId(), req.sort(), req.status())));
    }

    @DeleteMapping("/org/{id}")
    @PreAuthorize("hasAuthority('system:org:edit')")
    @OperLog(module = "组织架构", content = "删除组织")
    public R<Void> deleteOrg(@PathVariable Long id) {
        systemService.deleteOrg(id);
        return R.ok();
    }

    @PutMapping("/org/{id}/status")
    @PreAuthorize("hasAuthority('system:org:edit')")
    @OperLog(module = "组织架构", content = "启停组织")
    public R<Void> orgStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        systemService.updateOrgStatus(id, req.status());
        return R.ok();
    }

    // ---------- 账号 ----------
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('system:user')")
    public R<PageResult<Map<String, Object>>> users(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Integer userType,
            @RequestParam(required = false) Integer status) {
        return R.ok(systemService.userPage(SecurityUtils.current(), pageNum, pageSize, keyword, orgId, deptId, roleId, userType, status));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('system:user')")
    public R<Map<String, Object>> userDetail(@PathVariable Long id) {
        return R.ok(systemService.getUser(id, SecurityUtils.current()));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('system:user:add')")
    @OperLog(module = "账号管理", content = "新增账号")
    public R<Map<String, Object>> createUser(@Valid @RequestBody UserSaveRequest req) {
        return R.ok(systemService.saveUser(SecurityUtils.current(), req));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperLog(module = "账号管理", content = "编辑账号")
    public R<Map<String, Object>> updateUser(@PathVariable Long id, @Valid @RequestBody UserSaveRequest req) {
        return R.ok(systemService.saveUser(SecurityUtils.current(), new UserSaveRequest(id, req.username(), req.realName(), req.phone(),
                req.idCard(), req.orgId(), req.deptId(), req.roleId(), req.userType(), req.status(), req.remark())));
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:reset')")
    @OperLog(module = "账号管理", content = "重置密码")
    public R<Map<String, String>> resetPassword(@PathVariable Long id) {
        return R.ok(Map.of("password", systemService.resetPassword(id, SecurityUtils.current())));
    }

    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperLog(module = "账号管理", content = "启停账号")
    public R<Void> userStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        systemService.updateUserStatus(id, req.status(), SecurityUtils.current());
        return R.ok();
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperLog(module = "账号管理", content = "删除账号")
    public R<Void> deleteUser(@PathVariable Long id) {
        systemService.deleteUser(id, SecurityUtils.current());
        return R.ok();
    }

    @PostMapping("/users/import")
    @PreAuthorize("hasAuthority('system:user:import')")
    @OperLog(module = "账号管理", content = "批量导入账号")
    public R<Map<String, Object>> importUsers(@RequestPart("file") MultipartFile file) throws IOException {
        return R.ok(systemService.importUsers(file.getInputStream(), SecurityUtils.current()));
    }

    @GetMapping("/users/export")
    @PreAuthorize("hasAuthority('system:user:export')")
    public void exportUsers(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long orgId,
                            @RequestParam(required = false) Long roleId,
                            HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("账号列表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        systemService.exportUsers(SecurityUtils.current(), keyword, orgId, roleId, response.getOutputStream());
    }

    @GetMapping("/users/template")
    public void userTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("账号导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        List<List<String>> head = List.of(List.of("账号"), List.of("姓名"), List.of("手机号"), List.of("身份证号"),
                List.of("组织编码"), List.of("部门编码"), List.of("角色编码"), List.of("初始密码"));
        EasyExcel.write(response.getOutputStream()).head(head).sheet("账号导入").doWrite(List.of());
    }

    // ---------- 角色 ----------
    @GetMapping("/roles")
    public R<List<Map<String, Object>>> roles() {
        return R.ok(systemService.roleList());
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('system:role')")
    public R<Map<String, Object>> roleDetail(@PathVariable Long id) {
        return R.ok(systemService.roleDetail(id));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @OperLog(module = "角色管理", content = "新增角色")
    public R<Long> createRole(@Valid @RequestBody RoleSaveRequest req) {
        return R.ok(systemService.saveRole(req));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @OperLog(module = "角色管理", content = "编辑角色权限")
    public R<Long> updateRole(@PathVariable Long id, @Valid @RequestBody RoleSaveRequest req) {
        return R.ok(systemService.saveRole(new RoleSaveRequest(id, req.roleName(), req.roleCode(), req.roleDesc(),
                req.dataScope(), req.status(), req.menuIds(), req.orgIds())));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @OperLog(module = "角色管理", content = "删除角色")
    public R<Void> deleteRole(@PathVariable Long id) {
        systemService.deleteRole(id);
        return R.ok();
    }

    @PutMapping("/roles/{id}/status")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @OperLog(module = "角色管理", content = "启停角色")
    public R<Void> roleStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        systemService.updateRoleStatus(id, req.status());
        return R.ok();
    }

    // ---------- 菜单 ----------
    @GetMapping("/menus/tree")
    @PreAuthorize("hasAuthority('system:menu')")
    public R<List<MenuNode>> menuTree() {
        return R.ok(systemService.allMenuTree());
    }

    @PostMapping("/menus")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @OperLog(module = "菜单管理", content = "新增菜单")
    public R<Long> createMenu(@Valid @RequestBody MenuSaveRequest req) {
        return R.ok(systemService.saveMenu(req));
    }

    @PutMapping("/menus/{id}")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @OperLog(module = "菜单管理", content = "编辑菜单")
    public R<Long> updateMenu(@PathVariable Long id, @Valid @RequestBody MenuSaveRequest req) {
        return R.ok(systemService.saveMenu(new MenuSaveRequest(id, req.parentId(), req.menuName(), req.menuType(),
                req.path(), req.component(), req.icon(), req.perms(), req.sort(), req.hidden(),
                req.mobileVisible(), req.status())));
    }

    @DeleteMapping("/menus/{id}")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @OperLog(module = "菜单管理", content = "删除菜单")
    public R<Void> deleteMenu(@PathVariable Long id) {
        systemService.deleteMenu(id);
        return R.ok();
    }

    @PutMapping("/menus/{id}/status")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @OperLog(module = "菜单管理", content = "启停菜单")
    public R<Void> menuStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        systemService.updateMenuStatus(id, req.status());
        return R.ok();
    }

    // ---------- 参数 / 字典 ----------
    @GetMapping("/configs")
    public R<List<SysConfig>> configs() {
        return R.ok(systemService.configList());
    }

    @PutMapping("/configs")
    @PreAuthorize("hasAuthority('system:config:edit')")
    @OperLog(module = "系统参数", content = "保存参数")
    public R<Void> saveConfigs(@Valid @RequestBody List<ConfigSaveRequest> list) {
        systemService.saveConfigs(list);
        return R.ok();
    }

    @GetMapping("/dicts")
    public R<List<SysDict>> dicts(@RequestParam(required = false) String dictType) {
        return R.ok(systemService.dictList(dictType));
    }

    @PostMapping("/dicts")
    @PreAuthorize("hasAuthority('system:dict:edit')")
    @OperLog(module = "数据字典", content = "新增字典")
    public R<Long> createDict(@Valid @RequestBody DictSaveRequest req) {
        return R.ok(systemService.saveDict(req));
    }

    @PutMapping("/dicts/{id}")
    @PreAuthorize("hasAuthority('system:dict:edit')")
    @OperLog(module = "数据字典", content = "编辑字典")
    public R<Long> updateDict(@PathVariable Long id, @Valid @RequestBody DictSaveRequest req) {
        return R.ok(systemService.saveDict(new DictSaveRequest(id, req.dictName(), req.dictType(), req.dictValue(),
                req.dictLabel(), req.sort(), req.status())));
    }

    @DeleteMapping("/dicts/{id}")
    @PreAuthorize("hasAuthority('system:dict:edit')")
    @OperLog(module = "数据字典", content = "删除字典")
    public R<Void> deleteDict(@PathVariable Long id) {
        systemService.deleteDict(id);
        return R.ok();
    }

    // ---------- 日志 ----------
    @GetMapping("/oper-logs")
    @PreAuthorize("hasAuthority('system:log')")
    public R<PageResult<Map<String, Object>>> operLogs(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return R.ok(systemService.operLogPage(pageNum, pageSize, keyword, module, start, end));
    }

    @GetMapping("/login-logs")
    @PreAuthorize("hasAuthority('system:loginlog')")
    public R<PageResult<Map<String, Object>>> loginLogs(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return R.ok(systemService.loginLogPage(SecurityUtils.current(), pageNum, pageSize, keyword, start, end));
    }
}
