package com.schoolmate.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 个人主页资料出参。
 *
 * @author Albot
 */
@Data
public class UserProfileVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "学号")
    private String studentNo;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "籍贯")
    private String hometown;

    @Schema(description = "现居城市")
    private String currentCity;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "个性签名")
    private String motto;

    @Schema(description = "入学年份")
    private String enrollmentYear;
}
