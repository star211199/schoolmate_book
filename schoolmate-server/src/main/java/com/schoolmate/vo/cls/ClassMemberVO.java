package com.schoolmate.vo.cls;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级成员出参。
 *
 * @author Albot
 */
@Data
public class ClassMemberVO {

    @Schema(description = "成员记录ID")
    private Long id;

    @Schema(description = "班级ID")
    private Long classId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "班级内角色 OWNER / MEMBER")
    private String memberRole;

    @Schema(description = "加入时间")
    private LocalDateTime joinTime;
}
