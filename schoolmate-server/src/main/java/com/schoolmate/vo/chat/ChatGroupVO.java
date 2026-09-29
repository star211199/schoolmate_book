package com.schoolmate.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群资料 VO。
 *
 * @author Albot
 */
@Data
public class ChatGroupVO {

    @Schema(description = "群ID")
    private Long id;

    @Schema(description = "群名称")
    private String groupName;

    @Schema(description = "群头像")
    private String avatar;

    @Schema(description = "群类型 CLASS/CUSTOM")
    private String groupType;

    @Schema(description = "关联班级ID")
    private Long classId;

    @Schema(description = "群主ID")
    private Long ownerId;

    @Schema(description = "群公告")
    private String notice;

    @Schema(description = "成员数")
    private Integer memberCount;

    @Schema(description = "成员上限")
    private Integer maxMember;

    @Schema(description = "入群方式 INVITE/APPROVAL")
    private String joinMode;

    @Schema(description = "当前用户在该群的角色 OWNER/ADMIN/MEMBER")
    private String myRole;

    @Schema(description = "对应的会话ID")
    private Long sessionId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
