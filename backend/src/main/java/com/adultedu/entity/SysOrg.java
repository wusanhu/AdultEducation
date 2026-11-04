package com.adultedu.entity;

import com.adultedu.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
public class SysOrg extends BaseEntity {
    private Long parentId;
    private String orgName;
    private Integer orgType;
    private String orgCode;
    private Long leaderUserId;
    private Integer sort;
    private Integer status;
}
