package com.schoolmate.dto.cls;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 加入班级入参。
 *
 * @author Albot
 */
@Data
public class JoinClassDTO {

    @Schema(description = "班级邀请码", example = "CLASS01")
    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;
}
