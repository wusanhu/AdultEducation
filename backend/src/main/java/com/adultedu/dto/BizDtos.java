package com.adultedu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BizDtos {
    private BizDtos() {
    }

    public record ProjectSaveRequest(
            Long id,
            @NotBlank(message = "请填写项目名称") String projectName,
            String projectCode,
            String description,
            @NotNull(message = "请选择开始时间") LocalDateTime startTime,
            @NotNull(message = "请选择结束时间") LocalDateTime endTime,
            @NotNull(message = "请填写所需总学分") @DecimalMin(value = "0.01", message = "所需学分必须大于 0") BigDecimal needCredit,
            Integer status,
            String certificateTemplateUrl,
            Integer allowOverdue,
            Integer autoGraduate,
            Integer needTeacherAudit,
            Integer needLeaderAudit,
            Integer needAdminAudit
    ) {
    }

    public record MemberRequest(List<Long> userIds) {
    }

    public record ProjectStatusRequest(LocalDateTime startTime, LocalDateTime endTime) {
    }

    public record AttachmentDto(String fileName, String fileUrl, String fileType, Long fileSize) {
    }

    public record QuestionDto(Long id, Integer questionType, String content, String optionsJson,
                              String answer, BigDecimal score, Integer sort) {
    }

    public record CourseSaveRequest(
            Long id,
            @NotNull(message = "请选择培训班") Long projectId,
            @NotBlank(message = "请填写课程名称") String courseName,
            String cover,
            @NotNull(message = "请填写课程学分") @DecimalMin(value = "0.01", message = "课程学分必须大于 0") BigDecimal credit,
            @NotNull(message = "请填写课程学时") @DecimalMin(value = "0.01", message = "课程学时必须大于 0") BigDecimal studyHour,
            Integer isRequired,
            Integer needExam,
            Integer passScore,
            Integer examLimit,
            String videoUrl,
            Integer videoDuration,
            String intro,
            Integer status,
            Integer verifyGearCode,
            String verifyGearJson,
            Integer allowSeek,
            Integer sort,
            List<AttachmentDto> attachments,
            List<QuestionDto> questions
    ) {
    }

    public record VerifyGearSaveRequest(
            Long id,
            @NotNull(message = "请填写档位编码") Integer gearCode,
            @NotBlank(message = "请填写档位名称") String gearName,
            String triggerPoints,
            Integer intervalMinute,
            Integer timeoutSecond,
            Integer maxFailCount,
            Integer failStrategy,
            Integer status,
            String description
    ) {
    }

    public record StudyStartRequest(@NotNull(message = "课程不能为空") Long courseId) {
    }

    public record HeartbeatRequest(
            @NotNull Long courseId,
            @NotBlank String sessionId,
            Integer deltaSeconds,
            BigDecimal progress,
            Integer position,
            Integer inForeground,
            Integer screenSwitch,
            Integer screenSwitchDuration
    ) {
    }

    public record PopupConfirmRequest(@NotNull Long courseId, @NotBlank String sessionId) {
    }

    public record VerifyStartRequest(@NotNull Long courseId, @NotBlank String triggerPoint) {
    }

    public record VerifySubmitRequest(@NotNull(message = "核验记录不能为空") Long logId, String imageBase64) {
    }

    public record SmsVerifySendRequest(@NotNull(message = "核验记录不能为空") Long logId) {
    }

    public record SmsVerifySubmitRequest(@NotNull Long logId, @NotBlank String code) {
    }

    public record VerifyReviewRequest(@NotBlank String action, String remark) {
    }

    public record CreditApplyRequest(
            @NotNull(message = "请选择学员") Long userId,
            @NotNull(message = "请选择培训班") Long projectId,
            @NotNull(message = "请填写补录学分") @DecimalMin(value = "0.01", message = "补录学分必须大于 0") BigDecimal creditValue,
            @NotBlank(message = "请填写补录原因") String remark
    ) {
    }

    public record CreditAuditRequest(@NotNull Boolean approve, String remark) {
    }

    public record CreditRevokeRequest(@NotBlank(message = "请填写撤销原因") String reason) {
    }

    public record GraduateApplyRequest(@NotNull(message = "请选择培训班") Long projectId) {
    }

    public record AuditRequest(@NotBlank(message = "请选择审批操作") String action, String opinion) {
    }

    public record ExamSubmitRequest(List<AnswerItem> answers) {
    }

    public record AnswerItem(Long questionId, String answer) {
    }

    public record ProfileUpdateRequest(String phone, String avatar, String realName) {
    }
}
