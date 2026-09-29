package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.chat.MessageSendDTO;
import com.schoolmate.service.ChatService;
import com.schoolmate.vo.chat.ChatMessageVO;
import com.schoolmate.vo.chat.ChatSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话与消息控制器。
 *
 * <p>职责划分：<b>拉取数据（会话列表、历史记录）走这里，实时收发走 WebSocket</b>。
 * 这样即使 WebSocket 断开，页面依然能正常浏览历史消息。
 *
 * @author Albot
 */
@Tag(name = "会话与消息", description = "会话列表、聊天记录、已读上报")
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Operation(summary = "我的会话列表（含未读数与最后一条消息摘要，按置顶+时间排序）")
    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> listSessions() {
        return Result.success(chatService.listSessions(UserContext.getUserId()));
    }

    @Operation(summary = "会话详情")
    @GetMapping("/sessions/{sessionId}")
    public Result<ChatSessionVO> getSession(@PathVariable Long sessionId) {
        return Result.success(chatService.getSession(UserContext.getUserId(), sessionId));
    }

    @Operation(summary = "获取或创建与某用户的私聊会话，返回会话ID")
    @PostMapping("/sessions/private/{targetUserId}")
    public Result<Long> getOrCreatePrivateSession(@PathVariable Long targetUserId) {
        return Result.success(chatService.getOrCreatePrivateSession(UserContext.getUserId(), targetUserId));
    }

    @Operation(summary = "分页拉取历史消息（向上翻页，beforeId 为空则取最新一页，返回按时间正序）")
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessageVO>> pageMessages(@PathVariable Long sessionId,
                                                    @RequestParam(required = false) Long beforeId,
                                                    @RequestParam(defaultValue = "30") Long size) {
        return Result.success(chatService.pageMessages(UserContext.getUserId(), sessionId, beforeId, size));
    }

    @Operation(summary = "增量拉取消息（断线重连后补齐断线期间的消息）")
    @GetMapping("/sessions/{sessionId}/messages/after")
    public Result<List<ChatMessageVO>> listMessagesAfter(@PathVariable Long sessionId,
                                                         @RequestParam(required = false) Long afterId) {
        return Result.success(chatService.listMessagesAfter(UserContext.getUserId(), sessionId, afterId));
    }

    @Operation(summary = "发送消息（WebSocket 不可用时的 REST 兜底通道）")
    @PostMapping("/messages")
    public Result<ChatMessageVO> sendMessage(@Valid @RequestBody MessageSendDTO dto) {
        return Result.success(chatService.sendMessage(UserContext.getUserId(), dto));
    }

    @Operation(summary = "上报已读位点（WebSocket 断线时可用此接口兜底）")
    @PutMapping("/sessions/{sessionId}/read")
    public Result<Void> markRead(@PathVariable Long sessionId, @RequestParam Long lastReadMessageId) {
        chatService.markRead(UserContext.getUserId(), sessionId, lastReadMessageId);
        return Result.success();
    }

    @Operation(summary = "撤回消息（仅发送者本人，限 2 分钟内）")
    @PutMapping("/messages/{messageId}/recall")
    public Result<ChatMessageVO> recall(@PathVariable Long messageId) {
        return Result.success(chatService.recallMessage(UserContext.getUserId(), messageId));
    }
}
