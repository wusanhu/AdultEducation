package com.adultedu.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("verify_gear")
public class VerifyGear {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer gearCode;
    private String gearName;
    private String triggerPoints;
    private Integer intervalMinute;
    private Integer timeoutSecond;
    private Integer maxFailCount;
    private Integer failStrategy;
    /** 是否真人活体核验 0否 1是 */
    private Integer liveCheck;
    /** 核验方式 1人脸核验 2学习验证（弹窗确认） */
    private Integer verifyMode;
    private Integer builtIn;
    private Integer status;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
