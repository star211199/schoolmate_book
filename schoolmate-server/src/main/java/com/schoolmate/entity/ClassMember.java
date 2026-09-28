package com.schoolmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级成员实体。
 *
 * @author Albot
 */
@Data
@TableName("class_member")
public class ClassMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级ID */
    private Long classId;

    /** 用户ID */
    private Long userId;

    /** 班级内角色 OWNER / MEMBER */
    private String memberRole;

    /** 加入时间 */
    private LocalDateTime joinTime;

    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
