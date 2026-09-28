package com.schoolmate.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台仪表盘统计数据。
 *
 * @author Albot
 */
@Data
public class StatsVO {

    @Schema(description = "用户总数")
    private Long userCount;

    @Schema(description = "班级总数")
    private Long classCount;

    @Schema(description = "留言总数")
    private Long messageCount;

    @Schema(description = "相册总数")
    private Long albumCount;

    @Schema(description = "照片总数")
    private Long photoCount;

    @Schema(description = "动态总数")
    private Long momentCount;

    @Schema(description = "待审核内容数")
    private Long pendingCount;
}
