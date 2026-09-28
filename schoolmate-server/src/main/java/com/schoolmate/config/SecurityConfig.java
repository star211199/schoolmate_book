package com.schoolmate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全相关配置：仅注入密码加密器，不启用 Spring Security 过滤器链。
 *
 * @author Albot
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt 密码加密器（强度 10）。
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
