package com.schoolmate.controller;

import com.schoolmate.common.PageResult;
import com.schoolmate.common.Result;
import com.schoolmate.context.UserContext;
import com.schoolmate.service.NotificationService;
import com.schoolmate.vo.notification.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知中心控制器。
 *
 * @author Albot
 */
@Tag(name = "通知中心", description = "社交事件通知：好友申请、点赞、评论、时光胶囊到期")
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @Operation(summary = "通知分页列表（unreadOnly=true 时只看未读）")
    @GetMapping
    public Result<PageResult<NotificationVO>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                   @RequestParam(defaultValue = "10") Long pageSize,
                                                   @RequestParam(required = false) Boolean unreadOnly) {
        return Result.success(notificationService.pageNotifications(
                UserContext.getUserId(), pageNum, pageSize, unreadOnly));
    }

    @Operation(summary = "未读通知数（导航栏铃铛角标）")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(notificationService.unreadCount(UserContext.getUserId()));
    }

    @Operation(summary = "标记单条已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(UserContext.getUserId(), id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(UserContext.getUserId());
        return Result.success();
    }
}
