package com.schoolmate.dto.moment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论入参。
 *
 * @author Albot
 */
@Data
public class CommentCreateDTO {

    @Schema(description = "评论内容")
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过 500 字")
    private String content;

    @Schema(description = "父评论ID（二级回复时填写，可空）")
    private Long parentId;
}
