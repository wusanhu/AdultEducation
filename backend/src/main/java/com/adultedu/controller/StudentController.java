package com.adultedu.controller;

import com.adultedu.common.PageResult;
import com.adultedu.common.R;
import com.adultedu.security.LoginUser;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.StudentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 学员台账（管理端）：查看所有学员账号及其学习概况。
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('student:list')")
    public R<PageResult<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long projectId) {
        LoginUser current = SecurityUtils.current();
        return R.ok(studentService.page(current, pageNum, pageSize, keyword, orgId, status, projectId));
    }
}
