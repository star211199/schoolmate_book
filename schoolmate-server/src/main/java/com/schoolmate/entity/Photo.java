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
 * 照片实体。
 *
 * @author Albot
 */
@Data
@TableName("photo")
public class Photo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 相册ID */
    private Long albumId;

    /** 上传者ID */
    private Long userId;

    /** 图片URL */
    private String url;

    /** 图片描述 */
    private String description;

    /** 审核状态 PENDING / PASSED / REJECTED */
    private String auditStatus;

    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
