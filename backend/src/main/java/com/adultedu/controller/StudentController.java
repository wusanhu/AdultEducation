package com.adultedu.controller;

import com.adultedu.common.PageResult;
import com.adultedu.common.R;
import com.adultedu.security.LoginUser;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.StudentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * 保存学员人脸基准照（核验 1:1 比对依据），faceImg 为文件上传接口返回的 URL，传空清除。
     */
    @PutMapping("/{id}/face-img")
    @PreAuthorize("hasAuthority('student:list')")
    public R<Void> updateFaceImg(@PathVariable Long id, @RequestBody Map<String, String> body) {
        LoginUser current = SecurityUtils.current();
        studentService.updateFaceImg(current, id, body.get("faceImg"));
        return R.ok();
    }
}
