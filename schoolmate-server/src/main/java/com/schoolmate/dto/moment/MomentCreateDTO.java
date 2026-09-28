package com.schoolmate.dto.moment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发布/修改班级动态入参。
 *
 * @author Albot
 */
@Data
public class MomentCreateDTO {

    @Schema(description = "动态内容")
    @NotBlank(message = "动态内容不能为空")
    @Size(max = 2000, message = "动态内容不能超过 2000 字")
    private String content;

    @Schema(description = "图片URL列表（需先调用文件上传接口获取）")
    private List<String> imageUrls;
}
