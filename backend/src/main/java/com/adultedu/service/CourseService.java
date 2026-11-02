package com.adultedu.service;

import cn.hutool.core.util.StrUtil;
import com.adultedu.common.BusinessException;
import com.adultedu.common.PageResult;
import com.adultedu.dto.BizDtos.AttachmentDto;
import com.adultedu.dto.BizDtos.CourseSaveRequest;
import com.adultedu.dto.BizDtos.QuestionDto;
import com.adultedu.dto.BizDtos.VerifyGearSaveRequest;
import com.adultedu.entity.Course;
import com.adultedu.entity.CourseAttachment;
import com.adultedu.entity.ExamRecord;
import com.adultedu.entity.ProjectMember;
import com.adultedu.entity.ProjectTeacher;
import com.adultedu.entity.Question;
import com.adultedu.entity.StudyRecord;
import com.adultedu.entity.SysUser;
import com.adultedu.entity.TrainingProject;
import com.adultedu.entity.VerifyGear;
import com.adultedu.mapper.CourseAttachmentMapper;
import com.adultedu.mapper.CourseMapper;
import com.adultedu.mapper.ExamRecordMapper;
import com.adultedu.mapper.ProjectMemberMapper;
import com.adultedu.mapper.ProjectTeacherMapper;
import com.adultedu.mapper.QuestionMapper;
import com.adultedu.mapper.StudyRecordMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.mapper.TrainingProjectMapper;
import com.adultedu.mapper.VerifyGearMapper;
import com.adultedu.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseMapper courseMapper;
    private final CourseAttachmentMapper attachmentMapper;
    private final QuestionMapper questionMapper;
    private final TrainingProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final ProjectTeacherMapper teacherMapper;
    private final StudyRecordMapper studyRecordMapper;
    private final ExamRecordMapper examRecordMapper;
    private final SysUserMapper userMapper;
    private final VerifyGearMapper gearMapper;
    private final DataScopeService dataScopeService;
    private final NotificationService notificationService;

    public PageResult<Map<String, Object>> coursePage(LoginUser current, long pageNum, long pageSize,
                                                      String keyword, Long projectId, Integer status, Integer verifyGearCode) {
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.like(Course::getCourseName, keyword);
        }
        if (projectId != null) {
            qw.eq(Course::getProjectId, projectId);
        }
        if (status != null) {
            qw.eq(Course::getStatus, status);
        }
        if (verifyGearCode != null) {
            qw.eq(Course::getVerifyGearCode, verifyGearCode);
        }
        if (!current.isSuperAdmin() && "teacher".equals(current.getRoleCode())) {
            List<Long> projectIds = teacherMapper.selectList(new LambdaQueryWrapper<ProjectTeacher>()
                            .eq(ProjectTeacher::getUserId, current.getId()))
                    .stream().map(ProjectTeacher::getProjectId).toList();
            List<Long> all = new ArrayList<>(projectIds);
            courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Course::getCreateUserId, current.getId())
                            .select(Course::getProjectId))
                    .forEach(c -> all.add(c.getProjectId()));
            if (all.isEmpty()) {
                return PageResult.of(List.of(), 0, pageNum, pageSize);
            }
            qw.in(Course::getProjectId, all.stream().distinct().toList());
        } else if (!current.isSuperAdmin() && "leader".equals(current.getRoleCode())) {
            List<Long> orgIds = dataScopeService.accessibleOrgIds(current);
            if (orgIds == null) {
                // 全部组织
            } else if (orgIds.isEmpty()) {
                return PageResult.of(List.of(), 0, pageNum, pageSize);
            } else {
                List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                                .and(w -> w.in(SysUser::getOrgId, orgIds).or().in(SysUser::getDeptId, orgIds)))
                        .stream().map(SysUser::getId).toList();
                List<Long> projectIds = userIds.isEmpty() ? List.of() : memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                                .in(ProjectMember::getUserId, userIds).eq(ProjectMember::getStatus, 1))
                        .stream().map(ProjectMember::getProjectId).distinct().toList();
                if (projectIds.isEmpty()) {
                    return PageResult.of(List.of(), 0, pageNum, pageSize);
                }
                qw.in(Course::getProjectId, projectIds);
            }
        }
        qw.orderByAsc(Course::getSort).orderByDesc(Course::getId);
        Page<Course> page = courseMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> records = enrich(page.getRecords());
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    private List<Map<String, Object>> enrich(List<Course> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        List<Long> projectIds = courses.stream().map(Course::getProjectId).distinct().toList();
        List<Long> userIds = courses.stream().map(Course::getCreateUserId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> projectNames = projectIds.isEmpty() ? Map.of() : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(TrainingProject::getId, TrainingProject::getProjectName, (a, b) -> a));
        Map<Long, String> userNames = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        Map<Integer, String> gearNames = gearMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .collect(Collectors.toMap(VerifyGear::getGearCode, VerifyGear::getGearName, (a, b) -> a));
        Map<Long, Long> studyCounts = new HashMap<>();
        Map<Long, BigDecimal> avgProgress = new HashMap<>();
        for (Map<String, Object> row : studyRecordMapper.selectMaps(new QueryWrapper<StudyRecord>()
                .select("course_id", "COUNT(*) AS cnt", "AVG(progress) AS avg_progress")
                .in("course_id", courseIds).groupBy("course_id"))) {
            Object cid = row.get("course_id");
            if (cid != null) {
                studyCounts.put(((Number) cid).longValue(), ((Number) row.get("cnt")).longValue());
                avgProgress.put(((Number) cid).longValue(), new BigDecimal(row.get("avg_progress").toString()));
            }
        }
        Map<Long, Long> questionCounts = new HashMap<>();
        for (Question q : questionMapper.selectList(new LambdaQueryWrapper<Question>().in(Question::getCourseId, courseIds))) {
            questionCounts.merge(q.getCourseId(), 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>(courses.size());
        for (Course c : courses) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("projectId", c.getProjectId());
            m.put("projectName", projectNames.get(c.getProjectId()));
            m.put("courseName", c.getCourseName());
            m.put("cover", c.getCover());
            m.put("credit", c.getCredit());
            m.put("studyHour", c.getStudyHour());
            m.put("isRequired", c.getIsRequired());
            m.put("needExam", c.getNeedExam());
            m.put("passScore", c.getPassScore());
            m.put("examLimit", c.getExamLimit());
            m.put("videoUrl", c.getVideoUrl());
            m.put("videoDuration", c.getVideoDuration());
            m.put("intro", c.getIntro());
            m.put("createUserId", c.getCreateUserId());
            m.put("createUserName", userNames.get(c.getCreateUserId()));
            m.put("status", c.getStatus());
            m.put("verifyGearCode", c.getVerifyGearCode());
            m.put("verifyGearName", gearNames.getOrDefault(c.getVerifyGearCode(), "不核验"));
            m.put("allowSeek", c.getAllowSeek());
            m.put("sort", c.getSort());
            m.put("studyCount", studyCounts.getOrDefault(c.getId(), 0L));
            m.put("avgProgress", avgProgress.getOrDefault(c.getId(), BigDecimal.ZERO).setScale(1, java.math.RoundingMode.HALF_UP));
            m.put("questionCount", questionCounts.getOrDefault(c.getId(), 0L));
            m.put("createTime", c.getCreateTime());
            result.add(m);
        }
        return result;
    }

    public Map<String, Object> courseDetail(Long id, LoginUser current) {
        Course c = courseMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("课程不存在");
        }
        if (!canViewProject(current, c.getProjectId())) {
            throw new BusinessException(403, "无权查看该课程");
        }
        boolean manage = canManage(c, current);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("projectId", c.getProjectId());
        TrainingProject p = projectMapper.selectById(c.getProjectId());
        m.put("projectName", p == null ? null : p.getProjectName());
        m.put("courseName", c.getCourseName());
        m.put("cover", c.getCover());
        m.put("credit", c.getCredit());
        m.put("studyHour", c.getStudyHour());
        m.put("isRequired", c.getIsRequired());
        m.put("needExam", c.getNeedExam());
        m.put("passScore", c.getPassScore());
        m.put("examLimit", c.getExamLimit());
        m.put("videoUrl", c.getVideoUrl());
        m.put("videoDuration", c.getVideoDuration());
        m.put("intro", c.getIntro());
        m.put("status", c.getStatus());
        m.put("verifyGearCode", c.getVerifyGearCode());
        m.put("verifyGearJson", c.getVerifyGearJson());
        m.put("allowSeek", c.getAllowSeek());
        m.put("sort", c.getSort());
        m.put("createUserId", c.getCreateUserId());
        m.put("createTime", c.getCreateTime());
        m.put("attachments", attachmentMapper.selectList(new LambdaQueryWrapper<CourseAttachment>()
                .eq(CourseAttachment::getCourseId, id).orderByAsc(CourseAttachment::getId)));
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, id).orderByAsc(Question::getSort).orderByAsc(Question::getId));
        if (!manage) {
            questions.forEach(q -> q.setAnswer(null));
        }
        m.put("questions", questions);
        m.put("manageable", manage);
        StudyRecord record = studyRecordMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, current.getId()).eq(StudyRecord::getCourseId, id));
        m.put("studyRecord", record);
        ExamRecord best = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, current.getId()).eq(ExamRecord::getCourseId, id)
                .orderByDesc(ExamRecord::getScore).last("limit 1"));
        m.put("bestExam", best);
        return m;
    }

    @Transactional
    public Long saveCourse(LoginUser current, CourseSaveRequest req) {
        TrainingProject project = projectMapper.selectById(req.projectId());
        if (project == null) {
            throw new BusinessException("培训班不存在");
        }
        Course entity;
        if (req.id() != null) {
            entity = courseMapper.selectById(req.id());
            if (entity == null) {
                throw new BusinessException("课程不存在");
            }
            if (!canManage(entity, current)) {
                throw new BusinessException(403, "无权编辑该课程");
            }
        } else {
            if (!current.isSuperAdmin() && !isProjectTeacher(current.getId(), req.projectId())) {
                throw new BusinessException(403, "仅管理员或被指派教员可创建课程");
            }
            entity = new Course();
            entity.setCreateUserId(current.getId());
        }
        VerifyGear gear = gearMapper.selectOne(new LambdaQueryWrapper<VerifyGear>()
                .eq(VerifyGear::getGearCode, req.verifyGearCode() == null ? 0 : req.verifyGearCode()));
        entity.setProjectId(req.projectId());
        entity.setCourseName(req.courseName().trim());
        entity.setCover(req.cover());
        entity.setCredit(req.credit());
        entity.setStudyHour(req.studyHour());
        entity.setIsRequired(req.isRequired() == null ? 1 : req.isRequired());
        entity.setNeedExam(req.needExam() == null ? 0 : req.needExam());
        entity.setPassScore(req.passScore() == null ? 60 : req.passScore());
        entity.setExamLimit(req.examLimit() == null ? 0 : req.examLimit());
        entity.setVideoUrl(req.videoUrl());
        entity.setVideoDuration(req.videoDuration() == null ? 0 : req.videoDuration());
        entity.setIntro(req.intro());
        entity.setStatus(req.status() == null ? 0 : req.status());
        entity.setVerifyGearCode(req.verifyGearCode() == null ? 0 : req.verifyGearCode());
        entity.setVerifyGearJson(StrUtil.isNotBlank(req.verifyGearJson()) ? req.verifyGearJson()
                : (gear == null ? null : gear.getTriggerPoints()));
        entity.setAllowSeek(req.allowSeek() == null ? 0 : req.allowSeek());
        entity.setSort(req.sort() == null ? 0 : req.sort());
        if (entity.getStatus() == 1) {
            validatePublish(entity, req.questions());
        }
        if (entity.getId() == null) {
            courseMapper.insert(entity);
        } else {
            courseMapper.updateById(entity);
        }
        saveAttachments(entity.getId(), req.attachments());
        saveQuestions(entity.getId(), req.questions());
        if (entity.getStatus() == 1) {
            notifyPublish(entity, current, req.id() == null);
        }
        return entity.getId();
    }

    private void validatePublish(Course course, List<QuestionDto> questions) {
        if (StrUtil.isBlank(course.getVideoUrl())) {
            throw new BusinessException("上架前请上传课程视频");
        }
        if (course.getNeedExam() != null && course.getNeedExam() == 1) {
            long count = questions == null ? questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                    .eq(Question::getCourseId, course.getId())) : questions.size();
            if (count == 0) {
                throw new BusinessException("该课程需要测验，请先配置至少一道题目");
            }
        }
    }

    private void saveAttachments(Long courseId, List<AttachmentDto> attachments) {
        if (attachments == null) {
            return;
        }
        attachmentMapper.delete(new LambdaQueryWrapper<CourseAttachment>().eq(CourseAttachment::getCourseId, courseId));
        for (AttachmentDto dto : attachments) {
            if (StrUtil.isBlank(dto.fileUrl())) {
                continue;
            }
            CourseAttachment a = new CourseAttachment();
            a.setCourseId(courseId);
            a.setFileName(dto.fileName());
            a.setFileUrl(dto.fileUrl());
            a.setFileType(dto.fileType());
            a.setFileSize(dto.fileSize());
            attachmentMapper.insert(a);
        }
    }

    private void saveQuestions(Long courseId, List<QuestionDto> questions) {
        if (questions == null) {
            return;
        }
        questionMapper.delete(new LambdaQueryWrapper<Question>().eq(Question::getCourseId, courseId));
        int sort = 1;
        for (QuestionDto dto : questions) {
            if (StrUtil.isBlank(dto.content()) || StrUtil.isBlank(dto.answer())) {
                throw new BusinessException("题目内容和答案不能为空");
            }
            Question q = new Question();
            q.setCourseId(courseId);
            q.setQuestionType(dto.questionType() == null ? 1 : dto.questionType());
            q.setContent(dto.content());
            q.setOptionsJson(dto.optionsJson());
            q.setAnswer(dto.answer().trim().toUpperCase());
            q.setScore(dto.score() == null ? BigDecimal.TEN : dto.score());
            q.setSort(dto.sort() == null ? sort : dto.sort());
            questionMapper.insert(q);
            sort++;
        }
    }

    private void notifyPublish(Course course, LoginUser current, boolean isNew) {
        List<Long> memberIds = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, course.getProjectId()).eq(ProjectMember::getStatus, 1))
                .stream().map(ProjectMember::getUserId).toList();
        if (memberIds.isEmpty()) {
            return;
        }
        String title = isNew ? "新课程已上线" : "课程信息已更新";
        String content = "《" + course.getCourseName() + "》" + (isNew ? "已发布" : "已更新")
                + "，学分 " + course.getCredit() + "，请及时学习。";
        notificationService.sendBatch(memberIds, title, content, "COURSE", course.getId());
    }

    @Transactional
    public void deleteCourse(Long id, LoginUser current) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!canManage(course, current)) {
            throw new BusinessException(403, "无权删除该课程");
        }
        long studied = studyRecordMapper.selectCount(new LambdaQueryWrapper<StudyRecord>().eq(StudyRecord::getCourseId, id));
        if (studied > 0) {
            throw new BusinessException("该课程已有学习记录，不能删除，可改为下架");
        }
        attachmentMapper.delete(new LambdaQueryWrapper<CourseAttachment>().eq(CourseAttachment::getCourseId, id));
        questionMapper.delete(new LambdaQueryWrapper<Question>().eq(Question::getCourseId, id));
        courseMapper.deleteById(id);
    }

    public void updateStatus(Long id, Integer status, LoginUser current) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!canManage(course, current)) {
            throw new BusinessException(403, "无权操作该课程");
        }
        if (status != null && status == 1) {
            validatePublish(course, null);
        }
        int old = course.getStatus() == null ? 0 : course.getStatus();
        course.setStatus(status);
        courseMapper.updateById(course);
        if (status != null && status == 1 && old != 1) {
            notifyPublish(course, current, true);
        }
    }

    public boolean canManage(Course course, LoginUser current) {
        return current.isSuperAdmin() || ("teacher".equals(current.getRoleCode())
                && current.getId().equals(course.getCreateUserId()));
    }

    public boolean isProjectTeacher(Long userId, Long projectId) {
        return teacherMapper.selectCount(new LambdaQueryWrapper<ProjectTeacher>()
                .eq(ProjectTeacher::getUserId, userId).eq(ProjectTeacher::getProjectId, projectId)) > 0;
    }

    public boolean canViewProject(LoginUser current, Long projectId) {
        if (current.isSuperAdmin() || (current.getDataScope() != null && current.getDataScope() == 1)) {
            return true;
        }
        if ("student".equals(current.getRoleCode())) {
            return memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                    .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, current.getId())
                    .eq(ProjectMember::getStatus, 1)) > 0;
        }
        if ("teacher".equals(current.getRoleCode())) {
            return isProjectTeacher(current.getId(), projectId) || courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                    .eq(Course::getProjectId, projectId).eq(Course::getCreateUserId, current.getId())) > 0;
        }
        List<Long> orgIds = dataScopeService.accessibleOrgIds(current);
        if (orgIds == null) {
            return true;
        }
        if (orgIds.isEmpty()) {
            return false;
        }
        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .and(w -> w.in(SysUser::getOrgId, orgIds).or().in(SysUser::getDeptId, orgIds)))
                .stream().map(SysUser::getId).toList();
        return !userIds.isEmpty() && memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getStatus, 1)
                .in(ProjectMember::getUserId, userIds)) > 0;
    }

    public List<Map<String, Object>> courseOptions(Long projectId, LoginUser current) {
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<>();
        if (projectId != null) {
            qw.eq(Course::getProjectId, projectId);
        }
        qw.orderByAsc(Course::getSort).orderByDesc(Course::getId);
        List<Course> list = courseMapper.selectList(qw);
        Map<Long, String> projectNames = projectMapper.selectList(new LambdaQueryWrapper<TrainingProject>()
                        .select(TrainingProject::getId, TrainingProject::getProjectName))
                .stream().collect(Collectors.toMap(TrainingProject::getId, TrainingProject::getProjectName, (a, b) -> a));
        return list.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("courseName", c.getCourseName());
            m.put("projectId", c.getProjectId());
            m.put("projectName", projectNames.get(c.getProjectId()));
            m.put("credit", c.getCredit());
            m.put("status", c.getStatus());
            return m;
        }).toList();
    }

    public List<VerifyGear> gears(boolean onlyEnabled) {
        LambdaQueryWrapper<VerifyGear> qw = new LambdaQueryWrapper<>();
        if (onlyEnabled) {
            qw.eq(VerifyGear::getStatus, 1);
        }
        qw.orderByAsc(VerifyGear::getGearCode);
        return gearMapper.selectList(qw);
    }

    @Transactional
    public Long saveGear(VerifyGearSaveRequest req) {
        VerifyGear gear;
        if (req.id() != null) {
            gear = gearMapper.selectById(req.id());
            if (gear == null) {
                throw new BusinessException("核验档位不存在");
            }
            if (gear.getBuiltIn() != null && gear.getBuiltIn() == 1 && !gear.getGearCode().equals(req.gearCode())) {
                throw new BusinessException("预置档位不能修改编码");
            }
        } else {
            if (gearMapper.selectCount(new LambdaQueryWrapper<VerifyGear>()
                    .eq(VerifyGear::getGearCode, req.gearCode())) > 0) {
                throw new BusinessException("档位编码已存在");
            }
            gear = new VerifyGear();
            gear.setBuiltIn(0);
        }
        gear.setGearCode(req.gearCode());
        gear.setGearName(req.gearName());
        gear.setTriggerPoints(req.triggerPoints());
        gear.setIntervalMinute(req.intervalMinute() == null ? 0 : req.intervalMinute());
        gear.setTimeoutSecond(req.timeoutSecond() == null ? 60 : req.timeoutSecond());
        gear.setMaxFailCount(req.maxFailCount() == null ? 3 : req.maxFailCount());
        gear.setFailStrategy(req.failStrategy() == null ? 1 : req.failStrategy());
        gear.setStatus(req.status() == null ? 1 : req.status());
        gear.setDescription(req.description());
        if (gear.getId() == null) {
            gearMapper.insert(gear);
        } else {
            gearMapper.updateById(gear);
        }
        return gear.getId();
    }

    public void deleteGear(Long id) {
        VerifyGear gear = gearMapper.selectById(id);
        if (gear == null) {
            throw new BusinessException("核验档位不存在");
        }
        if (gear.getBuiltIn() != null && gear.getBuiltIn() == 1) {
            throw new BusinessException("预置档位不能删除");
        }
        if (courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getVerifyGearCode, gear.getGearCode())) > 0) {
            throw new BusinessException("该档位已被课程使用，不能删除");
        }
        gearMapper.deleteById(id);
    }
}
