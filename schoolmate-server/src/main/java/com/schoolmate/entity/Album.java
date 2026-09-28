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
 * 相册实体。
 *
 * @author Albot
 */
@Data
@TableName("album")
public class Album {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级ID */
    private Long classId;

    /** 创建人ID */
    private Long userId;

    /** 相册名称 */
    private String name;

    /** 封面URL */
    private String coverUrl;

    /** 相册描述 */
    private String description;

    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
