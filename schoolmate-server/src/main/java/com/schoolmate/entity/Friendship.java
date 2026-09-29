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
 * 好友关系实体。
 *
 * <p>采用<b>单行存储</b>：约定 userAId &lt; userBId，配合 uk_pair 唯一索引，
 * 保证同一对用户只存在一行记录，避免双向两行数据不同步的问题。
 *
 * <p>备注名与分组双向存储，因为「我给对方起的备注」属于私有数据，不应暴露给对方。
 *
 * @author Albot
 */
@Data
@TableName("friendship")
public class Friendship {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 较小的用户ID */
    private Long userAId;

    /** 较大的用户ID */
    private Long userBId;

    /** A 给 B 设置的备注名 */
    private String remarkA;

    /** B 给 A 设置的备注名 */
    private String remarkB;

    /** A 侧好友分组 */
    private String groupA;

    /** B 侧好友分组 */
    private String groupB;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
