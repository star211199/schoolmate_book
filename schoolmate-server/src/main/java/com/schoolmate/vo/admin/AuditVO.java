package com.schoolmate.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待审核内容出参（留言 / 动态 / 照片统一视图）。
 *
 * @author Albot
 */
@Data
public class AuditVO {

    @Schema(description = "内容ID")
    private Long id;

    @Schema(description = "内容类型 MESSAGE / MOMENT / PHOTO")
    private String type;

    @Schema(description = "内容摘要")
    private String content;

    @Schema(description = "图片URL（照片类型）")
    private String url;

    @Schema(description = "发布人ID")
    private Long userId;

    @Schema(description = "发布人昵称")
    private String nickname;

    @Schema(description = "审核状态")
    private String auditStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
