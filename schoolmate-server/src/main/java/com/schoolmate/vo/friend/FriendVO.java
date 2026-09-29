package com.schoolmate.vo.friend;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 好友列表项 VO。
 *
 * @author Albot
 */
@Data
public class FriendVO {

    @Schema(description = "好友用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "我对该好友设置的备注名")
    private String remark;

    @Schema(description = "展示名称：有备注优先用备注")
    private String displayName;

    @Schema(description = "好友分组")
    private String groupName;

    @Schema(description = "个性签名")
    private String motto;

    @Schema(description = "是否在线")
    private Boolean online;

    @Schema(description = "与我的私聊会话ID，便于直接跳转聊天")
    private Long sessionId;
}
