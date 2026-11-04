package com.adultedu.entity;

import com.adultedu.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("training_project")
public class TrainingProject extends BaseEntity {
    private String projectName;
    private String projectCode;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private BigDecimal needCredit;
    private Integer status;
    private String certificateTemplateUrl;
    private Long createUserId;
    private Integer allowOverdue;
    private Integer autoGraduate;
    private Integer needTeacherAudit;
    private Integer needLeaderAudit;
}
