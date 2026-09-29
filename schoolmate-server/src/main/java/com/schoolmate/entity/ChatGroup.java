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
 * 群聊实体。
 *
 * <p>群分两类：{@code CLASS} 由班级自动创建且与班级一一对应（数据库唯一索引兜底），
 * {@code CUSTOM} 由用户自由创建。
 *
 * @author Albot
 */
@Data
@TableName("chat_group")
public class ChatGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 群名称 */
    private String groupName;

    /** 群头像URL */
    private String avatar;

    /** 群类型 CLASS 班级群 / CUSTOM 自建群 */
    private String groupType;

    /** 班级群关联的班级ID，自建群为 null */
    private Long classId;

    /** 群主ID */
    private Long ownerId;

    /** 群公告 */
    private String notice;

    /** 成员数（冗余字段，便于列表展示） */
    private Integer memberCount;

    /** 成员上限 */
    private Integer maxMember;

    /** 入群方式 INVITE 仅邀请 / APPROVAL 需审核 */
    private String joinMode;

    /** 状态 NORMAL / DISMISSED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
