package com.schoolmate.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录结果出参（Token + 用户基本信息）。
 *
 * @author Albot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "Token 类型")
    private String tokenType = "Bearer";

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "角色 USER / ADMIN")
    private String role;
}
