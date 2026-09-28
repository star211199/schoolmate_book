package com.schoolmate.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：负责 Token 的生成与解析。
 *
 * @author Albot
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${schoolmate.jwt.secret}")
    private String secret;

    @Value("${schoolmate.jwt.expire-minutes}")
    private Long expireMinutes;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token，载荷中携带 userId / username / role。
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>(4);
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireMinutes * 60 * 1000);

        return Jwts.builder()
            .claims(claims)
            .subject(username)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(getKey())
            .compact();
    }

    /**
     * 解析 Token，失败时返回 null（由拦截器转为 401）。
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception e) {
            log.debug("Token 解析失败：{}", e.getMessage());
            return null;
        }
    }

    public Long getUserId(Claims claims) {
        return claims.get("userId", Long.class);
    }

    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
