package com.adultedu.controller;

import com.adultedu.aspect.OperLog;
import com.adultedu.common.PageResult;
import com.adultedu.common.R;
import com.adultedu.dto.BizDtos.MemberRequest;
import com.adultedu.dto.BizDtos.ProjectSaveRequest;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.ProjectService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAuthority('project:list')")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer status) {
        return R.ok(projectService.projectPage(SecurityUtils.current(), pageNum, pageSize, keyword, status));
    }

    @GetMapping("/options")
    public R<List<Map<String, Object>>> options() {
        return R.ok(projectService.projectOptions());
    }

    @GetMapping("/teacher-options")
    @PreAuthorize("hasAuthority('project:teacher')")
    public R<List<Map<String, Object>>> teacherOptions() {
        return R.ok(projectService.teacherOptions());
    }

    @GetMapping("/student-options")
    @PreAuthorize("hasAuthority('project:member')")
    public R<List<Map<String, Object>>> studentOptions(@RequestParam(required = false) Long projectId) {
        return R.ok(projectService.studentOptions(projectId));
    }

    @GetMapping("/my")
    public R<List<Map<String, Object>>> myProjects() {
        return R.ok(projectService.myProjects(SecurityUtils.current().getId()));
    }

    @GetMapping("/my/{id}")
    public R<Map<String, Object>> myProjectDetail(@PathVariable Long id) {
        return R.ok(projectService.myProjectDetail(id, SecurityUtils.current().getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('project:list')")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(projectService.projectDetail(id, SecurityUtils.current()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('project:add')")
    @OperLog(module = "培训班管理", content = "新增培训班")
    public R<Long> save(@Valid @RequestBody ProjectSaveRequest req) {
        return R.ok(projectService.saveProject(req, SecurityUtils.current()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('project:edit')")
    @OperLog(module = "培训班管理", content = "编辑培训班")
    public R<Long> update(@PathVariable Long id, @Valid @RequestBody ProjectSaveRequest req) {
        ProjectSaveRequest withId = new ProjectSaveRequest(id, req.projectName(), req.projectCode(), req.description(),
                req.startTime(), req.endTime(), req.needCredit(), req.status(), req.certificateTemplateUrl(),
                req.allowOverdue(), req.autoGraduate(), req.needTeacherAudit(), req.needLeaderAudit(), req.needAdminAudit());
        return R.ok(projectService.saveProject(withId, SecurityUtils.current()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('project:delete')")
    @OperLog(module = "培训班管理", content = "删除培训班")
    public R<Void> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('project:edit')")
    @OperLog(module = "培训班管理", content = "变更培训班状态")
    public R<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        projectService.updateProjectStatus(id, status);
        return R.ok();
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAuthority('project:member') or hasAuthority('project:list')")
    public R<PageResult<Map<String, Object>>> members(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") long pageNum,
                                                      @RequestParam(defaultValue = "10") long pageSize,
                                                      @RequestParam(required = false) String keyword) {
        return R.ok(projectService.memberPage(id, pageNum, pageSize, keyword));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('project:member')")
    @OperLog(module = "培训班管理", content = "添加培训班学员")
    public R<Map<String, Object>> addMembers(@PathVariable Long id, @RequestBody MemberRequest req) {
        return R.ok(projectService.addMembers(id, req.userIds()));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('project:member')")
    @OperLog(module = "培训班管理", content = "移除培训班学员")
    public R<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return R.ok();
    }

    @GetMapping("/{id}/teachers")
    @PreAuthorize("hasAuthority('project:teacher') or hasAuthority('project:list')")
    public R<List<Map<String, Object>>> teachers(@PathVariable Long id) {
        return R.ok(projectService.teacherList(id));
    }

    @PutMapping("/{id}/teachers")
    @PreAuthorize("hasAuthority('project:teacher')")
    @OperLog(module = "培训班管理", content = "分配培训班教员")
    public R<Void> assignTeachers(@PathVariable Long id, @RequestBody MemberRequest req) {
        projectService.assignTeachers(id, req.userIds());
        return R.ok();
    }
}
