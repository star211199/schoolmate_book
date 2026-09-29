package com.schoolmate.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新群资料入参。
 *
 * @author Albot
 */
@Data
public class GroupUpdateDTO {

    @Schema(description = "群名称")
    @Size(max = 100, message = "群名称不能超过 100 字")
    private String groupName;

    @Schema(description = "群公告")
    @Size(max = 500, message = "群公告不能超过 500 字")
    private String notice;

    @Schema(description = "群头像URL")
    @Size(max = 255, message = "头像地址过长")
    private String avatar;
}
