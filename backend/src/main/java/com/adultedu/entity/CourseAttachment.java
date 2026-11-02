package com.adultedu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_attachment")
public class CourseAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
