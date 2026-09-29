package com.schoolmate.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量拉人入群入参。
 *
 * @author Albot
 */
@Data
public class GroupMemberAddDTO {

    @Schema(description = "要加入的用户ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少选择一位成员")
    private List<Long> userIds;
}
