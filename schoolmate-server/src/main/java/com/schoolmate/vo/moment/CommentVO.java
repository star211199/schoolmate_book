package com.schoolmate.vo.moment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论出参。
 *
 * @author Albot
 */
@Data
public class CommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "动态ID")
    private Long momentId;

    @Schema(description = "评论人ID")
    private Long userId;

    @Schema(description = "评论人昵称")
    private String nickname;

    @Schema(description = "评论人头像")
    private String avatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "父评论ID")
    private Long parentId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
