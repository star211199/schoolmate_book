package com.schoolmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 同学个人主页资料实体（与 user 一对一）。
 *
 * @author Albot
 */
@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 学号 */
    private String studentNo;

    /** 性别 MALE / FEMALE / UNKNOWN */
    private String gender;

    /** 生日 */
    private LocalDate birthday;

    /** 星座（根据生日自动计算） */
    private String constellation;

    /** MBTI 人格类型 */
    private String mbti;

    /** 兴趣爱好（JSON 数组字符串） */
    private String hobbies;

    /** 技能标签（JSON 数组字符串） */
    private String skills;

    /** 毕业寄语 */
    private String graduationMessage;

    /** 社交链接（JSON 对象字符串） */
    private String socialLinks;

    /** 个人主页封面图 URL */
    private String coverImage;

    /** 籍贯 */
    private String hometown;

    /** 现居城市 */
    private String currentCity;

    /** 联系方式（微信/QQ） */
    private String contact;

    /** 个性签名 */
    private String motto;

    /** 入学年份 */
    private String enrollmentYear;

    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
