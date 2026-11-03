package com.adultedu.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class QuestionImportRow {
    @ExcelProperty("题目类型")
    private String questionType;
    @ExcelProperty("题目内容")
    private String content;
    @ExcelProperty("选项A")
    private String optionA;
    @ExcelProperty("选项B")
    private String optionB;
    @ExcelProperty("选项C")
    private String optionC;
    @ExcelProperty("选项D")
    private String optionD;
    @ExcelProperty("选项E")
    private String optionE;
    @ExcelProperty("选项F")
    private String optionF;
    @ExcelProperty("正确答案")
    private String answer;
    @ExcelProperty("分值")
    private String score;
}
