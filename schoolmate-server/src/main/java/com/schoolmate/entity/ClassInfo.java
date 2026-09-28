package com.schoolmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级实体。
 *
 * @author Albot
 */
@Data
@TableName("class_info")
public class ClassInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级名称 */
    private String className;

    /** 年级 */
    private String grade;

    /** 专业 */
    private String major;

    /** 班级简介 */
    private String description;

    /** 邀请码 */
    private String inviteCode;

    /** 创建人ID */
    private Long ownerId;

    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
