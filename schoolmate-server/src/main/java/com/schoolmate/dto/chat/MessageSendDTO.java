package com.schoolmate.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送消息入参（WebSocket CHAT_SEND 帧的 data 部分）。
 *
 * <p>跨字段校验（文本消息必须有内容）由 Service 层负责，不在 DTO 上堆砌注解。
 *
 * @author Albot
 */
@Data
public class MessageSendDTO {

    @Schema(description = "会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @Schema(description = "消息类型 TEXT/IMAGE/FILE", example = "TEXT")
    private String msgType;

    @Schema(description = "文本内容")
    @Size(max = 2000, message = "消息内容不能超过 2000 字")
    private String content;

    @Schema(description = "扩展信息，JSON 字符串（图片URL等）")
    @Size(max = 1000, message = "扩展信息过长")
    private String extra;

    @Schema(description = "引用的消息ID")
    private Long replyToId;

    @Schema(description = "前端生成的幂等ID，用于防止重复发送与匹配发送回执")
    @Size(max = 64, message = "幂等ID过长")
    private String clientMsgId;
}
