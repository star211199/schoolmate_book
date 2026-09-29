package com.schoolmate.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmate.common.ResultCode;
import com.schoolmate.dto.chat.MessageSendDTO;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.service.ChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天 WebSocket 处理器：所有实时消息的收发入口。
 *
 * <p>选用 Spring 原生 TextWebSocketHandler 而非 STOMP / {@code @ServerEndpoint} 的原因：
 * <ul>
 *   <li>{@code @ServerEndpoint} 的实例由容器管理，无法注入 Spring Bean，只能靠静态变量兜底</li>
 *   <li>STOMP 概念较多（broker、destination），排查问题链路长</li>
 *   <li>原生 Handler 能注入 Bean、握手可鉴权、会话管理完全可控，也便于讲清原理</li>
 * </ul>
 *
 * <p>说明：真正的业务逻辑都在 {@link ChatService} 里，本类只负责「解析帧 → 派发 → 异常兜底」。
 *
 * @author Albot
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Resource
    private WsSessionRegistry sessionRegistry;

    @Resource
    private ChatService chatService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = currentUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未认证"));
            return;
        }
        sessionRegistry.add(userId, session);

        Map<String, Object> payload = new HashMap<>(2);
        payload.put("userId", userId);
        payload.put("onlineCount", sessionRegistry.onlineCount());
        sessionRegistry.pushToUser(userId, WsFrame.of(WsFrameType.S2C_CONNECTED, payload));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            pushError(userId, "消息格式不正确");
            return;
        }

        String type = root.path("type").asText("");
        JsonNode data = root.path("data");
        try {
            switch (type) {
                case WsFrameType.C2S_CHAT_SEND -> handleChatSend(userId, data);
                case WsFrameType.C2S_READ_ACK -> handleReadAck(userId, data);
                case WsFrameType.C2S_RECALL -> handleRecall(userId, data);
                case WsFrameType.C2S_TYPING -> handleTyping(userId, data);
                case WsFrameType.C2S_PING -> sessionRegistry.pushToUser(userId,
                    WsFrame.of(WsFrameType.S2C_PONG));
                default -> pushError(userId, "未知的消息类型：" + type);
            }
        } catch (BusinessException e) {
            // 业务异常（未入群、被禁言等）回给客户端提示，不影响连接
            pushError(userId, e.getMessage());
        } catch (Exception e) {
            log.error("处理 WebSocket 帧失败：userId={}，type={}", userId, type, e);
            pushError(userId, "服务端处理失败，请稍后重试");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = currentUserId(session);
        if (userId != null) {
            sessionRegistry.remove(userId, session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输异常：sessionId={}，原因={}", session.getId(), exception.getMessage());
        Long userId = currentUserId(session);
        if (userId != null) {
            sessionRegistry.remove(userId, session);
        }
    }

    /* ==================== 各类型帧的处理 ==================== */

    /**
     * 发送消息。
     *
     * <p>注意这里<b>不主动推送</b>：落库、扇出 CHAT_MESSAGE、回 MESSAGE_ACK 都由
     * ChatService.sendMessage 在事务提交后完成，保证「先落库再推送」。
     */
    private void handleChatSend(Long userId, JsonNode data) throws Exception {
        MessageSendDTO dto = objectMapper.treeToValue(data, MessageSendDTO.class);
        if (dto == null || dto.getSessionId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话ID不能为空");
        }
        chatService.sendMessage(userId, dto);
    }

    private void handleReadAck(Long userId, JsonNode data) {
        long sessionId = data.path("sessionId").asLong(0L);
        long lastReadMessageId = data.path("lastReadMessageId").asLong(0L);
        if (sessionId > 0L) {
            chatService.markRead(userId, sessionId, lastReadMessageId);
        }
    }

    private void handleRecall(Long userId, JsonNode data) {
        long messageId = data.path("messageId").asLong(0L);
        if (messageId > 0L) {
            chatService.recallMessage(userId, messageId);
        }
    }

    /** 「正在输入」：轻量状态，不落库，直接转发给会话中的其他人 */
    private void handleTyping(Long userId, JsonNode data) {
        long sessionId = data.path("sessionId").asLong(0L);
        if (sessionId <= 0L) {
            return;
        }
        List<Long> memberIds = chatService.listSessionMemberIds(sessionId);
        Map<String, Object> payload = new HashMap<>(2);
        payload.put("sessionId", sessionId);
        payload.put("userId", userId);
        sessionRegistry.pushToUsers(memberIds, WsFrame.of(WsFrameType.C2S_TYPING, payload),
            id -> !id.equals(userId));
    }

    private void pushError(Long userId, String message) {
        Map<String, Object> payload = new HashMap<>(1);
        payload.put("message", message);
        sessionRegistry.pushToUser(userId, WsFrame.of(WsFrameType.S2C_ERROR, payload));
    }

    private Long currentUserId(WebSocketSession session) {
        Object value = session.getAttributes().get(WsHandshakeInterceptor.ATTR_USER_ID);
        return value instanceof Long userId ? userId : null;
    }
}
