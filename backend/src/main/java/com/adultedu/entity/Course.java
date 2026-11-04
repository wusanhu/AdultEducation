package com.adultedu.entity;

import com.adultedu.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course")
public class Course extends BaseEntity {
    private Long projectId;
    private String courseName;
    private String cover;
    private BigDecimal credit;
    private BigDecimal studyHour;
    private Integer isRequired;
    private Integer needExam;
    private Integer passScore;
    private Integer examLimit;
    private String videoUrl;
    private Integer videoDuration;
    private String intro;
    private Long createUserId;
    private Integer status;
    private Integer verifyGearCode;
    private String verifyGearJson;
    private Integer allowSeek;
    private Integer sort;
}
