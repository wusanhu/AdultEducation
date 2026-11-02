package com.adultedu.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
@ColumnWidth(18)
public class UserExportRow {
    @ExcelProperty("账号")
    private String username;
    @ExcelProperty("姓名")
    private String realName;
    @ExcelProperty("手机号")
    private String phone;
    @ExcelProperty("身份证号")
    private String idCard;
    @ExcelProperty("所属组织")
    private String orgName;
    @ExcelProperty("所属部门")
    private String deptName;
    @ExcelProperty("角色")
    private String roleName;
    @ExcelProperty("账号类型")
    private String userTypeName;
    @ExcelProperty("状态")
    private String statusName;
    @ExcelProperty("最后登录时间")
    private String lastLoginTime;
}
