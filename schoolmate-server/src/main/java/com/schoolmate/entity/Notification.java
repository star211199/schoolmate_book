package com.schoolmate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知中心实体。
 *
 * <p>聚合社交事件（好友申请/通过、动态被点赞/评论、时光胶囊到期），
 * 驱动导航栏铃铛角标与通知列表页。
 *
 * @author Albot
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人ID */
    private Long userId;

    /** 通知类型 FRIEND_REQUEST/FRIEND_ACCEPTED/MOMENT_LIKE/MOMENT_COMMENT/CAPSULE_OPENED */
    private String type;

    /** 标题，如「张三 赞了你的动态」 */
    private String title;

    /** 摘要内容（评论内容、申请留言等，截断存储） */
    private String content;

    /** 关联业务类型 FRIEND/MOMENT/CAPSULE，前端据此决定跳转目标 */
    private String bizType;

    /** 关联业务ID（requestId/momentId/capsuleId） */
    private Long bizId;

    /** 触发人ID（谁赞的、谁评论的），系统通知为 null */
    private Long fromUserId;

    /** 已读标记 0未读 1已读 */
    private Integer readFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
