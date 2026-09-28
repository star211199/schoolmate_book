package com.schoolmate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态点赞实体。
 *
 * @author Albot
 */
@Data
@TableName("moment_like")
public class MomentLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 动态 ID */
    private Long momentId;

    /** 点赞用户 ID */
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
