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

    @Schema(description = "星座（根据生日自动计算）")
    private String constellation;

    @Schema(description = "MBTI 人格类型")
    private String mbti;

    @Schema(description = "兴趣爱好标签")
    private java.util.List<String> hobbies;

    @Schema(description = "技能标签")
    private java.util.List<String> skills;

    @Schema(description = "毕业寄语")
    private String graduationMessage;

    @Schema(description = "社交链接")
    private java.util.Map<String, String> socialLinks;

    @Schema(description = "个人主页封面图 URL")
    private String coverImage;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "昵称")
    private String nickname;

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
