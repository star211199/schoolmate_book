package com.schoolmate.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 找回密码（发送验证码）入参。
 *
 * @author Albot
 */
@Schema(description = "找回密码入参：发送邮箱验证码")
@Data
public class ForgotPasswordDTO {

    @Schema(description = "登录账号", example = "zhangsan")
    @NotBlank(message = "账号不能为空")
    private String username;

    @Schema(description = "注册时绑定的邮箱", example = "zhangsan@qq.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
