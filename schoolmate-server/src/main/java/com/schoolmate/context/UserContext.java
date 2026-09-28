package com.schoolmate.context;

import lombok.Data;

/**
 * 当前登录用户上下文（线程隔离）。
 *
 * <p>由 JwtInterceptor 在鉴权通过后写入，请求结束后必须清理，避免线程复用导致串号。
 *
 * @author Albot
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> LOCAL = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        LOCAL.set(loginUser);
    }

    public static LoginUser get() {
        return LOCAL.get();
    }

    /** 当前登录用户 ID */
    public static Long getUserId() {
        LoginUser user = LOCAL.get();
        return user == null ? null : user.getUserId();
    }

    /** 当前登录用户名 */
    public static String getUsername() {
        LoginUser user = LOCAL.get();
        return user == null ? null : user.getUsername();
    }

    public static boolean isAdmin() {
        LoginUser user = LOCAL.get();
        return user != null && "ADMIN".equals(user.getRole());
    }

    public static void clear() {
        LOCAL.remove();
    }

    @Data
    public static class LoginUser {
        private Long userId;
        private String username;
        private String role;
    }
}
