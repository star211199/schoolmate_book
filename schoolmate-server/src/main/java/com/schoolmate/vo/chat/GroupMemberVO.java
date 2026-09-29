package com.schoolmate.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群成员 VO。
 *
 * @author Albot
 */
@Data
public class GroupMemberVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "群内角色 OWNER/ADMIN/MEMBER")
    private String memberRole;

    @Schema(description = "群昵称")
    private String groupNickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "是否在线")
    private Boolean online;

    @Schema(description = "是否被禁言")
    private Boolean muted;

    @Schema(description = "加入时间")
    private LocalDateTime joinTime;
}
