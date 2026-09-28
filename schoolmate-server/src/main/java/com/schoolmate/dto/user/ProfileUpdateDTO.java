package com.schoolmate.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 个人主页资料入参。
 *
 * @author Albot
 */
@Data
public class ProfileUpdateDTO {

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "学号")
    private String studentNo;

    @Schema(description = "性别 MALE / FEMALE / UNKNOWN")
    private String gender;

    @Schema(description = "生日 yyyy-MM-dd")
    private LocalDate birthday;

    @Schema(description = "籍贯")
    private String hometown;

    @Schema(description = "现居城市")
    private String currentCity;

    @Schema(description = "联系方式（微信/QQ）")
    private String contact;

    @Schema(description = "个性签名")
    private String motto;

    @Schema(description = "入学年份")
    private String enrollmentYear;
}
