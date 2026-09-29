package com.schoolmate.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * WebSocket 会话注册表：维护「用户ID → 该用户的在线连接集合」。
 *
 * <p>这是整个实时推送的基础设施，同时充当在线状态的唯一数据来源。
 *
 * <p><b>为什么一个用户要对应多个连接：</b>同一个人可能同时开着电脑浏览器和手机，
 * 两个连接都应该收到消息，所以用 Set 而不是单个 Session。
 *
 * <p><b>为什么用 ConcurrentWebSocketSessionDecorator 包装：</b>原生 WebSocketSession
 * 不是线程安全的，同一个连接如果被多个线程同时写（比如群聊扇出和心跳响应并发），
 * 会抛 IllegalStateException 导致连接被关闭。装饰器内部加了锁并与发送缓冲上限。
 *
 * <p><b>单机限制：</b>本实现把在线状态放在 JVM 内存里，因此只支持单节点部署。
 * 若未来需要集群，把 {@link #pushToUser} 改为「发 Redis Pub/Sub，各节点收到后推给本地连接」
 * 即可，调用方无需改动。
 *
 * @author Albot
 */
@Slf4j
@Component
public class WsSessionRegistry {

    /** 发送超时（毫秒）：单条消息写入超过该时长视为连接异常 */
    private static final int SEND_TIME_LIMIT_MS = 5000;

    /** 发送缓冲上限（字节）：客户端读得慢时，缓冲超过该值就关闭连接，防止内存被拖垮 */
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public WsSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 注册一条新连接。
     */
    public void add(Long userId, WebSocketSession rawSession) {
        WebSocketSession session = new ConcurrentWebSocketSessionDecorator(
            rawSession, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WebSocket 已连接：userId={}，sessionId={}，该用户在线连接数={}",
            userId, rawSession.getId(), userSessions.get(userId).size());
    }

    /**
     * 移除连接（连接关闭或推送失败时调用）。
     */
    public void remove(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        Set<WebSocketSession> set = userSessions.get(userId);
        if (set == null) {
            return;
        }
        // 注册表里保存的是包装后的 session，而容器回调传入的是原始 session，
        // 两者引用不同，因此按 sessionId 匹配移除
        set.removeIf(s -> session.getId().equals(s.getId()));
        if (set.isEmpty()) {
            userSessions.remove(userId, set);
            log.debug("WebSocket 已断开：userId={}，该用户已全部离线", userId);
        }
    }

    /** 用户是否在线 */
    public boolean isOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        Set<WebSocketSession> set = userSessions.get(userId);
        return set != null && !set.isEmpty();
    }

    /** 当前在线用户ID集合 */
    public Set<Long> onlineUserIds() {
        return Collections.unmodifiableSet(userSessions.keySet());
    }

    public int onlineCount() {
        return userSessions.size();
    }

    /**
     * 向指定用户的所有在线连接推送一帧。
     *
     * <p><b>关键优化：</b>先序列化一次，再遍历所有连接发送。
     * 如果在循环里逐条序列化，群聊扇出时会重复做 N 次 JSON 序列化。
     */
    public void pushToUser(Long userId, WsFrame frame) {
        Set<WebSocketSession> set = userSessions.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        String json = toJson(frame);
        if (json == null) {
            return;
        }
        for (WebSocketSession session : set) {
            if (!sendQuietly(session, json)) {
                remove(userId, session);
            }
        }
    }

    /**
     * 批量推送（群聊扇出场景）。
     *
     * @param userIds 目标用户
     * @param frame   帧
     * @param filter  返回 true 表示该用户需要接收，可用于排除自己或免打扰用户
     */
    public void pushToUsers(Collection<Long> userIds, WsFrame frame, java.util.function.Predicate<Long> filter) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        String json = toJson(frame);
        if (json == null) {
            return;
        }
        for (Long userId : userIds) {
            if (filter != null && !filter.test(userId)) {
                continue;
            }
            Set<WebSocketSession> set = userSessions.get(userId);
            if (set == null || set.isEmpty()) {
                continue;
            }
            for (WebSocketSession session : set) {
                if (!sendQuietly(session, json)) {
                    remove(userId, session);
                }
            }
        }
    }

    /**
     * 遍历所有在线连接，按 (userId, session) 回调。
     * 供「广播给所有在线用户」等场景使用。
     */
    public void forEachSession(BiConsumer<Long, WebSocketSession> action) {
        userSessions.forEach((userId, set) -> set.forEach(session -> action.accept(userId, session)));
    }

    /**
     * 静默发送：任何异常都只记录日志，不向上抛出。
     *
     * <p>这一点很重要——群聊扇出时不能因为某一个人网络异常就打断整个循环，
     * 更不能让异常影响到发送者的主流程。
     *
     * @return true 表示发送成功，false 表示该连接已失效、应当剔除
     */
    private boolean sendQuietly(WebSocketSession session, String json) {
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            session.sendMessage(new TextMessage(json));
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 推送失败，将剔除该连接：sessionId={}，原因={}", session.getId(), e.getMessage());
            return false;
        }
    }

    private String toJson(WsFrame frame) {
        try {
            return objectMapper.writeValueAsString(frame);
        } catch (Exception e) {
            log.error("WebSocket 帧序列化失败：type={}", frame == null ? null : frame.getType(), e);
            return null;
        }
    }
}
