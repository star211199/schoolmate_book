package com.schoolmate.dto.friend;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设置好友备注 / 分组入参。
 *
 * @author Albot
 */
@Data
public class FriendUpdateDTO {

    @Schema(description = "备注名，传空字符串表示清除备注")
    @Size(max = 50, message = "备注名不能超过 50 字")
    private String remark;

    @Schema(description = "好友分组名")
    @Size(max = 50, message = "分组名不能超过 50 字")
    private String groupName;
}
