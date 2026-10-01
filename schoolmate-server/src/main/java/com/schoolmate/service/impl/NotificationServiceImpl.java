package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schoolmate.common.PageResult;
import com.schoolmate.common.ResultCode;
import com.schoolmate.entity.Notification;
import com.schoolmate.entity.User;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.mapper.NotificationMapper;
import com.schoolmate.service.NotificationService;
import com.schoolmate.service.UserService;
import com.schoolmate.vo.notification.NotificationVO;
import com.schoolmate.ws.WsFrame;
import com.schoolmate.ws.WsFrameType;
import com.schoolmate.ws.WsSessionRegistry;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.schoolmate.utils.TransactionUtil.afterCommit;

/**
 * 通知中心实现。
 *
 * @author Albot
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    /** 摘要最长保留字符数，超出的截断 */
    private static final int CONTENT_MAX_LEN = 200;

    @Resource
    private UserService userService;

    @Resource
    private WsSessionRegistry wsSessionRegistry;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notify(Long userId, String type, String title, String content,
                       String bizType, Long bizId, Long fromUserId) {
        if (userId == null) {
            return;
        }
        // 自己触发的动作（自己赞自己、自己评论自己）不产生通知
        if (Objects.equals(userId, fromUserId)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(truncate(content));
        notification.setBizType(bizType);
        notification.setBizId(bizId);
        notification.setFromUserId(fromUserId);
        notification.setReadFlag(0);
        this.save(notification);

        // 落库成功后实时推送，让在线用户的铃铛立即 +1
        Notification saved = notification;
        afterCommit(() -> wsSessionRegistry.pushToUser(userId,
                WsFrame.of(WsFrameType.S2C_NOTIFICATION, toVO(saved))));
    }

    @Override
    public PageResult<NotificationVO> pageNotifications(Long userId, Long pageNum, Long pageSize, Boolean unreadOnly) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getId);
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.eq(Notification::getReadFlag, 0);
        }
        IPage<Notification> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page, this::toVO);
    }

    @Override
    public long unreadCount(Long userId) {
        Long count = this.count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, 0));
        return count == null ? 0 : count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long id) {
        Notification notification = this.getById(id);
        if (notification == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
        }
        if (!Objects.equals(notification.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该通知");
        }
        if (notification.getReadFlag() == 0) {
            notification.setReadFlag(1);
            this.updateById(notification);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        this.lambdaUpdate()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, 0)
                .set(Notification::getReadFlag, 1)
                .update();
    }

    /* ==================== 内部工具 ==================== */

    private NotificationVO toVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setType(notification.getType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setBizType(notification.getBizType());
        vo.setBizId(notification.getBizId());
        vo.setFromUserId(notification.getFromUserId());
        vo.setRead(notification.getReadFlag() != null && notification.getReadFlag() == 1);
        vo.setCreateTime(notification.getCreateTime());

        if (notification.getFromUserId() != null) {
            User from = userService.getById(notification.getFromUserId());
            if (from != null) {
                vo.setFromNickname(from.getNickname());
                vo.setFromAvatar(from.getAvatar());
            }
        }
        return vo;
    }

    private String truncate(String content) {
        if (content == null || content.length() <= CONTENT_MAX_LEN) {
            return content;
        }
        return content.substring(0, CONTENT_MAX_LEN);
    }
}
