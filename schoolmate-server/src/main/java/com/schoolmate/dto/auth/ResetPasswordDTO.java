package com.schoolmate.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 凭邮箱验证码重置密码入参。
 *
 * @author Albot
 */
@Schema(description = "凭邮箱验证码重置密码入参")
@Data
public class ResetPasswordDTO {

    @Schema(description = "登录账号", example = "zhangsan")
    @NotBlank(message = "账号不能为空")
    private String username;

    @Schema(description = "注册时绑定的邮箱", example = "zhangsan@qq.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "邮箱收到的 6 位验证码", example = "483920")
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为 6 位数字")
    private String code;

    @Schema(description = "新密码（6-32 位）", example = "newpass123")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String newPassword;
}
