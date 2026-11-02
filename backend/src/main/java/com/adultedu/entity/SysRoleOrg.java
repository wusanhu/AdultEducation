package com.adultedu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role_org")
public class SysRoleOrg {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long orgId;
    private LocalDateTime createTime;
}
