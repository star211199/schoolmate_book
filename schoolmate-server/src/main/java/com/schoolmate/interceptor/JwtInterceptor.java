package com.schoolmate.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmate.common.Result;
import com.schoolmate.common.ResultCode;
import com.schoolmate.context.UserContext;
import com.schoolmate.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 鉴权拦截器：解析 Token 并写入 UserContext，失败返回 401。
 *
 * @author Albot
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "缺少登录凭证");
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null) {
            writeUnauthorized(response, "登录凭证无效或已过期");
            return false;
        }

        UserContext.LoginUser loginUser = new UserContext.LoginUser();
        loginUser.setUserId(jwtUtil.getUserId(claims));
        loginUser.setUsername(jwtUtil.getUsername(claims));
        loginUser.setRole(jwtUtil.getRole(claims));
        UserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 必须清理，防止线程复用导致用户信息串号
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(ResultCode.UNAUTHORIZED, message)));
    }
}
