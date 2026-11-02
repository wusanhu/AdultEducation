package com.adultedu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("study_heartbeat_log")
public class StudyHeartbeatLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studyRecordId;
    private Long userId;
    private Long courseId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportTime;
    private Integer deltaSeconds;
    private BigDecimal progress;
    private Integer inForeground;
    private Integer screenSwitch;
    private String ip;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
