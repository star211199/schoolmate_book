package com.schoolmate.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项。
 *
 * <p>私聊与群聊共用此 VO，前端依据 sessionType 决定展示对方昵称还是群名称。
 *
 * @author Albot
 */
@Data
public class ChatSessionVO {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话类型 PRIVATE/GROUP")
    private String sessionType;

    @Schema(description = "会话业务键")
    private String bizKey;

    /** ---------- 私聊字段 ---------- */

    @Schema(description = "私聊对方用户ID")
    private Long targetUserId;

    /** ---------- 群聊字段 ---------- */

    @Schema(description = "群ID")
    private Long groupId;

    /** ---------- 展示字段（私聊取对方信息，群聊取群信息） ---------- */

    @Schema(description = "会话标题：私聊为对方备注/昵称，群聊为群名称")
    private String title;

    @Schema(description = "会话头像")
    private String avatar;

    @Schema(description = "群成员数（群聊时有值）")
    private Integer memberCount;

    @Schema(description = "对方是否在线（私聊时有值）")
    private Boolean online;

    /** ---------- 消息与状态 ---------- */

    @Schema(description = "最后一条消息ID")
    private Long lastMessageId;

    @Schema(description = "最后一条消息时间")
    private LocalDateTime lastMessageTime;

    @Schema(description = "最后一条消息摘要，用于会话列表预览")
    private String lastMessagePreview;

    @Schema(description = "未读条数")
    private Long unreadCount;

    @Schema(description = "是否置顶")
    private Boolean pinned;

    @Schema(description = "是否免打扰")
    private Boolean muted;
}
