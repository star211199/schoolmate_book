package com.schoolmate.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册入参。
 *
 * @author Albot
 */
@Data
public class RegisterDTO {

    @Schema(description = "登录账号", example = "zhangsan")
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 50, message = "账号长度需在 3-50 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6-32 之间")
    private String password;

    @Schema(description = "昵称", example = "张三")
    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    @Schema(description = "班级邀请码（可选，填写后自动加入班级）", example = "CLASS01")
    private String inviteCode;
}
