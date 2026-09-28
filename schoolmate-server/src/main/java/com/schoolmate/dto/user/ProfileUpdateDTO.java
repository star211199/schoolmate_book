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

    @Schema(description = "生日 yyyy-MM-dd（保存时自动计算星座）")
    private LocalDate birthday;

    @Schema(description = "MBTI 人格类型，如 INFP")
    private String mbti;

    @Schema(description = "兴趣爱好标签列表")
    private java.util.List<String> hobbies;

    @Schema(description = "技能标签列表")
    private java.util.List<String> skills;

    @Schema(description = "毕业寄语")
    private String graduationMessage;

    @Schema(description = "社交链接，如 {\"qq\":\"123\",\"wechat\":\"abc\"}")
    private java.util.Map<String, String> socialLinks;

    @Schema(description = "个人主页封面图 URL")
    private String coverImage;

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
