package com.schoolmate.vo.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 留言出参。
 *
 * @author Albot
 */
@Data
public class MessageVO {

    @Schema(description = "留言ID")
    private Long id;

    @Schema(description = "班级ID")
    private Long classId;

    @Schema(description = "留言人ID")
    private Long userId;

    @Schema(description = "留言人昵称")
    private String nickname;

    @Schema(description = "留言人头像")
    private String avatar;

    @Schema(description = "留言内容")
    private String content;

    @Schema(description = "审核状态")
    private String auditStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
