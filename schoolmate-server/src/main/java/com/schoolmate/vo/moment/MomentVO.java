package com.schoolmate.vo.moment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 班级动态出参（时间轴数据源）。
 *
 * @author Albot
 */
@Data
public class MomentVO {

    @Schema(description = "动态ID")
    private Long id;

    @Schema(description = "班级ID")
    private Long classId;

    @Schema(description = "发布人ID")
    private Long userId;

    @Schema(description = "发布人昵称")
    private String nickname;

    @Schema(description = "发布人头像")
    private String avatar;

    @Schema(description = "动态内容")
    private String content;

    @Schema(description = "图片URL列表")
    private List<String> imageUrls;

    @Schema(description = "评论数量")
    private Long commentCount;

    @Schema(description = "审核状态")
    private String auditStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
