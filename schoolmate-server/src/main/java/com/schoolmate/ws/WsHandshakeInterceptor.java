package com.schoolmate.ws;

import com.schoolmate.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器。
 *
 * <p><b>为什么用 URL 参数传 token：</b>浏览器原生的 {@code new WebSocket(url)} 不支持自定义请求头，
 * 因此无法像普通接口那样带 {@code Authorization}。可选方案有三种：
 * <ol>
 *   <li>URL 查询参数（本实现）—— 握手阶段就能验签，非法 token 直接拒绝、连接建立不起来</li>
 *   <li>Sec-WebSocket-Protocol 子协议头</li>
 *   <li>连上之后再发一条 AUTH 帧 —— <b>不推荐</b>，因为握手时服务端还不知道你是谁，
 *       无法拒绝非法连接，等于给了攻击者一个白嫖的连接位</li>
 * </ol>
 *
 * <p>注意：token 出现在 URL 里会进入访问日志，生产环境建议改用一次性 ticket 换取连接。
 *
 * @author Albot
 */
@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    /** 会话属性中保存用户ID的键 */
    public static final String ATTR_USER_ID = "userId";

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            log.debug("WebSocket 握手被拒绝：缺少 token，uri={}", request.getURI());
            return reject(response, "缺少 token");
        }
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null) {
            log.debug("WebSocket 握手被拒绝：token 无效或已过期");
            return reject(response, "token 无效或已过期");
        }

        Long userId = jwtUtil.getUserId(claims);
        if (userId == null) {
            log.debug("WebSocket 握手被拒绝：token 中缺少 userId");
            return reject(response, "token 中缺少 userId");
        }
        // 传给 ChatWebSocketHandler，后续所有帧都靠它识别身份
        attributes.put(ATTR_USER_ID, userId);
        attributes.put("username", jwtUtil.getUsername(claims));
        return true;
    }

    /**
     * 拒绝握手时显式回 403。
     *
     * <p>返回 false 只是让 Spring 中止握手，框架不会自动设置状态码 ——
     * 不设置的话响应仍是 200 空响应体，用 curl 探测会误以为握手成功。
     */
    private boolean reject(ServerHttpResponse response, String reason) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        try {
            response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
            response.getBody().write(("WebSocket 握手被拒绝：" + reason).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.debug("写入握手拒绝响应失败：{}", e.getMessage());
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }

    /** 优先取 URL 参数，其次兼容非浏览器客户端的 Authorization 头 */
    private String resolveToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}
