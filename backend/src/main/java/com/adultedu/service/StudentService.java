package com.adultedu.service;

import cn.hutool.core.util.StrUtil;
import com.adultedu.common.PageResult;
import com.adultedu.entity.CreditRecord;
import com.adultedu.entity.ProjectMember;
import com.adultedu.entity.StudyRecord;
import com.adultedu.entity.SysOrg;
import com.adultedu.entity.SysRole;
import com.adultedu.entity.SysUser;
import com.adultedu.mapper.CreditRecordMapper;
import com.adultedu.mapper.ProjectMemberMapper;
import com.adultedu.mapper.StudyRecordMapper;
import com.adultedu.mapper.SysOrgMapper;
import com.adultedu.mapper.SysRoleMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 学员台账：汇总学员账号及其学习概况。
 * 数据来源是账号管理里以「学员」角色创建的账号（sys_user.role_id 指向 role_code=student），
 * 不另建新表——学员本质就是带学员角色的用户，学习/学分数据已有独立明细表，聚合查询即可。
 */
@Service
public class StudentService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysOrgMapper orgMapper;
    private final ProjectMemberMapper memberMapper;
    private final StudyRecordMapper studyRecordMapper;
    private final CreditRecordMapper creditRecordMapper;
    private final DataScopeService dataScopeService;

    public StudentService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysOrgMapper orgMapper,
                          ProjectMemberMapper memberMapper, StudyRecordMapper studyRecordMapper,
                          CreditRecordMapper creditRecordMapper, DataScopeService dataScopeService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.orgMapper = orgMapper;
        this.memberMapper = memberMapper;
        this.studyRecordMapper = studyRecordMapper;
        this.creditRecordMapper = creditRecordMapper;
        this.dataScopeService = dataScopeService;
    }

    public PageResult<Map<String, Object>> page(LoginUser current, long pageNum, long pageSize,
                                                String keyword, Long orgId, Integer status,
                                                Long projectId) {
        // 学员角色可能有多个（自定义角色可复制 student code），按 role_code 找齐
        List<SysRole> studentRoles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "student").eq(SysRole::getStatus, 1));
        List<Long> roleIds = studentRoles.stream().map(SysRole::getId).toList();
        if (roleIds.isEmpty()) {
            return PageResult.of(List.of(), 0, pageNum, pageSize);
        }

        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        qw.in(SysUser::getRoleId, roleIds);
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
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
        qw.orderByDesc(SysUser::getId);
        Page<SysUser> result = userMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<SysUser> users = result.getRecords();
        if (users.isEmpty()) {
            return PageResult.of(List.of(), result.getTotal(), pageNum, pageSize);
        }

        // 项目筛选：只保留该班在班学员
        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        if (projectId != null) {
            List<ProjectMember> members = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, projectId).in(ProjectMember::getUserId, userIds)
                    .eq(ProjectMember::getStatus, 1));
            List<Long> allowed = members.stream().map(ProjectMember::getUserId).distinct().toList();
            users = users.stream().filter(u -> allowed.contains(u.getId())).toList();
        }

        List<Map<String, Object>> rows = buildRows(users);
        return PageResult.of(rows, result.getTotal(), pageNum, pageSize);
    }

    private List<Map<String, Object>> buildRows(List<SysUser> users) {
        List<Long> userIds = users.stream().map(SysUser::getId).toList();

        // 批量聚合，避免逐人 N+1 查询
        List<ProjectMember> members = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .in(ProjectMember::getUserId, userIds).eq(ProjectMember::getStatus, 1));
        Map<Long, Long> projectCount = members.stream()
                .collect(Collectors.groupingBy(ProjectMember::getUserId, Collectors.counting()));

        List<StudyRecord> studies = studyRecordMapper.selectList(new LambdaQueryWrapper<StudyRecord>()
                .in(StudyRecord::getUserId, userIds));
        Map<Long, List<StudyRecord>> byUser = studies.stream()
                .collect(Collectors.groupingBy(StudyRecord::getUserId));
        Map<Long, Long> learningCount = new HashMap<>();
        Map<Long, Long> completedCount = new HashMap<>();
        Map<Long, LocalDateTime> lastStudy = new HashMap<>();
        for (Map.Entry<Long, List<StudyRecord>> e : byUser.entrySet()) {
            long learning = e.getValue().stream().filter(s -> Objects.equals(s.getStatus(), 1)).count();
            long completed = e.getValue().stream().filter(s -> Objects.equals(s.getStatus(), 2)).count();
            learningCount.put(e.getKey(), learning);
            completedCount.put(e.getKey(), completed);
            e.getValue().stream()
                    .map(s -> s.getLastHeartbeat() != null ? s.getLastHeartbeat() : s.getUpdateTime())
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .ifPresent(t -> lastStudy.put(e.getKey(), t));
        }

        List<CreditRecord> credits = creditRecordMapper.selectList(new LambdaQueryWrapper<CreditRecord>()
                .in(CreditRecord::getUserId, userIds).eq(CreditRecord::getAuditStatus, 2));
        Map<Long, BigDecimal> creditSum = credits.stream()
                .collect(Collectors.groupingBy(CreditRecord::getUserId,
                        Collectors.reducing(BigDecimal.ZERO, CreditRecord::getCreditValue, BigDecimal::add)));

        // 批量取组织/部门名
        List<Long> orgIds = users.stream()
                .flatMap(u -> java.util.stream.Stream.of(u.getOrgId(), u.getDeptId()))
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> orgNames = orgIds.isEmpty() ? Map.of()
                : orgMapper.selectBatchIds(orgIds).stream()
                        .collect(Collectors.toMap(SysOrg::getId, SysOrg::getOrgName, (a, b) -> a));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysUser u : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", u.getId());
            row.put("username", u.getUsername());
            row.put("realName", u.getRealName());
            row.put("phone", u.getPhone());
            row.put("orgId", u.getOrgId());
            row.put("orgName", u.getOrgId() == null ? null : orgNames.get(u.getOrgId()));
            row.put("deptName", u.getDeptId() == null ? null : orgNames.get(u.getDeptId()));
            row.put("status", u.getStatus());
            row.put("faceImg", u.getFaceImg());
            row.put("createTime", u.getCreateTime());
            row.put("lastLoginTime", u.getLastLoginTime());
            row.put("enrolledProjects", projectCount.getOrDefault(u.getId(), 0L));
            row.put("learningCourses", learningCount.getOrDefault(u.getId(), 0L));
            row.put("completedCourses", completedCount.getOrDefault(u.getId(), 0L));
            row.put("totalCredits", creditSum.getOrDefault(u.getId(), BigDecimal.ZERO));
            row.put("lastStudyTime", lastStudy.get(u.getId()));
            rows.add(row);
        }
        return rows;
    }

    /**
     * 保存学员人脸基准照（管理端上传后回写 URL），用于核验时 1:1 比对。
     */
    public void updateFaceImg(LoginUser current, Long userId, String faceImg) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new com.adultedu.common.BusinessException("学员不存在");
        }
        List<Long> scope = dataScopeService.accessibleOrgIds(current);
        if (scope != null && user.getOrgId() != null && !scope.contains(user.getOrgId())) {
            throw new com.adultedu.common.BusinessException(403, "无权操作该学员");
        }
        user.setFaceImg(StrUtil.isBlank(faceImg) ? null : faceImg.trim());
        userMapper.updateById(user);
    }
}
