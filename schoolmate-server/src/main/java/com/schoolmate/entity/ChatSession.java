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
 * 会话实体（私聊与群聊统一）。
 *
 * <p>bizKey 是防重复创建的关键：
 * <ul>
 *   <li>私聊：{@code P_{小userId}_{大userId}}，保证两人之间只有一个会话</li>
 *   <li>群聊：{@code G_{groupId}}</li>
 * </ul>
 *
 * @author Albot
 */
@Data
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一键，配合唯一索引防止并发重复建会话 */
    private String bizKey;

    /** 会话类型 PRIVATE / GROUP */
    private String sessionType;

    /** 群聊时的群ID */
    private Long groupId;

    /** 私聊时的对方用户ID（从当前用户视角看） */
    private Long targetUserId;

    /** 最后一条消息ID */
    private Long lastMessageId;

    /** 最后一条消息时间，会话列表按此倒序 */
    private LocalDateTime lastMessageTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
