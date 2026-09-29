package com.schoolmate.vo.friend;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友申请 VO（同时用于「收到的申请」与「我发出的申请」两个列表）。
 *
 * @author Albot
 */
@Data
public class FriendRequestVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "申请人ID")
    private Long fromUserId;

    @Schema(description = "被申请人ID")
    private Long toUserId;

    @Schema(description = "申请人昵称")
    private String nickname;

    @Schema(description = "申请人头像")
    private String avatar;

    @Schema(description = "申请人真实姓名")
    private String realName;

    @Schema(description = "验证消息")
    private String message;

    @Schema(description = "状态 PENDING/ACCEPTED/REJECTED/EXPIRED")
    private String status;

    @Schema(description = "申请时间")
    private LocalDateTime createTime;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;
}
