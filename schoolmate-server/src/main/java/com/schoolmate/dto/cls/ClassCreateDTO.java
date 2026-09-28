package com.schoolmate.dto.cls;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建班级入参。
 *
 * @author Albot
 */
@Data
public class ClassCreateDTO {

    @Schema(description = "班级名称", example = "2024级智能科学与技术1班")
    @NotBlank(message = "班级名称不能为空")
    @Size(max = 100, message = "班级名称长度不能超过 100")
    private String className;

    @Schema(description = "年级", example = "2024级")
    private String grade;

    @Schema(description = "专业", example = "智能科学与技术")
    private String major;

    @Schema(description = "班级简介")
    private String description;
}
