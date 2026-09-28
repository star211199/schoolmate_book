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
 * 班级留言实体。
 *
 * @author Albot
 */
@Data
@TableName("`message`")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级ID */
    private Long classId;

    /** 留言人ID */
    private Long userId;

    /** 留言内容 */
    private String content;

    /** 审核状态 PENDING / PASSED / REJECTED */
    private String auditStatus;

    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
