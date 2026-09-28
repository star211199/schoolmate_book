package com.schoolmate.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布留言入参。
 *
 * @author Albot
 */
@Data
public class MessageCreateDTO {

    @Schema(description = "留言内容")
    @NotBlank(message = "留言内容不能为空")
    @Size(max = 1000, message = "留言内容不能超过 1000 字")
    private String content;
}
