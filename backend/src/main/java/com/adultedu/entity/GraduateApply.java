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
@TableName("graduate_apply")
public class GraduateApply extends BaseEntity {
    private Long userId;
    private Long projectId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;
    private String currentNode;
    private Integer auditStatus;
    private String rejectReason;
    private BigDecimal creditSnapshot;
    private BigDecimal studyHourSnapshot;
    private Long firstAuditUser;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstAuditTime;
    private String firstAuditOpinion;
    private Long secondAuditUser;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime secondAuditTime;
    private String secondAuditOpinion;
    private Long finalAuditUser;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finalAuditTime;
    private String finalAuditOpinion;
}
