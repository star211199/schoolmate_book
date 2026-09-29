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
 * 群成员实体。
 *
 * <p>与 class_member 的分工：class_member 维护「教务关系」（谁是这个班的同学），
 * chat_group_member 维护「聊天关系」（谁在这个群里）。班级群两者同步，自建群只有后者。
 *
 * @author Albot
 */
@Data
@TableName("chat_group_member")
public class ChatGroupMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 群ID */
    private Long groupId;

    /** 用户ID */
    private Long userId;

    /** 群内角色 OWNER / ADMIN / MEMBER */
    private String memberRole;

    /** 群昵称 */
    private String groupNickname;

    /** 禁言截止时间，null 表示未被禁言 */
    private LocalDateTime muteUntil;

    /** 已读位点：该成员已读到的最大消息ID，未读数由此计算 */
    private Long lastReadMessageId;

    /** 加入时间 */
    private LocalDateTime joinTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
