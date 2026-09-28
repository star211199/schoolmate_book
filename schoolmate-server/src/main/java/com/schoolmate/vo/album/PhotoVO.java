package com.schoolmate.vo.album;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 照片出参。
 *
 * @author Albot
 */
@Data
public class PhotoVO {

    @Schema(description = "照片ID")
    private Long id;

    @Schema(description = "相册ID")
    private Long albumId;

    @Schema(description = "上传者ID")
    private Long userId;

    @Schema(description = "上传者昵称")
    private String nickname;

    @Schema(description = "图片URL")
    private String url;

    @Schema(description = "图片描述")
    private String description;

    @Schema(description = "审核状态")
    private String auditStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
