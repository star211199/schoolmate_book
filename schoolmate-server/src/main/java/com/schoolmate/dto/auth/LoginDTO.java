package com.schoolmate.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录入参。
 *
 * @author Albot
 */
@Data
public class LoginDTO {

    @Schema(description = "登录账号", example = "admin")
    @NotBlank(message = "账号不能为空")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
