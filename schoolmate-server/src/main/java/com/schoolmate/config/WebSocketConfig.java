package com.schoolmate.config;

import com.schoolmate.ws.ChatWebSocketHandler;
import com.schoolmate.ws.WsHandshakeInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置。
 *
 * <p>端点路径为 {@code /ws/chat}，由于 application.yml 中配置了
 * {@code server.servlet.context-path: /api}，前端实际连接地址是
 * <b>{@code ws://host:8080/api/ws/chat?token=xxx}</b>。
 *
 * @author Albot
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private ChatWebSocketHandler chatWebSocketHandler;

    @Resource
    private WsHandshakeInterceptor wsHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
            .addInterceptors(wsHandshakeInterceptor)
            // 开发期放开来源限制：前端可能通过 localhost、127.0.0.1、局域网 IP 或
            // natapp 隧道域名访问，逐一枚举不现实。生产环境应改为具体域名白名单。
            .setAllowedOriginPatterns("*");
    }
}
