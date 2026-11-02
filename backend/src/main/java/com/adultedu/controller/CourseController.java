package com.adultedu.controller;

import com.adultedu.aspect.OperLog;
import com.adultedu.common.PageResult;
import com.adultedu.common.R;
import com.adultedu.dto.BizDtos.CourseSaveRequest;
import com.adultedu.dto.BizDtos.VerifyGearSaveRequest;
import com.adultedu.entity.VerifyGear;
import com.adultedu.security.SecurityUtils;
import com.adultedu.service.CourseService;
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
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @GetMapping
    @PreAuthorize("hasAuthority('course:list')")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long projectId,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) Integer verifyGearCode) {
        return R.ok(courseService.coursePage(SecurityUtils.current(), pageNum, pageSize, keyword, projectId, status, verifyGearCode));
    }

    @GetMapping("/options")
    public R<List<Map<String, Object>>> options(@RequestParam(required = false) Long projectId) {
        return R.ok(courseService.courseOptions(projectId, SecurityUtils.current()));
    }

    @GetMapping("/gears")
    public R<List<VerifyGear>> gears(@RequestParam(defaultValue = "false") boolean onlyEnabled) {
        return R.ok(courseService.gears(onlyEnabled));
    }

    @PostMapping("/gears")
    @PreAuthorize("hasAuthority('verify:gear')")
    @OperLog(module = "核验档位", content = "保存核验档位")
    public R<Long> saveGear(@Valid @RequestBody VerifyGearSaveRequest req) {
        return R.ok(courseService.saveGear(req));
    }

    @DeleteMapping("/gears/{id}")
    @PreAuthorize("hasAuthority('verify:gear')")
    @OperLog(module = "核验档位", content = "删除核验档位")
    public R<Void> deleteGear(@PathVariable Long id) {
        courseService.deleteGear(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(courseService.courseDetail(id, SecurityUtils.current()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('course:add') or hasAuthority('course:edit')")
    @OperLog(module = "课程管理", content = "保存课程")
    public R<Long> save(@Valid @RequestBody CourseSaveRequest req) {
        return R.ok(courseService.saveCourse(SecurityUtils.current(), req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('course:edit')")
    @OperLog(module = "课程管理", content = "编辑课程")
    public R<Long> update(@PathVariable Long id, @Valid @RequestBody CourseSaveRequest req) {
        CourseSaveRequest withId = new CourseSaveRequest(id, req.projectId(), req.courseName(), req.cover(),
                req.credit(), req.studyHour(), req.isRequired(), req.needExam(), req.passScore(), req.examLimit(),
                req.videoUrl(), req.videoDuration(), req.intro(), req.status(), req.verifyGearCode(),
                req.verifyGearJson(), req.allowSeek(), req.sort(), req.attachments(), req.questions());
        return R.ok(courseService.saveCourse(SecurityUtils.current(), withId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('course:delete')")
    @OperLog(module = "课程管理", content = "删除课程")
    public R<Void> delete(@PathVariable Long id) {
        courseService.deleteCourse(id, SecurityUtils.current());
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('course:publish')")
    @OperLog(module = "课程管理", content = "课程上下架")
    public R<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        courseService.updateStatus(id, status, SecurityUtils.current());
        return R.ok();
    }
}
