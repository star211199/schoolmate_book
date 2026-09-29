package com.schoolmate.dto.friend;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发起好友申请入参。
 *
 * @author Albot
 */
@Data
public class FriendRequestCreateDTO {

    @Schema(description = "被申请人用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "被申请人不能为空")
    private Long toUserId;

    @Schema(description = "验证消息", example = "我是同班同学张三")
    @Size(max = 200, message = "验证消息不能超过 200 字")
    private String message;
}
