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
 * 时光胶囊实体（写给未来自己或全班的一封信，到点才可开启）。
 *
 * <p>属于 P3 亮点功能，P0 阶段仅建表与实体，业务接口留待后续迭代。
 *
 * @author Albot
 */
@Data
@TableName("time_capsule")
public class TimeCapsule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 写信人ID */
    private Long userId;

    /** 所属班级ID */
    private Long classId;

    /** 标题 */
    private String title;

    /** 信件内容 */
    private String content;

    /** 开启时间，到点后才能查看 */
    private LocalDateTime openTime;

    /** 收件类型 SELF 写给未来的自己 / PUBLIC 写给全班 */
    private String openType;

    /** 状态 SEALED 封存中 / OPENED 已开启 */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
