package com.schoolmate.dto.cls;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新班级入参。
 *
 * @author Albot
 */
@Data
public class ClassUpdateDTO {

    @Schema(description = "班级名称")
    @NotBlank(message = "班级名称不能为空")
    private String className;

    @Schema(description = "年级")
    private String grade;

    @Schema(description = "毕业日期 yyyy-MM-dd")
    private java.time.LocalDate graduationDate;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "班级简介")
    private String description;
}
