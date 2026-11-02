package com.adultedu.service;

import cn.hutool.core.util.StrUtil;
import com.adultedu.common.BusinessException;
import com.adultedu.common.PageResult;
import com.adultedu.dto.BizDtos.AnswerItem;
import com.adultedu.dto.BizDtos.ExamSubmitRequest;
import com.adultedu.entity.Course;
import com.adultedu.entity.ExamRecord;
import com.adultedu.entity.ProjectMember;
import com.adultedu.entity.Question;
import com.adultedu.entity.StudyRecord;
import com.adultedu.entity.SysUser;
import com.adultedu.entity.TrainingProject;
import com.adultedu.mapper.CourseMapper;
import com.adultedu.mapper.ExamRecordMapper;
import com.adultedu.mapper.ProjectMemberMapper;
import com.adultedu.mapper.QuestionMapper;
import com.adultedu.mapper.StudyRecordMapper;
import com.adultedu.mapper.SysUserMapper;
import com.adultedu.mapper.TrainingProjectMapper;
import com.adultedu.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {
    private final CourseMapper courseMapper;
    private final QuestionMapper questionMapper;
    private final ExamRecordMapper examMapper;
    private final StudyRecordMapper studyMapper;
    private final ProjectMemberMapper memberMapper;
    private final SysUserMapper userMapper;
    private final TrainingProjectMapper projectMapper;
    private final CreditService creditService;
    private final StudyService studyService;
    private final NotificationService notificationService;

    public Map<String, Object> paper(LoginUser current, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (course.getNeedExam() == null || course.getNeedExam() != 1) {
            throw new BusinessException("该课程不需要课后测验");
        }
        StudyRecord study = studyMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, current.getId()).eq(StudyRecord::getCourseId, courseId));
        if (study == null || study.getStatus() == null || study.getStatus() != 2) {
            throw new BusinessException("请先完成课程视频学习后再参加测验");
        }
        List<ExamRecord> attempts = examMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, current.getId()).eq(ExamRecord::getCourseId, courseId)
                .orderByDesc(ExamRecord::getAttemptNo));
        boolean passed = attempts.stream().anyMatch(a -> a.getIsPass() != null && a.getIsPass() == 1);
        int limit = course.getExamLimit() == null ? 0 : course.getExamLimit();
        int remaining = limit == 0 ? -1 : Math.max(0, limit - attempts.size());
        if (passed) {
            throw new BusinessException("您已通过该课程测验，无需重复作答");
        }
        if (limit > 0 && attempts.size() >= limit) {
            throw new BusinessException("已达到测验次数上限（" + limit + " 次），无法继续作答");
        }
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId).orderByAsc(Question::getSort).orderByAsc(Question::getId));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("courseId", course.getId());
        m.put("courseName", course.getCourseName());
        m.put("passScore", course.getPassScore());
        m.put("examLimit", limit);
        m.put("attemptCount", attempts.size());
        m.put("remainingAttempts", remaining);
        m.put("bestScore", attempts.stream().map(ExamRecord::getScore).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
        m.put("questions", questions.stream().map(q -> {
            Map<String, Object> qm = new LinkedHashMap<>();
            qm.put("id", q.getId());
            qm.put("questionType", q.getQuestionType());
            qm.put("content", q.getContent());
            qm.put("optionsJson", q.getOptionsJson());
            qm.put("score", q.getScore());
            qm.put("sort", q.getSort());
            return qm;
        }).toList());
        return m;
    }

    @Transactional
    public Map<String, Object> submit(LoginUser current, Long courseId, ExamSubmitRequest req) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (course.getNeedExam() == null || course.getNeedExam() != 1) {
            throw new BusinessException("该课程不需要课后测验");
        }
        StudyRecord study = studyMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, current.getId()).eq(StudyRecord::getCourseId, courseId));
        if (study == null || study.getStatus() == null || study.getStatus() != 2) {
            throw new BusinessException("请先完成课程视频学习后再参加测验");
        }
        List<ExamRecord> attempts = examMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, current.getId()).eq(ExamRecord::getCourseId, courseId));
        if (attempts.stream().anyMatch(a -> a.getIsPass() != null && a.getIsPass() == 1)) {
            throw new BusinessException("您已通过该课程测验，无需重复作答");
        }
        int limit = course.getExamLimit() == null ? 0 : course.getExamLimit();
        if (limit > 0 && attempts.size() >= limit) {
            throw new BusinessException("已达到测验次数上限（" + limit + " 次）");
        }
        if (req.answers() == null || req.answers().isEmpty()) {
            throw new BusinessException("请作答后再提交");
        }
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId));
        if (questions.isEmpty()) {
            throw new BusinessException("该课程尚未配置测验题目");
        }
        Map<Long, String> answers = req.answers().stream()
                .filter(a -> a.questionId() != null)
                .collect(Collectors.toMap(AnswerItem::questionId, a -> a.answer() == null ? "" : a.answer(),
                        (a, b) -> a));
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal got = BigDecimal.ZERO;
        List<Map<String, Object>> details = new ArrayList<>();
        for (Question q : questions) {
            BigDecimal qs = q.getScore() == null ? BigDecimal.TEN : q.getScore();
            totalScore = totalScore.add(qs);
            String given = answers.getOrDefault(q.getId(), "");
            boolean right = sameAnswer(q.getAnswer(), given);
            if (right) {
                got = got.add(qs);
            }
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("questionId", q.getId());
            d.put("correct", right);
            d.put("answer", q.getAnswer());
            d.put("given", given);
            details.add(d);
        }
        BigDecimal score = questions.isEmpty() ? BigDecimal.ZERO
                : got.multiply(BigDecimal.valueOf(100)).divide(totalScore, 2, java.math.RoundingMode.HALF_UP);
        int passScore = course.getPassScore() == null ? 60 : course.getPassScore();
        boolean pass = score.compareTo(BigDecimal.valueOf(passScore)) >= 0;
        ExamRecord record = new ExamRecord();
        record.setUserId(current.getId());
        record.setCourseId(courseId);
        record.setProjectId(course.getProjectId());
        record.setScore(score);
        record.setIsPass(pass ? 1 : 0);
        record.setAnswerContent(toJson(req.answers()));
        record.setAttemptNo(attempts.size() + 1);
        record.setSubmitTime(LocalDateTime.now());
        examMapper.insert(record);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("score", score);
        m.put("passScore", passScore);
        m.put("passed", pass);
        m.put("attemptNo", record.getAttemptNo());
        m.put("remainingAttempts", limit == 0 ? -1 : Math.max(0, limit - record.getAttemptNo()));
        m.put("details", details);
        if (pass) {
            creditService.awardCourseCredit(current.getId(), course.getProjectId(), courseId,
                    course.getCredit(), "完成课程《" + course.getCourseName() + "》学习及测验自动发放");
            studyService.updateMemberCompletion(study);
            notificationService.send(current.getId(), "课程测验通过",
                    "《" + course.getCourseName() + "》测验得分 " + score + "，课程学分 " + course.getCredit() + " 已发放。",
                    "COURSE", courseId);
            m.put("creditAwarded", true);
            m.put("creditValue", course.getCredit());
        }
        return m;
    }

    private boolean sameAnswer(String correct, String given) {
        if (StrUtil.isBlank(correct) || StrUtil.isBlank(given)) {
            return false;
        }
        Set<String> c = Arrays.stream(correct.toUpperCase().split("[,，\\s]+"))
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        Set<String> g = Arrays.stream(given.toUpperCase().split("[,，\\s]+"))
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        return !c.isEmpty() && c.equals(g);
    }

    private String toJson(List<AnswerItem> answers) {
        return answers.stream().map(a -> "{\"questionId\":" + a.questionId()
                        + ",\"answer\":\"" + (a.answer() == null ? "" : a.answer().replace("\"", "\\\"")) + "\"}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    public PageResult<Map<String, Object>> recordPage(LoginUser current, long pageNum, long pageSize,
                                                      Long courseId, Long projectId, Long userId, Integer isPass) {
        LambdaQueryWrapper<ExamRecord> qw = new LambdaQueryWrapper<>();
        if (courseId != null) {
            qw.eq(ExamRecord::getCourseId, courseId);
        }
        if (projectId != null) {
            qw.eq(ExamRecord::getProjectId, projectId);
        }
        if (userId != null) {
            qw.eq(ExamRecord::getUserId, userId);
        }
        if (isPass != null) {
            qw.eq(ExamRecord::getIsPass, isPass);
        }
        List<Long> visible = creditService.visibleUserIds(current);
        if (visible != null) {
            if (visible.isEmpty()) {
                return PageResult.of(List.of(), 0, pageNum, pageSize);
            }
            qw.in(ExamRecord::getUserId, visible);
        }
        if ("teacher".equals(current.getRoleCode()) && !current.isSuperAdmin()) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .eq(Course::getCreateUserId, current.getId()).select(Course::getId))
                    .stream().map(Course::getId).toList();
            if (courseIds.isEmpty()) {
                return PageResult.of(List.of(), 0, pageNum, pageSize);
            }
            qw.in(ExamRecord::getCourseId, courseIds);
        }
        qw.orderByDesc(ExamRecord::getSubmitTime);
        Page<ExamRecord> page = examMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<ExamRecord> records = page.getRecords();
        List<Long> userIds = records.stream().map(ExamRecord::getUserId).distinct().toList();
        List<Long> courseIds = records.stream().map(ExamRecord::getCourseId).distinct().toList();
        List<Long> projectIds = records.stream().map(ExamRecord::getProjectId).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        Map<Long, String> courses = courseIds.isEmpty() ? Map.of() : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getCourseName, (a, b) -> a));
        Map<Long, String> projects = projectIds.isEmpty() ? Map.of() : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(TrainingProject::getId, TrainingProject::getProjectName, (a, b) -> a));
        List<Map<String, Object>> list = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            SysUser u = users.get(r.getUserId());
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            m.put("userName", u == null ? null : u.getRealName());
            m.put("username", u == null ? null : u.getUsername());
            m.put("courseId", r.getCourseId());
            m.put("courseName", courses.get(r.getCourseId()));
            m.put("projectId", r.getProjectId());
            m.put("projectName", projects.get(r.getProjectId()));
            m.put("score", r.getScore());
            m.put("isPass", r.getIsPass());
            m.put("attemptNo", r.getAttemptNo());
            m.put("submitTime", r.getSubmitTime());
            return m;
        }).toList();
        return PageResult.of(list, page.getTotal(), pageNum, pageSize);
    }

    public List<Map<String, Object>> myRecords(LoginUser current, Long courseId) {
        LambdaQueryWrapper<ExamRecord> qw = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, current.getId());
        if (courseId != null) {
            qw.eq(ExamRecord::getCourseId, courseId);
        }
        qw.orderByDesc(ExamRecord::getSubmitTime);
        return examMapper.selectList(qw).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("courseId", r.getCourseId());
            m.put("score", r.getScore());
            m.put("isPass", r.getIsPass());
            m.put("attemptNo", r.getAttemptNo());
            m.put("submitTime", r.getSubmitTime());
            return m;
        }).toList();
    }
}
