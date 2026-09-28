package com.schoolmate.vo.cls;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 生日提醒出参。
 *
 * @author Albot
 */
@Data
public class BirthdayReminderVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "星座")
    private String constellation;

    @Schema(description = "距下一次生日天数（0 表示今天）")
    private Long daysUntil;

    @Schema(description = "今天是否生日")
    private Boolean today;
}
