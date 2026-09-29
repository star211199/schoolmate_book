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
 * 用户会话状态实体。
 *
 * <p>置顶、免打扰、已读位点都是「用户私有」的数据，必须与共享的 chat_session 分开存，
 * 否则 A 置顶了群聊会连带影响 B。
 *
 * @author Albot
 */
@Data
@TableName("chat_session_user")
public class ChatSessionUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private Long sessionId;

    /** 用户ID */
    private Long userId;

    /** 已读位点：未读数 = 该会话中 id 大于此值且非本人发送的消息数 */
    private Long lastReadMessageId;

    /** 是否置顶 */
    private Integer pinned;

    /** 是否免打扰 */
    private Integer muted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
