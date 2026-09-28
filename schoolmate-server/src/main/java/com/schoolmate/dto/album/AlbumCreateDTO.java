package com.schoolmate.dto.album;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建相册入参。
 *
 * @author Albot
 */
@Data
public class AlbumCreateDTO {

    @Schema(description = "相册名称")
    @NotBlank(message = "相册名称不能为空")
    @Size(max = 100, message = "相册名称长度不能超过 100")
    private String name;

    @Schema(description = "相册描述")
    private String description;

    @Schema(description = "封面URL（可选，上传照片后自动回填）")
    private String coverUrl;
}
