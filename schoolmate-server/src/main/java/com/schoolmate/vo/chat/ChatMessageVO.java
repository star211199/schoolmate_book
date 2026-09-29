package com.schoolmate.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息 VO。
 *
 * <p>同时用于 REST 拉历史记录与 WebSocket 推送，保证前端两种来源的结构完全一致。
 *
 * @author Albot
 */
@Data
public class ChatMessageVO {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "发送者ID")
    private Long senderId;

    @Schema(description = "发送者昵称")
    private String senderNickname;

    @Schema(description = "发送者头像")
    private String senderAvatar;

    @Schema(description = "群聊时展示的发送者名称：优先群昵称，其次昵称")
    private String senderDisplayName;

    @Schema(description = "消息类型 TEXT/IMAGE/FILE/SYSTEM/RECALL")
    private String msgType;

    @Schema(description = "文本内容")
    private String content;

    @Schema(description = "扩展信息：图片URL列表等")
    private List<String> extraImages;

    @Schema(description = "原始扩展JSON，前端按需解析")
    private String extra;

    @Schema(description = "引用的消息ID")
    private Long replyToId;

    @Schema(description = "引用的消息摘要")
    private String replyToPreview;

    @Schema(description = "前端幂等ID，用于把本地乐观插入的气泡与回执对应起来")
    private String clientMsgId;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;
}
