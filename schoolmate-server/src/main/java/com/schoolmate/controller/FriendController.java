package com.schoolmate.controller;

import com.schoolmate.common.Result;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.friend.FriendRequestCreateDTO;
import com.schoolmate.dto.friend.FriendUpdateDTO;
import com.schoolmate.service.FriendService;
import com.schoolmate.vo.friend.FriendRequestVO;
import com.schoolmate.vo.friend.FriendVO;
import com.schoolmate.vo.friend.UserSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 好友控制器。
 *
 * @author Albot
 */
@Tag(name = "好友", description = "好友列表、好友申请、用户搜索")
@RestController
@RequestMapping("/friends")
public class FriendController {

    @Resource
    private FriendService friendService;

    @Operation(summary = "我的好友列表（含在线状态与私聊会话ID）")
    @GetMapping
    public Result<List<FriendVO>> listFriends() {
        return Result.success(friendService.listFriends(UserContext.getUserId()));
    }

    @Operation(summary = "删除好友（聊天记录保留，重新加回后仍可见）")
    @DeleteMapping("/{friendUserId}")
    public Result<Void> deleteFriend(@PathVariable Long friendUserId) {
        friendService.deleteFriend(UserContext.getUserId(), friendUserId);
        return Result.success();
    }

    @Operation(summary = "设置好友备注名与分组")
    @PutMapping("/{friendUserId}")
    public Result<FriendVO> updateFriend(@PathVariable Long friendUserId,
                                         @Valid @RequestBody FriendUpdateDTO dto) {
        return Result.success(friendService.updateFriend(UserContext.getUserId(), friendUserId, dto));
    }

    @Operation(summary = "收到的好友申请（status 可选 PENDING/ACCEPTED/REJECTED）")
    @GetMapping("/requests")
    public Result<List<FriendRequestVO>> listReceived(@RequestParam(required = false) String status) {
        return Result.success(friendService.listReceivedRequests(UserContext.getUserId(), status));
    }

    @Operation(summary = "我发出的好友申请")
    @GetMapping("/requests/sent")
    public Result<List<FriendRequestVO>> listSent() {
        return Result.success(friendService.listSentRequests(UserContext.getUserId()));
    }

    @Operation(summary = "待我处理的申请数量（用于「新朋友」红点）")
    @GetMapping("/requests/pending-count")
    public Result<Long> pendingCount() {
        return Result.success(friendService.countPending(UserContext.getUserId()));
    }

    @Operation(summary = "发起好友申请")
    @PostMapping("/requests")
    public Result<Long> sendRequest(@Valid @RequestBody FriendRequestCreateDTO dto) {
        return Result.success(friendService.sendRequest(UserContext.getUserId(), dto), "申请已发送");
    }

    @Operation(summary = "同意好友申请（会同时建立私聊会话）")
    @PutMapping("/requests/{requestId}/accept")
    public Result<Void> accept(@PathVariable Long requestId) {
        friendService.acceptRequest(UserContext.getUserId(), requestId);
        return Result.success();
    }

    @Operation(summary = "拒绝好友申请")
    @PutMapping("/requests/{requestId}/reject")
    public Result<Void> reject(@PathVariable Long requestId) {
        friendService.rejectRequest(UserContext.getUserId(), requestId);
        return Result.success();
    }

    @Operation(summary = "搜索用户（按账号/昵称/真实姓名，返回与我关系）")
    @GetMapping("/search")
    public Result<List<UserSearchVO>> search(@RequestParam String keyword) {
        return Result.success(friendService.searchUsers(UserContext.getUserId(), keyword));
    }

    @Operation(summary = "查询我与某用户的关系 SELF/NONE/IS_FRIEND/PENDING_SENT/PENDING_RECEIVED")
    @GetMapping("/relation/{targetUserId}")
    public Result<String> relation(@PathVariable Long targetUserId) {
        return Result.success(friendService.getRelation(UserContext.getUserId(), targetUserId));
    }
}
