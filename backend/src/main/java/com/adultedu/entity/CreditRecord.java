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
@TableName("credit_record")
public class CreditRecord extends BaseEntity {
    private Long userId;
    private Long projectId;
    private Long courseId;
    private BigDecimal creditValue;
    private Integer sourceType;
    private Integer auditStatus;
    private Long auditUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    private String auditRemark;
    private String auditNode;
    private Long applyUserId;
    private String remark;
    private String revokeReason;
    private Long revokeUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime revokeTime;
}
