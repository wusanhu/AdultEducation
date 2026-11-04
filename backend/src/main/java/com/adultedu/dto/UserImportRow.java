package com.adultedu.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserImportRow {
    @ExcelProperty("账号")
    private String username;
    @ExcelProperty("姓名")
    private String realName;
    @ExcelProperty("手机号")
    private String phone;
    @ExcelProperty("身份证号")
    private String idCard;
    @ExcelProperty("组织编码")
    private String orgCode;
    @ExcelProperty("部门编码")
    private String deptCode;
    @ExcelProperty("角色编码")
    private String roleCode;
    @ExcelProperty("初始密码")
    private String initialPassword;
}
