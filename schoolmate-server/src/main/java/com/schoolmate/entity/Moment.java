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
 * 班级动态实体（时间轴数据源）。
 *
 * @author Albot
 */
@Data
@TableName("moment")
public class Moment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级ID */
    private Long classId;

    /** 发布人ID */
    private Long userId;

    /** 动态内容 */
    private String content;

    /** 图片URL数组（JSON 字符串） */
    private String imageUrls;

    /** 审核状态 PENDING / PASSED / REJECTED */
    private String auditStatus;

    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
