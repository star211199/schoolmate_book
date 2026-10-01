package com.schoolmate.vo.capsule;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 时光胶囊出参。
 *
 * <p><b>关键约定</b>：未到开启时间时 {@link #content} 为 null，
 * 前端用 {@link #openable} 与 {@link #countdownSeconds} 渲染倒计时卡片。
 *
 * @author Albot
 */
@Data
public class CapsuleVO {

    private Long id;

    /** 写信人信息 */
    private Long userId;
    private String nickname;
    private String avatar;

    /** 所属班级（PUBLIC 类型才有） */
    private Long classId;

    private String title;

    /** 信件内容：未到开启时间为 null */
    private String content;

    /** 开启时间 */
    private LocalDateTime openTime;

    /** 收件类型 SELF / PUBLIC */
    private String openType;

    /** 状态 SEALED 封存中 / OPENED 已开启（由定时任务维护，仅用于展示） */
    private String status;

    /** 是否已到开启时间（实时计算，不看 status） */
    private Boolean openable;

    /** 距开启剩余秒数，已到点为 0 */
    private Long countdownSeconds;

    private LocalDateTime createTime;
}
