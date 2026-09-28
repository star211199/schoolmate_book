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
 * 动态评论实体。
 *
 * @author Albot
 */
@Data
@TableName("`comment`")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 动态ID */
    private Long momentId;

    /** 评论人ID */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 父评论ID（预留二级回复） */
    private Long parentId;

    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
