package com.schoolmate.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询公共入参。
 *
 * @author Albot
 */
@Data
public class BasePageQuery {

    @Schema(description = "页码，从 1 开始", example = "1")
    private Long pageNum = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize = 10L;
}
