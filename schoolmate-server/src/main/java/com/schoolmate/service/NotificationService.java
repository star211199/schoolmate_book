package com.schoolmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schoolmate.common.PageResult;
import com.schoolmate.entity.Notification;
import com.schoolmate.vo.notification.NotificationVO;

/**
 * 通知中心服务。
 *
 * @author Albot
 */
public interface NotificationService extends IService<Notification> {

    /** 通知类型：收到好友申请 */
    String TYPE_FRIEND_REQUEST = "FRIEND_REQUEST";
    /** 通知类型：好友申请被同意 */
    String TYPE_FRIEND_ACCEPTED = "FRIEND_ACCEPTED";
    /** 通知类型：动态被点赞 */
    String TYPE_MOMENT_LIKE = "MOMENT_LIKE";
    /** 通知类型：动态被评论 / 评论被回复 */
    String TYPE_MOMENT_COMMENT = "MOMENT_COMMENT";
    /** 通知类型：时光胶囊到点开启 */
    String TYPE_CAPSULE_OPENED = "CAPSULE_OPENED";

    /** 关联业务类型 */
    String BIZ_FRIEND = "FRIEND";
    String BIZ_MOMENT = "MOMENT";
    String BIZ_CAPSULE = "CAPSULE";

    /**
     * 写入一条通知并实时推送给接收人。
     *
     * <p>接收人等于触发人（如自己赞自己的动态）时不产生通知。
     * 推送挂在事务提交后执行，避免「通知到了但业务数据回滚了」。
     *
     * @param userId     接收人ID
     * @param type       通知类型（{@code TYPE_*} 常量）
     * @param title      标题
     * @param content    摘要内容（可空）
     * @param bizType    关联业务类型 FRIEND/MOMENT/CAPSULE（可空）
     * @param bizId      关联业务ID（可空）
     * @param fromUserId 触发人ID（系统通知传 null）
     */
    void notify(Long userId, String type, String title, String content,
                String bizType, Long bizId, Long fromUserId);

    /** 通知分页列表，unreadOnly 为 true 时只看未读 */
    PageResult<NotificationVO> pageNotifications(Long userId, Long pageNum, Long pageSize, Boolean unreadOnly);

    /** 未读数（铃铛角标） */
    long unreadCount(Long userId);

    /** 标记单条已读（仅本人） */
    void markRead(Long userId, Long id);

    /** 全部标记已读 */
    void markAllRead(Long userId);
}
