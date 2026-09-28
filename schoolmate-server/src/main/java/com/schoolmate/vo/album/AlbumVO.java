package com.schoolmate.vo.album;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相册出参。
 *
 * @author Albot
 */
@Data
public class AlbumVO {

    @Schema(description = "相册ID")
    private Long id;

    @Schema(description = "班级ID")
    private Long classId;

    @Schema(description = "创建人ID")
    private Long userId;

    @Schema(description = "相册名称")
    private String name;

    @Schema(description = "封面URL")
    private String coverUrl;

    @Schema(description = "相册描述")
    private String description;

    @Schema(description = "照片数量")
    private Long photoCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
