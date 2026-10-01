package com.schoolmate.dto.capsule;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 写时光胶囊入参。
 *
 * @author Albot
 */
@Schema(description = "写时光胶囊入参")
@Data
public class CapsuleCreateDTO {

    @Schema(description = "标题", example = "写给毕业那天的自己")
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过 100 字")
    private String title;

    @Schema(description = "信件内容")
    @NotBlank(message = "信件内容不能为空")
    @Size(max = 2000, message = "信件内容不能超过 2000 字")
    private String content;

    @Schema(description = "开启时间（必须是将来），到点前任何人无法查看内容", example = "2028-06-30 12:00:00")
    @NotNull(message = "开启时间不能为空")
    @Future(message = "开启时间必须是将来的时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime openTime;

    @Schema(description = "收件类型 SELF 写给未来的自己 / PUBLIC 写给全班", example = "SELF")
    @NotBlank(message = "收件类型不能为空")
    @Pattern(regexp = "SELF|PUBLIC", message = "收件类型只能是 SELF 或 PUBLIC")
    private String openType;

    @Schema(description = "openType=PUBLIC 时必填：写给哪个班", example = "1")
    private Long classId;
}
