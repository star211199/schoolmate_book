package com.schoolmate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友申请实体。
 *
 * <p>状态流转：PENDING → ACCEPTED / REJECTED，长期未处理可由定时任务置为 EXPIRED。
 *
 * @author Albot
 */
@Data
@TableName("friend_request")
public class FriendRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人ID */
    private Long fromUserId;

    /** 被申请人ID */
    private Long toUserId;

    /** 验证消息 */
    private String message;

    /** 状态 PENDING / ACCEPTED / REJECTED / EXPIRED */
    private String status;

    /** 处理时间 */
    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
