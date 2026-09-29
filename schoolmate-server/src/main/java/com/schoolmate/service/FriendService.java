package com.schoolmate.service;

import com.schoolmate.dto.friend.FriendRequestCreateDTO;
import com.schoolmate.dto.friend.FriendUpdateDTO;
import com.schoolmate.vo.friend.FriendRequestVO;
import com.schoolmate.vo.friend.FriendVO;
import com.schoolmate.vo.friend.UserSearchVO;

import java.util.List;

/**
 * 好友服务。
 *
 * @author Albot
 */
public interface FriendService {

    /** 我的好友列表（含在线状态与私聊会话ID） */
    List<FriendVO> listFriends(Long userId);

    /** 删除好友（同时把双方从好友关系中移除，保留聊天记录） */
    void deleteFriend(Long userId, Long friendUserId);

    /** 修改备注名 / 分组 */
    FriendVO updateFriend(Long userId, Long friendUserId, FriendUpdateDTO dto);

    /** 发起好友申请，返回申请ID */
    Long sendRequest(Long fromUserId, FriendRequestCreateDTO dto);

    /** 我收到的申请（status 为空时返回全部） */
    List<FriendRequestVO> listReceivedRequests(Long userId, String status);

    /** 我发出的申请 */
    List<FriendRequestVO> listSentRequests(Long userId);

    /** 同意申请：建立好友关系并自动创建私聊会话 */
    void acceptRequest(Long userId, Long requestId);

    /** 拒绝申请 */
    void rejectRequest(Long userId, Long requestId);

    /** 待我处理的申请数量（用于红点） */
    Long countPending(Long userId);

    /** 搜索用户（排除自己），并标注与我的关系 */
    List<UserSearchVO> searchUsers(Long userId, String keyword);

    /** 查询我与目标用户的关系 SELF/NONE/IS_FRIEND/PENDING_SENT/PENDING_RECEIVED */
    String getRelation(Long userId, Long targetUserId);

    /** 是否为好友 */
    boolean isFriend(Long userId, Long otherUserId);

    /** 断言两人是好友，否则抛业务异常 */
    void assertFriend(Long userId, Long otherUserId);
}
