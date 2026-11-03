package com.adultedu.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.adultedu.common.BusinessException;
import com.adultedu.common.PageResult;
import com.adultedu.dto.OrgNode;
import com.adultedu.dto.SystemDtos.ConfigSaveRequest;
import com.adultedu.dto.SystemDtos.DictSaveRequest;
import com.adultedu.dto.SystemDtos.MenuSaveRequest;
import com.adultedu.dto.SystemDtos.OrgSaveRequest;
import com.adultedu.dto.SystemDtos.RoleSaveRequest;
import com.adultedu.dto.SystemDtos.UserSaveRequest;
import com.adultedu.dto.UserExportRow;
import com.adultedu.dto.UserImportRow;
import com.adultedu.entity.CreditRecord;
import com.adultedu.entity.GraduateApply;
import com.adultedu.entity.ProjectMember;
import com.adultedu.entity.SysConfig;
import com.adultedu.entity.SysDict;
import com.adultedu.entity.SysLoginLog;
import com.adultedu.entity.SysMenu;
import com.adultedu.entity.SysOperLog;
import com.adultedu.entity.SysOrg;
import com.adultedu.entity.SysRole;
import com.adultedu.entity.SysRoleMenu;
import com.adultedu.entity.SysRoleOrg;
import com.adultedu.entity.SysUser;
import com.adultedu.entity.StudyRecord;
import com.adultedu.mapper.CreditRecordMapper;
import com.adultedu.mapper.GraduateApplyMapper;
import com.adultedu.mapper.ProjectMemberMapper;
import com.adultedu.mapper.StudyRecordMapper;
import com.adultedu.mapper.SysConfigMapper;
import com.adultedu.mapper.SysDictMapper;
import com.adultedu.mapper.SysLoginLogMapper;
import com.adultedu.mapper.SysMenuMapper;
import com.adultedu.mapper.SysOperLogMapper;
import com.adultedu.mapper.SysOrgMapper;
import com.adultedu.mapper.SysRoleMapper;
import com.adultedu.mapper.SysRoleMenuMapper;
import com.adultedu.mapper.SysRoleOrgMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.security.LoginUser;
import com.adultedu.security.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemService {
    private final SysOrgMapper orgMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleOrgMapper roleOrgMapper;
    private final SysConfigMapper configMapper;
    private final SysDictMapper dictMapper;
    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final StudyRecordMapper studyRecordMapper;
    private final CreditRecordMapper creditRecordMapper;
    private final GraduateApplyMapper graduateApplyMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final PasswordEncoder passwordEncoder;
    private final DataScopeService dataScopeService;
    private final PermissionService permissionService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== 组织架构 ====================
    public List<OrgNode> orgTree() {
        List<SysOrg> all = orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .orderByAsc(SysOrg::getSort).orderByAsc(SysOrg::getId));
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().select(SysUser::getId, SysUser::getRealName));
        Map<Long, String> userNames = users.stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        Map<Long, Long> counts = userMapper.selectList(new LambdaQueryWrapper<SysUser>().select(SysUser::getId, SysUser::getOrgId, SysUser::getDeptId))
                .stream().filter(u -> u.getDeptId() != null)
                .collect(Collectors.groupingBy(SysUser::getDeptId, Collectors.counting()));
        Map<Long, OrgNode> nodes = new LinkedHashMap<>();
        for (SysOrg org : all) {
            OrgNode node = new OrgNode();
            node.setId(org.getId());
            node.setParentId(org.getParentId());
            node.setOrgName(org.getOrgName());
            node.setOrgType(org.getOrgType());
            node.setOrgCode(org.getOrgCode());
            node.setLeaderUserId(org.getLeaderUserId());
            node.setLeaderName(userNames.get(org.getLeaderUserId()));
            node.setSort(org.getSort());
            node.setStatus(org.getStatus());
            node.setUserCount(counts.getOrDefault(org.getId(), 0L));
            nodes.put(org.getId(), node);
        }
        List<OrgNode> roots = new ArrayList<>();
        for (SysOrg org : all) {
            OrgNode node = nodes.get(org.getId());
            if (org.getParentId() == null || org.getParentId() == 0 || !nodes.containsKey(org.getParentId())) {
                roots.add(node);
            } else {
                nodes.get(org.getParentId()).getChildren().add(node);
            }
        }
        return roots;
    }

    @Transactional
    public Long saveOrg(OrgSaveRequest req) {
        SysOrg parent = null;
        if (req.parentId() != null && req.parentId() != 0) {
            parent = orgMapper.selectById(req.parentId());
            if (parent == null) {
                throw new BusinessException("上级组织不存在");
            }
        }
        SysOrg sameCode = orgMapper.selectOne(new LambdaQueryWrapper<SysOrg>().eq(SysOrg::getOrgCode, req.orgCode()));
        if (sameCode != null && !sameCode.getId().equals(req.id())) {
            throw new BusinessException("组织编码已存在");
        }
        SysOrg org = req.id() == null ? new SysOrg() : orgMapper.selectById(req.id());
        if (org == null) {
            throw new BusinessException("组织不存在");
        }
        if (req.id() != null && req.parentId() != null) {
            if (req.parentId().equals(req.id())) {
                throw new BusinessException("上级组织不能是自身");
            }
            List<Long> descendants = dataScopeService.descendants(req.id());
            if (descendants.contains(req.parentId())) {
                throw new BusinessException("不能移动到自己的下级组织");
            }
        }
        org.setParentId(req.parentId() == null ? 0 : req.parentId());
        org.setOrgName(req.orgName());
        org.setOrgType(req.orgType());
        org.setOrgCode(req.orgCode());
        org.setLeaderUserId(req.leaderUserId());
        org.setSort(req.sort() == null ? 0 : req.sort());
        org.setStatus(req.status() == null ? 1 : req.status());
        if (req.id() == null) {
            orgMapper.insert(org);
        } else {
            orgMapper.updateById(org);
        }
        return org.getId();
    }

    @Transactional
    public void deleteOrg(Long id) {
        Long children = orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>().eq(SysOrg::getParentId, id));
        if (children > 0) {
            throw new BusinessException("该节点下存在下级组织/部门，禁止删除");
        }
        Long users = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .and(w -> w.eq(SysUser::getOrgId, id).or().eq(SysUser::getDeptId, id)));
        if (users > 0) {
            throw new BusinessException("该节点下存在账号，禁止删除，请先迁移账号");
        }
        orgMapper.deleteById(id);
    }

    @Transactional
    public void updateOrgStatus(Long id, Integer status) {
        SysOrg org = orgMapper.selectById(id);
        if (org == null) {
            throw new BusinessException("组织不存在");
        }
        org.setStatus(status);
        orgMapper.updateById(org);
    }

    // ==================== 用户账号 ====================
    public PageResult<Map<String, Object>> userPage(LoginUser current, long pageNum, long pageSize, String keyword,
                                                    Long orgId, Long deptId, Long roleId, Integer userType, Integer status) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (roleId != null) {
            qw.eq(SysUser::getRoleId, roleId);
        }
        if (userType != null) {
            qw.eq(SysUser::getUserType, userType);
        }
        if (status != null) {
            qw.eq(SysUser::getStatus, status);
        }
        List<Long> scope = dataScopeService.accessibleOrgIds(current);
        if (scope != null) {
            if (scope.isEmpty()) {
                return PageResult.of(List.of(), 0, pageNum, pageSize);
            }
            qw.and(w -> w.in(SysUser::getOrgId, scope).or().in(SysUser::getDeptId, scope));
        }
        if (orgId != null) {
            List<Long> ids = dataScopeService.descendants(orgId);
            qw.and(w -> w.in(SysUser::getOrgId, ids).or().in(SysUser::getDeptId, ids));
        }
        if (deptId != null) {
            qw.eq(SysUser::getDeptId, deptId);
        }
        qw.orderByDesc(SysUser::getId);
        Page<SysUser> result = userMapper.selectPage(page, qw);
        List<Map<String, Object>> records = result.getRecords().stream().map(this::userMap).toList();
        return PageResult.of(records, result.getTotal(), pageNum, pageSize);
    }

    public Map<String, Object> getUser(Long id, LoginUser current) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        assertUserAccessible(current, user);
        return userMap(user);
    }

    private void assertUserAccessible(LoginUser current, SysUser user) {
        if (current == null || current.isSuperAdmin()) {
            return;
        }
        Long orgId = user.getOrgId() == null ? user.getDeptId() : user.getOrgId();
        if (!dataScopeService.canAccessOrg(current, orgId)) {
            throw new BusinessException(403, "无权操作该账号");
        }
    }

    public Map<String, Object> userMap(SysUser user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("realName", user.getRealName());
        map.put("phone", user.getPhone());
        map.put("idCard", user.getIdCard());
        map.put("orgId", user.getOrgId());
        map.put("deptId", user.getDeptId());
        map.put("roleId", user.getRoleId());
        map.put("userType", user.getUserType());
        map.put("status", user.getStatus());
        map.put("mustChangePwd", user.getMustChangePwd());
        map.put("lastLoginTime", user.getLastLoginTime());
        map.put("lastLoginIp", user.getLastLoginIp());
        map.put("lockUntil", user.getLockUntil());
        map.put("remark", user.getRemark());
        map.put("createTime", user.getCreateTime());
        if (user.getOrgId() != null) {
            SysOrg org = orgMapper.selectById(user.getOrgId());
            map.put("orgName", org == null ? null : org.getOrgName());
        }
        if (user.getDeptId() != null) {
            SysOrg dept = orgMapper.selectById(user.getDeptId());
            map.put("deptName", dept == null ? null : dept.getOrgName());
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        map.put("roleName", role == null ? null : role.getRoleName());
        map.put("roleCode", role == null ? null : role.getRoleCode());
        return map;
    }

    @Transactional
    public Map<String, Object> saveUser(LoginUser current, UserSaveRequest req) {
        SysRole role = roleMapper.selectById(req.roleId());
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (!current.isSuperAdmin() && "super_admin".equals(role.getRoleCode())) {
            throw new BusinessException(403, "无权分配超级管理员角色");
        }
        SysUser same = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.username()));
        if (same != null && !same.getId().equals(req.id())) {
            throw new BusinessException("账号已存在");
        }
        if (StrUtil.isNotBlank(req.phone())) {
            SysUser phoneUser = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, req.phone()));
            if (phoneUser != null && !phoneUser.getId().equals(req.id())) {
                throw new BusinessException("手机号已被其他账号绑定");
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        if (req.id() == null) {
            if (!current.isSuperAdmin()
                    && !dataScopeService.canAccessOrg(current, req.orgId() == null ? req.deptId() : req.orgId())) {
                throw new BusinessException(403, "无权在目标组织下创建账号");
            }
            SysUser user = new SysUser();
            user.setUsername(req.username());
            user.setRealName(req.realName());
            user.setPhone(req.phone());
            user.setIdCard(req.idCard());
            user.setOrgId(req.orgId());
            user.setDeptId(req.deptId());
            user.setRoleId(req.roleId());
            user.setUserType(req.userType() == null ? roleToUserType(role.getRoleCode()) : req.userType());
            user.setStatus(req.status() == null ? 1 : req.status());
            user.setRemark(req.remark());
            String password = RandomUtil.randomString("ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz", 4) + RandomUtil.randomNumbers(4);
            user.setPassword(passwordEncoder.encode(password));
            user.setMustChangePwd(1);
            user.setLoginFailCount(0);
            userMapper.insert(user);
            notificationService.send(user.getId(), "账号开通通知",
                    "您的继续教育平台账号已开通，账号：" + user.getUsername() + "，初始密码：" + password + "，首次登录请修改密码。",
                    "ACCOUNT", user.getId());
            result.put("id", user.getId());
            result.put("username", user.getUsername());
            result.put("initialPassword", password);
            return result;
        }
        SysUser user = userMapper.selectById(req.id());
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        assertUserAccessible(current, user);
        if (!current.isSuperAdmin()
                && !dataScopeService.canAccessOrg(current, req.orgId() == null ? req.deptId() : req.orgId())) {
            throw new BusinessException(403, "无权将账号调整到目标组织");
        }
        user.setUsername(req.username());
        user.setRealName(req.realName());
        user.setPhone(req.phone());
        user.setIdCard(req.idCard());
        user.setOrgId(req.orgId());
        user.setDeptId(req.deptId());
        user.setRoleId(req.roleId());
        user.setUserType(req.userType() == null ? roleToUserType(role.getRoleCode()) : req.userType());
        user.setStatus(req.status() == null ? user.getStatus() : req.status());
        user.setRemark(req.remark());
        userMapper.updateById(user);
        result.put("id", user.getId());
        return result;
    }

    @Transactional
    public String resetPassword(Long id, LoginUser current) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        assertUserAccessible(current, user);
        String password = RandomUtil.randomString("ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz", 4) + RandomUtil.randomNumbers(4);
        user.setPassword(passwordEncoder.encode(password));
        user.setMustChangePwd(1);
        user.setLoginFailCount(0);
        user.setLockUntil(null);
        userMapper.updateById(user);
        notificationService.send(id, "密码重置通知", "管理员已重置您的登录密码，请使用新密码登录并及时修改。", "ACCOUNT", id);
        return password;
    }

    @Transactional
    public void updateUserStatus(Long id, Integer status, LoginUser current) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        assertUserAccessible(current, user);
        if (id == 1L && status == 0) {
            throw new BusinessException("内置超级管理员不可停用");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Transactional
    public void deleteUser(Long id, LoginUser current) {
        if (id == 1L) {
            throw new BusinessException("内置超级管理员不可删除");
        }
        if (id.equals(current.getId())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        SysUser target = userMapper.selectById(id);
        if (target == null) {
            throw new BusinessException("账号不存在");
        }
        assertUserAccessible(current, target);
        long studies = studyRecordMapper.selectCount(new LambdaQueryWrapper<StudyRecord>().eq(StudyRecord::getUserId, id));
        long credits = creditRecordMapper.selectCount(new LambdaQueryWrapper<CreditRecord>().eq(CreditRecord::getUserId, id));
        long applies = graduateApplyMapper.selectCount(new LambdaQueryWrapper<GraduateApply>().eq(GraduateApply::getUserId, id));
        if (studies + credits + applies > 0) {
            throw new BusinessException("该账号已产生学习、学分或审批数据，禁止删除，请改为停用");
        }
        projectMemberMapper.delete(new LambdaQueryWrapper<ProjectMember>().eq(ProjectMember::getUserId, id));
        userMapper.deleteById(id);
    }

    public Map<String, Object> importUsers(InputStream inputStream, LoginUser current) {
        List<UserImportRow> rows = new ArrayList<>();
        try {
            rows = com.alibaba.excel.EasyExcel.read(inputStream).head(UserImportRow.class).sheet().doReadSync();
        } catch (Exception e) {
            throw new BusinessException("Excel 解析失败，请使用系统模板：" + e.getMessage());
        }
        Map<String, SysOrg> orgByCode = orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()).stream()
                .collect(Collectors.toMap(SysOrg::getOrgCode, Function.identity(), (a, b) -> a));
        Map<String, SysRole> roleByCode = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()).stream()
                .collect(Collectors.toMap(SysRole::getRoleCode, Function.identity(), (a, b) -> a));
        List<Map<String, Object>> errors = new ArrayList<>();
        List<Map<String, Object>> accounts = new ArrayList<>();
        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            UserImportRow row = rows.get(i);
            int line = i + 2;
            try {
                if (StrUtil.isBlank(row.getUsername()) || StrUtil.isBlank(row.getRealName())) {
                    throw new BusinessException("账号与姓名不能为空");
                }
                if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, row.getUsername())) > 0) {
                    throw new BusinessException("账号已存在");
                }
                SysOrg org = orgByCode.get(row.getOrgCode());
                if (org == null) {
                    throw new BusinessException("组织编码不存在：" + row.getOrgCode());
                }
                SysOrg dept = StrUtil.isBlank(row.getDeptCode()) ? null : orgByCode.get(row.getDeptCode());
                if (StrUtil.isNotBlank(row.getDeptCode()) && dept == null) {
                    throw new BusinessException("部门编码不存在：" + row.getDeptCode());
                }
                SysRole role = roleByCode.get(row.getRoleCode());
                if (role == null) {
                    throw new BusinessException("角色编码不存在：" + row.getRoleCode());
                }
                if (!dataScopeService.canAccessOrg(current, org.getId()) && !current.isSuperAdmin()) {
                    throw new BusinessException("无权向该组织导入账号");
                }
                String password = StrUtil.isBlank(row.getInitialPassword()) ? RandomUtil.randomString("ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz", 4) + RandomUtil.randomNumbers(4) : row.getInitialPassword();
                SysUser user = new SysUser();
                user.setUsername(row.getUsername());
                user.setRealName(row.getRealName());
                user.setPhone(row.getPhone());
                user.setIdCard(row.getIdCard());
                user.setOrgId(org.getId());
                user.setDeptId(dept == null ? null : dept.getId());
                user.setRoleId(role.getId());
                user.setUserType(roleToUserType(role.getRoleCode()));
                user.setStatus(1);
                user.setPassword(passwordEncoder.encode(password));
                user.setMustChangePwd(1);
                userMapper.insert(user);
                notificationService.send(user.getId(), "账号开通通知",
                        "您的继续教育平台账号已开通，账号：" + user.getUsername() + "，初始密码：" + password + "，首次登录请修改密码。",
                        "ACCOUNT", user.getId());
                Map<String, Object> account = new LinkedHashMap<>();
                account.put("username", user.getUsername());
                account.put("realName", user.getRealName());
                account.put("initialPassword", password);
                accounts.add(account);
                success++;
            } catch (Exception e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("row", line);
                err.put("username", row.getUsername());
                err.put("message", e.getMessage());
                errors.add(err);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("success", success);
        result.put("fail", errors.size());
        result.put("errors", errors);
        result.put("accounts", accounts);
        return result;
    }

    public void exportUsers(LoginUser current, String keyword, Long orgId, Long roleId, OutputStream out) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }
        if (roleId != null) {
            qw.eq(SysUser::getRoleId, roleId);
        }
        List<Long> scope = dataScopeService.accessibleOrgIds(current);
        if (scope != null) {
            if (scope.isEmpty()) {
                qw.eq(SysUser::getId, -1L);
            } else {
                qw.and(w -> w.in(SysUser::getOrgId, scope).or().in(SysUser::getDeptId, scope));
            }
        }
        if (orgId != null) {
            List<Long> ids = dataScopeService.descendants(orgId);
            qw.and(w -> w.in(SysUser::getOrgId, ids).or().in(SysUser::getDeptId, ids));
        }
        List<SysUser> users = userMapper.selectList(qw);
        List<UserExportRow> rows = new ArrayList<>();
        for (SysUser u : users) {
            UserExportRow row = new UserExportRow();
            row.setUsername(u.getUsername());
            row.setRealName(u.getRealName());
            row.setPhone(u.getPhone());
            row.setIdCard(u.getIdCard());
            row.setOrgName(nameOf(u.getOrgId()));
            row.setDeptName(nameOf(u.getDeptId()));
            SysRole role = roleMapper.selectById(u.getRoleId());
            row.setRoleName(role == null ? "" : role.getRoleName());
            row.setUserTypeName(userTypeName(u.getUserType()));
            row.setStatusName(u.getStatus() != null && u.getStatus() == 1 ? "启用" : "停用");
            row.setLastLoginTime(u.getLastLoginTime() == null ? "" : u.getLastLoginTime().format(DT));
            rows.add(row);
        }
        com.alibaba.excel.EasyExcel.write(out, UserExportRow.class).sheet("账号列表").doWrite(rows);
    }

    // ==================== 角色 ====================
    public List<Map<String, Object>> roleList() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysRole role : roles) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", role.getId());
            map.put("roleName", role.getRoleName());
            map.put("roleCode", role.getRoleCode());
            map.put("roleDesc", role.getRoleDesc());
            map.put("dataScope", role.getDataScope());
            map.put("builtIn", role.getBuiltIn());
            map.put("status", role.getStatus());
            map.put("createTime", role.getCreateTime());
            map.put("userCount", userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, role.getId())));
            list.add(map);
        }
        return list;
    }

    public Map<String, Object> roleDetail(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("role", role);
        map.put("menuIds", roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id))
                .stream().map(SysRoleMenu::getMenuId).toList());
        map.put("orgIds", roleOrgMapper.selectList(new LambdaQueryWrapper<SysRoleOrg>().eq(SysRoleOrg::getRoleId, id))
                .stream().map(SysRoleOrg::getOrgId).toList());
        return map;
    }

    @Transactional
    public Long saveRole(RoleSaveRequest req) {
        SysRole same = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, req.roleCode()));
        if (same != null && !same.getId().equals(req.id())) {
            throw new BusinessException("角色编码已存在");
        }
        SysRole role = req.id() == null ? new SysRole() : roleMapper.selectById(req.id());
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (role.getBuiltIn() != null && role.getBuiltIn() == 1 && !role.getRoleCode().equals(req.roleCode())) {
            throw new BusinessException("预置角色编码不可修改");
        }
        role.setRoleName(req.roleName());
        role.setRoleCode(req.roleCode());
        role.setRoleDesc(req.roleDesc());
        role.setDataScope(req.dataScope());
        role.setStatus(req.status() == null ? 1 : req.status());
        role.setBuiltIn(role.getBuiltIn() == null ? 0 : role.getBuiltIn());
        if (req.id() == null) {
            roleMapper.insert(role);
        } else {
            roleMapper.updateById(role);
        }
        if (req.menuIds() != null) {
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, role.getId()));
            for (Long menuId : req.menuIds()) {
                SysRoleMenu link = new SysRoleMenu();
                link.setRoleId(role.getId());
                link.setMenuId(menuId);
                link.setCreateTime(LocalDateTime.now());
                roleMenuMapper.insert(link);
            }
        }
        roleOrgMapper.delete(new LambdaQueryWrapper<SysRoleOrg>().eq(SysRoleOrg::getRoleId, role.getId()));
        if (req.dataScope() != null && req.dataScope() == 5 && req.orgIds() != null) {
            for (Long orgId : req.orgIds()) {
                SysRoleOrg link = new SysRoleOrg();
                link.setRoleId(role.getId());
                link.setOrgId(orgId);
                link.setCreateTime(LocalDateTime.now());
                roleOrgMapper.insert(link);
            }
        }
        permissionService.invalidate(role.getId());
        return role.getId();
    }

    @Transactional
    public void deleteRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (role.getBuiltIn() != null && role.getBuiltIn() == 1) {
            throw new BusinessException("预置角色不可删除");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, id)) > 0) {
            throw new BusinessException("角色下存在账号，禁止删除");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        roleOrgMapper.delete(new LambdaQueryWrapper<SysRoleOrg>().eq(SysRoleOrg::getRoleId, id));
        roleMapper.deleteById(id);
        permissionService.invalidate(id);
    }

    @Transactional
    public void updateRoleStatus(Long id, Integer status) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (role.getBuiltIn() == 1 && "super_admin".equals(role.getRoleCode()) && status == 0) {
            throw new BusinessException("超级管理员角色不可停用");
        }
        role.setStatus(status);
        roleMapper.updateById(role);
        permissionService.invalidate(id);
    }

    // ==================== 菜单 ====================
    public List<com.adultedu.dto.MenuNode> allMenuTree() {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId));
        Map<Long, com.adultedu.dto.MenuNode> nodes = new LinkedHashMap<>();
        for (SysMenu menu : all) {
            com.adultedu.dto.MenuNode node = new com.adultedu.dto.MenuNode();
            node.setId(menu.getId());
            node.setParentId(menu.getParentId());
            node.setMenuName(menu.getMenuName());
            node.setMenuType(menu.getMenuType());
            node.setPath(menu.getPath());
            node.setComponent(menu.getComponent());
            node.setIcon(menu.getIcon());
            node.setPerms(menu.getPerms());
            node.setSort(menu.getSort());
            node.setHidden(menu.getHidden());
            node.setMobileVisible(menu.getMobileVisible());
            nodes.put(menu.getId(), node);
        }
        List<com.adultedu.dto.MenuNode> roots = new ArrayList<>();
        for (SysMenu menu : all) {
            com.adultedu.dto.MenuNode node = nodes.get(menu.getId());
            if (menu.getParentId() == null || menu.getParentId() == 0 || !nodes.containsKey(menu.getParentId())) {
                roots.add(node);
            } else {
                nodes.get(menu.getParentId()).getChildren().add(node);
            }
        }
        return roots;
    }

    @Transactional
    public Long saveMenu(MenuSaveRequest req) {
        SysMenu menu = req.id() == null ? new SysMenu() : menuMapper.selectById(req.id());
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        if (req.id() != null && req.parentId() != null && req.parentId().equals(req.id())) {
            throw new BusinessException("上级菜单不能是自身");
        }
        menu.setParentId(req.parentId() == null ? 0 : req.parentId());
        menu.setMenuName(req.menuName());
        menu.setMenuType(req.menuType());
        menu.setPath(req.path());
        menu.setComponent(req.component());
        menu.setIcon(req.icon());
        menu.setPerms(req.perms());
        menu.setSort(req.sort() == null ? 0 : req.sort());
        menu.setHidden(req.hidden() == null ? 0 : req.hidden());
        menu.setMobileVisible(req.mobileVisible() == null ? 0 : req.mobileVisible());
        menu.setStatus(req.status() == null ? 1 : req.status());
        if (req.id() == null) {
            menuMapper.insert(menu);
        } else {
            menuMapper.updateById(menu);
        }
        permissionService.invalidateAll();
        return menu.getId();
    }

    @Transactional
    public void deleteMenu(Long id) {
        if (menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id)) > 0) {
            throw new BusinessException("存在下级菜单，禁止删除");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
        menuMapper.deleteById(id);
        permissionService.invalidateAll();
    }

    @Transactional
    public void updateMenuStatus(Long id, Integer status) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        menu.setStatus(status);
        menuMapper.updateById(menu);
        permissionService.invalidateAll();
    }

    // ==================== 参数/字典 ====================
    public List<SysConfig> configList() {
        return configMapper.selectList(new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getId));
    }

    public Map<String, String> configMap() {
        return configList().stream().collect(Collectors.toMap(SysConfig::getConfigKey,
                c -> c.getConfigValue() == null ? "" : c.getConfigValue(), (a, b) -> a, LinkedHashMap::new));
    }

    @Transactional
    public void saveConfigs(List<ConfigSaveRequest> list) {
        for (ConfigSaveRequest req : list) {
            SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, req.configKey()));
            if (config == null) {
                config = new SysConfig();
                config.setConfigKey(req.configKey());
                config.setConfigName(req.configName());
                config.setConfigValue(req.configValue());
                config.setConfigType(req.configType());
                config.setRemark(req.remark());
                configMapper.insert(config);
            } else {
                if (req.configName() != null) {
                    config.setConfigName(req.configName());
                }
                config.setConfigValue(req.configValue());
                if (req.configType() != null) {
                    config.setConfigType(req.configType());
                }
                if (req.remark() != null) {
                    config.setRemark(req.remark());
                }
                configMapper.updateById(config);
            }
        }
    }

    public List<Map<String, Object>> dictList(String dictType) {
        LambdaQueryWrapper<SysDict> qw = new LambdaQueryWrapper<SysDict>().orderByAsc(SysDict::getDictType).orderByAsc(SysDict::getSort);
        if (StrUtil.isNotBlank(dictType)) {
            qw.eq(SysDict::getDictType, dictType);
        }
        List<SysDict> all = dictMapper.selectList(qw);
        Map<String, List<SysDict>> group = all.stream().collect(Collectors.groupingBy(SysDict::getDictType, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        group.forEach((type, items) -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("dictType", type);
            map.put("dictName", items.get(0).getDictName());
            map.put("items", items);
            result.add(map);
        });
        return result;
    }

    @Transactional
    public Long saveDict(DictSaveRequest req) {
        SysDict same = dictMapper.selectOne(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, req.dictType()).eq(SysDict::getDictValue, req.dictValue()));
        if (same != null && !same.getId().equals(req.id())) {
            throw new BusinessException("同类型下字典值已存在");
        }
        SysDict dict = req.id() == null ? new SysDict() : dictMapper.selectById(req.id());
        if (dict == null) {
            throw new BusinessException("字典项不存在");
        }
        dict.setDictName(req.dictName());
        dict.setDictType(req.dictType());
        dict.setDictValue(req.dictValue());
        dict.setDictLabel(req.dictLabel());
        dict.setSort(req.sort() == null ? 0 : req.sort());
        dict.setStatus(req.status() == null ? 1 : req.status());
        if (req.id() == null) {
            dictMapper.insert(dict);
        } else {
            dictMapper.updateById(dict);
        }
        return dict.getId();
    }

    @Transactional
    public void deleteDict(Long id) {
        dictMapper.deleteById(id);
    }

    // ==================== 日志 ====================
    public PageResult<Map<String, Object>> operLogPage(long pageNum, long pageSize, String keyword,
                                                       String module, String start, String end) {
        Page<SysOperLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysOperLog> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like(SysOperLog::getUserName, keyword).or().like(SysOperLog::getOperContent, keyword));
        }
        if (StrUtil.isNotBlank(module)) {
            qw.eq(SysOperLog::getOperModule, module);
        }
        if (StrUtil.isNotBlank(start)) {
            qw.ge(SysOperLog::getOperTime, parseTime(start, false));
        }
        if (StrUtil.isNotBlank(end)) {
            qw.le(SysOperLog::getOperTime, parseTime(end, true));
        }
        qw.orderByDesc(SysOperLog::getId);
        Page<SysOperLog> result = operLogMapper.selectPage(page, qw);
        List<Map<String, Object>> records = result.getRecords().stream().map(log -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", log.getId());
            map.put("userName", log.getUserName());
            map.put("orgName", nameOf(log.getOrgId()));
            map.put("operModule", log.getOperModule());
            map.put("operContent", log.getOperContent());
            map.put("operTime", log.getOperTime());
            map.put("ip", log.getIp());
            map.put("device", log.getDevice());
            map.put("result", log.getResult());
            map.put("duration", log.getDuration());
            return map;
        }).toList();
        return PageResult.of(records, result.getTotal(), pageNum, pageSize);
    }

    public PageResult<Map<String, Object>> loginLogPage(LoginUser current, long pageNum, long pageSize,
                                                        String keyword, String start, String end) {
        Page<SysLoginLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysLoginLog> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like(SysLoginLog::getUsername, keyword).or().like(SysLoginLog::getIp, keyword));
        }
        if (!current.isSuperAdmin()) {
            if (current.getUserType() != null && current.getUserType() == 2) {
                List<Long> orgIds = dataScopeService.accessibleOrgIds(current);
                List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                                .and(w -> w.in(SysUser::getOrgId, orgIds == null ? List.of(-1L) : orgIds)
                                        .or().in(SysUser::getDeptId, orgIds == null ? List.of(-1L) : orgIds)))
                        .stream().map(SysUser::getId).toList();
                if (userIds.isEmpty()) {
                    return PageResult.of(List.of(), 0, pageNum, pageSize);
                }
                qw.in(SysLoginLog::getUserId, userIds);
            } else {
                qw.eq(SysLoginLog::getUserId, current.getId());
            }
        }
        if (StrUtil.isNotBlank(start)) {
            qw.ge(SysLoginLog::getLoginTime, parseTime(start, false));
        }
        if (StrUtil.isNotBlank(end)) {
            qw.le(SysLoginLog::getLoginTime, parseTime(end, true));
        }
        qw.orderByDesc(SysLoginLog::getId);
        Page<SysLoginLog> result = loginLogMapper.selectPage(page, qw);
        List<Map<String, Object>> records = result.getRecords().stream().map(log -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", log.getId());
            map.put("username", log.getUsername());
            map.put("realName", log.getUserId() == null ? null : realNameOf(log.getUserId()));
            map.put("ip", log.getIp());
            map.put("device", log.getDevice());
            map.put("status", log.getStatus());
            map.put("msg", log.getMsg());
            map.put("loginTime", log.getLoginTime());
            return map;
        }).toList();
        return PageResult.of(records, result.getTotal(), pageNum, pageSize);
    }

    public void writeOperLog(LoginUser user, String module, String content, String ip, String device,
                             int result, String method, long duration) {
        SysOperLog log = new SysOperLog();
        log.setUserId(user == null ? null : user.getId());
        log.setUserName(user == null ? "匿名" : user.getRealName());
        log.setOrgId(user == null ? null : user.getOrgId());
        log.setOperModule(module);
        log.setOperContent(StrUtil.maxLength(content, 900));
        log.setOperTime(LocalDateTime.now());
        log.setIp(ip);
        log.setDevice(device);
        log.setResult(result);
        log.setMethod(method);
        log.setDuration(duration);
        operLogMapper.insert(log);
    }

    private String nameOf(Long id) {
        if (id == null) {
            return null;
        }
        SysOrg org = orgMapper.selectById(id);
        return org == null ? null : org.getOrgName();
    }

    private String realNameOf(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getRealName();
    }

    private int roleToUserType(String roleCode) {
        return switch (roleCode) {
            case "super_admin", "admin" -> 1;
            case "leader" -> 2;
            case "teacher" -> 3;
            default -> 4;
        };
    }

    private String userTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case 1 -> "管理员";
            case 2 -> "领导";
            case 3 -> "教员";
            default -> "学员";
        };
    }

    private LocalDateTime parseTime(String text, boolean endOfDay) {
        try {
            if (text.length() <= 10) {
                LocalDate date = LocalDate.parse(text, D);
                return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
            }
            return LocalDateTime.parse(text, DT);
        } catch (Exception e) {
            throw new BusinessException("时间格式错误，应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }
}
