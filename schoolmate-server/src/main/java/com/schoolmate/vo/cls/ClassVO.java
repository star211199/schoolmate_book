package com.schoolmate.vo.cls;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级出参。
 *
 * @author Albot
 */
@Data
public class ClassVO {

    @Schema(description = "班级ID")
    private Long id;

    @Schema(description = "班级名称")
    private String className;

    @Schema(description = "年级")
    private String grade;

    @Schema(description = "毕业日期")
    private java.time.LocalDate graduationDate;

    @Schema(description = "距毕业天数（未设置毕业日期为 null，已过为负数）")
    private Long daysToGraduation;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "班级简介")
    private String description;

    @Schema(description = "邀请码")
    private String inviteCode;

    @Schema(description = "创建人ID")
    private Long ownerId;

    @Schema(description = "成员数量")
    private Long memberCount;

    @Schema(description = "当前登录用户在班级内的角色（未加入为 null）")
    private String currentUserRole;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
