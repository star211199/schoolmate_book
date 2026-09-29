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
 * 聊天消息实体（私聊与群聊共用一张表）。
 *
 * <p>设计要点：
 * <ul>
 *   <li>不存 receiverId —— 私聊的接收方可由 session 推出，群聊的接收方是成员列表</li>
 *   <li>主键自增且单调递增，直接作为「已读位点」的比较依据</li>
 *   <li>clientMsgId 用于幂等去重，防止网络抖动导致重复发送</li>
 *   <li>撤回不删记录，改为把 msgType 置为 RECALL，避免破坏已读位点的连续性</li>
 * </ul>
 *
 * @author Albot
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private Long sessionId;

    /** 发送者ID */
    private Long senderId;

    /** 消息类型 TEXT / IMAGE / FILE / SYSTEM / RECALL */
    private String msgType;

    /** 文本内容（落库前已完成 XSS 转义） */
    private String content;

    /** 扩展信息 JSON：图片URL、文件名、被@的 userId 列表 */
    private String extra;

    /** 引用的消息ID */
    private Long replyToId;

    /** 前端生成的幂等ID */
    private String clientMsgId;

    /** 发送时间 */
    private LocalDateTime sendTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
