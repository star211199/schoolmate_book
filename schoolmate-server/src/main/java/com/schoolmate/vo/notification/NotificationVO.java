package com.schoolmate.vo.notification;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知出参。
 *
 * @author Albot
 */
@Data
public class NotificationVO {

    private Long id;

    /** 通知类型 FRIEND_REQUEST/FRIEND_ACCEPTED/MOMENT_LIKE/MOMENT_COMMENT/CAPSULE_OPENED */
    private String type;

    /** 标题，如「张三 赞了你的动态」 */
    private String title;

    /** 摘要内容 */
    private String content;

    /** 关联业务类型 FRIEND/MOMENT/CAPSULE（前端决定跳转目标） */
    private String bizType;

    /** 关联业务ID */
    private Long bizId;

    /** 触发人信息（系统通知为 null） */
    private Long fromUserId;
    private String fromNickname;
    private String fromAvatar;

    /** 是否已读 */
    private Boolean read;

    private LocalDateTime createTime;
}
