package com.schoolmate.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息出参（对手机号做脱敏处理，不返回密码等敏感字段）。
 *
 * @author Albot
 */
@Data
public class UserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "手机号（脱敏，如 138****8888）")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
