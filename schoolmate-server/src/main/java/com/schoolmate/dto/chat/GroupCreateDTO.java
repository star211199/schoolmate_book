package com.schoolmate.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建群聊入参。
 *
 * @author Albot
 */
@Data
public class GroupCreateDTO {

    @Schema(description = "群名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "群名称不能为空")
    @Size(max = 100, message = "群名称不能超过 100 字")
    private String groupName;

    @Schema(description = "初始成员用户ID列表（不含自己，自己自动成为群主）")
    private List<Long> memberIds;
}
