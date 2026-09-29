package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schoolmate.common.ResultCode;
import com.schoolmate.dto.friend.FriendRequestCreateDTO;
import com.schoolmate.dto.friend.FriendUpdateDTO;
import com.schoolmate.entity.ChatSession;
import com.schoolmate.entity.FriendRequest;
import com.schoolmate.entity.Friendship;
import com.schoolmate.entity.User;
import com.schoolmate.entity.UserProfile;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.mapper.ChatSessionMapper;
import com.schoolmate.mapper.FriendRequestMapper;
import com.schoolmate.mapper.FriendshipMapper;
import com.schoolmate.mapper.UserMapper;
import com.schoolmate.mapper.UserProfileMapper;
import com.schoolmate.service.ChatService;
import com.schoolmate.service.FriendService;
import com.schoolmate.utils.TransactionUtil;
import com.schoolmate.vo.friend.FriendRequestVO;
import com.schoolmate.vo.friend.FriendVO;
import com.schoolmate.vo.friend.UserSearchVO;
import com.schoolmate.ws.WsFrame;
import com.schoolmate.ws.WsFrameType;
import com.schoolmate.ws.WsSessionRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 好友服务实现。
 *
 * <p>好友关系采用<b>单行存储</b>（userAId &lt; userBId），避免双向两行数据不同步。
 *
 * <p>最关键的一处设计在 {@link #acceptRequest}：<b>同意申请的一瞬间就自动创建私聊会话</b>。
 * 如果等用户点开聊天框才建会话，会话列表里不会立即出现对方，用户会误以为没加上好友。
 *
 * @author Albot
 */
@Slf4j
@Service
public class FriendServiceImpl extends ServiceImpl<FriendshipMapper, Friendship> implements FriendService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    public static final String RELATION_SELF = "SELF";
    public static final String RELATION_NONE = "NONE";
    public static final String RELATION_FRIEND = "IS_FRIEND";
    public static final String RELATION_PENDING_SENT = "PENDING_SENT";
    public static final String RELATION_PENDING_RECEIVED = "PENDING_RECEIVED";

    @Resource
    private FriendRequestMapper friendRequestMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserProfileMapper userProfileMapper;

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatService chatService;

    @Resource
    private WsSessionRegistry wsSessionRegistry;

    /* ==================== 好友列表与维护 ==================== */

    @Override
    public List<FriendVO> listFriends(Long userId) {
        List<Friendship> relations = baseMapper.selectList(new LambdaQueryWrapper<Friendship>()
            .and(w -> w.eq(Friendship::getUserAId, userId).or().eq(Friendship::getUserBId, userId)));
        if (relations.isEmpty()) {
            return List.of();
        }

        // 解析出每个好友的用户ID，以及「我给他的备注」
        Map<Long, Friendship> friendIdToRelation = new HashMap<>(relations.size());
        Map<Long, String> friendIdToRemark = new HashMap<>(relations.size());
        Map<Long, String> friendIdToGroup = new HashMap<>(relations.size());
        for (Friendship f : relations) {
            boolean meIsA = Objects.equals(f.getUserAId(), userId);
            Long friendId = meIsA ? f.getUserBId() : f.getUserAId();
            friendIdToRelation.put(friendId, f);
            friendIdToRemark.put(friendId, meIsA ? f.getRemarkA() : f.getRemarkB());
            friendIdToGroup.put(friendId, meIsA ? f.getGroupA() : f.getGroupB());
        }

        Set<Long> friendIds = friendIdToRelation.keySet();
        Map<Long, User> userMap = userMapper.selectBatchIds(friendIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Map<Long, String> realNameMap = userProfileMapper.selectList(
                new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, friendIds))
            .stream().filter(p -> StringUtils.hasText(p.getRealName()))
            .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getRealName, (a, b) -> a));

        // 批量查已有的私聊会话，便于前端从好友列表直接跳转聊天
        List<String> bizKeys = friendIds.stream()
            .map(fid -> "P_" + Math.min(userId, fid) + "_" + Math.max(userId, fid))
            .toList();
        Map<String, Long> bizKeyToSessionId = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>().in(ChatSession::getBizKey, bizKeys))
            .stream().collect(Collectors.toMap(ChatSession::getBizKey, ChatSession::getId, (a, b) -> a));

        List<FriendVO> result = new ArrayList<>(friendIds.size());
        for (Long friendId : friendIds) {
            User u = userMap.get(friendId);
            if (u == null) {
                continue;
            }
            String nickname = StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername();
            String remark = friendIdToRemark.get(friendId);

            FriendVO vo = new FriendVO();
            vo.setUserId(friendId);
            vo.setNickname(nickname);
            vo.setAvatar(u.getAvatar());
            vo.setRealName(realNameMap.get(friendId));
            vo.setRemark(remark);
            vo.setDisplayName(StringUtils.hasText(remark) ? remark : nickname);
            vo.setGroupName(friendIdToGroup.get(friendId));
            vo.setOnline(wsSessionRegistry.isOnline(friendId));
            vo.setSessionId(bizKeyToSessionId.get("P_" + Math.min(userId, friendId) + "_" + Math.max(userId, friendId)));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long userId, Long friendUserId) {
        Friendship relation = findRelation(userId, friendUserId);
        if (relation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "你们还不是好友");
        }
        // 逻辑删除关系。聊天记录保留，通过好友申请重新加回后仍能看到历史
        baseMapper.deleteById(relation.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendVO updateFriend(Long userId, Long friendUserId, FriendUpdateDTO dto) {
        Friendship relation = findRelation(userId, friendUserId);
        if (relation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "你们还不是好友");
        }
        boolean meIsA = Objects.equals(relation.getUserAId(), userId);
        if (dto.getRemark() != null) {
            if (meIsA) {
                relation.setRemarkA(dto.getRemark());
            } else {
                relation.setRemarkB(dto.getRemark());
            }
        }
        if (dto.getGroupName() != null) {
            if (meIsA) {
                relation.setGroupA(dto.getGroupName());
            } else {
                relation.setGroupB(dto.getGroupName());
            }
        }
        baseMapper.updateById(relation);

        return listFriends(userId).stream()
            .filter(f -> Objects.equals(f.getUserId(), friendUserId))
            .findFirst()
            .orElse(null);
    }

    /* ==================== 好友申请 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendRequest(Long fromUserId, FriendRequestCreateDTO dto) {
        Long toUserId = dto.getToUserId();
        if (Objects.equals(fromUserId, toUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能添加自己为好友");
        }
        if (userMapper.selectById(toUserId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (isFriend(fromUserId, toUserId)) {
            throw new BusinessException(ResultCode.CONFLICT, "你们已经是好友了");
        }

        // 对方已经向我发过申请：直接互相成为好友，省掉一次多余的确认
        FriendRequest reverse = findPendingRequest(toUserId, fromUserId);
        if (reverse != null) {
            acceptRequest(fromUserId, reverse.getId());
            return reverse.getId();
        }

        if (findPendingRequest(fromUserId, toUserId) != null) {
            throw new BusinessException(ResultCode.CONFLICT, "已发送过申请，请等待对方处理");
        }

        // 曾经被拒绝过则清掉旧记录，允许重新申请
        FriendRequest rejected = friendRequestMapper.selectOne(new LambdaQueryWrapper<FriendRequest>()
            .eq(FriendRequest::getFromUserId, fromUserId)
            .eq(FriendRequest::getToUserId, toUserId)
            .eq(FriendRequest::getStatus, STATUS_REJECTED)
            .orderByDesc(FriendRequest::getId)
            .last("LIMIT 1"));
        if (rejected != null) {
            friendRequestMapper.deleteById(rejected.getId());
        }

        String message = StringUtils.hasText(dto.getMessage()) ? dto.getMessage() : "你好，我想加你为好友";
        FriendRequest request = new FriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setMessage(message);
        request.setStatus(STATUS_PENDING);
        friendRequestMapper.insert(request);

        // 对方在线则实时收到通知，「新朋友」入口出现红点
        User fromUser = userMapper.selectById(fromUserId);
        Map<String, Object> payload = new HashMap<>(4);
        payload.put("requestId", request.getId());
        payload.put("fromUserId", fromUserId);
        payload.put("nickname", fromUser == null ? null : fromUser.getNickname());
        payload.put("avatar", fromUser == null ? null : fromUser.getAvatar());
        payload.put("message", message);
        afterCommit(() -> wsSessionRegistry.pushToUser(toUserId,
            WsFrame.of(WsFrameType.S2C_FRIEND_REQUEST, payload)));

        return request.getId();
    }

    @Override
    public List<FriendRequestVO> listReceivedRequests(Long userId, String status) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<FriendRequest>()
            .eq(FriendRequest::getToUserId, userId)
            .orderByDesc(FriendRequest::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(FriendRequest::getStatus, status);
        } else {
            // 默认不展示已过期的申请
            wrapper.ne(FriendRequest::getStatus, "EXPIRED");
        }
        return toRequestVOList(friendRequestMapper.selectList(wrapper));
    }

    @Override
    public List<FriendRequestVO> listSentRequests(Long userId) {
        return toRequestVOList(friendRequestMapper.selectList(new LambdaQueryWrapper<FriendRequest>()
            .eq(FriendRequest::getFromUserId, userId)
            .ne(FriendRequest::getStatus, "EXPIRED")
            .orderByDesc(FriendRequest::getId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请不存在");
        }
        if (!Objects.equals(request.getToUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权处理该申请");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "该申请已处理");
        }

        request.setStatus(STATUS_ACCEPTED);
        request.setHandleTime(LocalDateTime.now());
        friendRequestMapper.updateById(request);

        Long otherId = request.getFromUserId();
        long min = Math.min(userId, otherId);
        long max = Math.max(userId, otherId);

        Friendship relation = new Friendship();
        relation.setUserAId(min);
        relation.setUserBId(max);
        try {
            baseMapper.insert(relation);
        } catch (DuplicateKeyException e) {
            log.debug("好友关系已存在，跳过插入：{} - {}", min, max);
        }

        // 关键：立刻建会话，双方会话列表即时出现对方
        Long sessionId = chatService.getOrCreatePrivateSession(min, max);

        Map<String, Object> payload = new HashMap<>(4);
        payload.put("userId", userId);
        payload.put("sessionId", sessionId);
        afterCommit(() -> wsSessionRegistry.pushToUser(otherId,
            WsFrame.of(WsFrameType.S2C_FRIEND_ACCEPTED, payload)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请不存在");
        }
        if (!Objects.equals(request.getToUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权处理该申请");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "该申请已处理");
        }
        request.setStatus(STATUS_REJECTED);
        request.setHandleTime(LocalDateTime.now());
        friendRequestMapper.updateById(request);
        // 有意不通知申请人：避免「被拒绝」的尴尬，对方侧一直显示等待验证即可
    }

    @Override
    public Long countPending(Long userId) {
        return friendRequestMapper.selectCount(new LambdaQueryWrapper<FriendRequest>()
            .eq(FriendRequest::getToUserId, userId)
            .eq(FriendRequest::getStatus, STATUS_PENDING));
    }

    /* ==================== 搜索与关系判断 ==================== */

    @Override
    public List<UserSearchVO> searchUsers(Long userId, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        String kw = keyword.trim();

        Set<Long> matchedIds = new HashSet<>();
        // 账号 / 昵称匹配
        userMapper.selectList(new LambdaQueryWrapper<User>()
                .and(w -> w.like(User::getUsername, kw).or().like(User::getNickname, kw))
                .last("LIMIT 20"))
            .forEach(u -> matchedIds.add(u.getId()));
        // 真实姓名匹配（在 user_profile 里）
        userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                .like(UserProfile::getRealName, kw)
                .last("LIMIT 20"))
            .forEach(p -> matchedIds.add(p.getUserId()));

        matchedIds.remove(userId);

        if (matchedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(matchedIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<Long, String> realNameMap = userProfileMapper.selectList(
                new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, matchedIds))
            .stream().filter(p -> StringUtils.hasText(p.getRealName()))
            .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getRealName, (a, b) -> a));

        List<UserSearchVO> result = new ArrayList<>(matchedIds.size());
        for (Long id : matchedIds) {
            User u = userMap.get(id);
            if (u == null) {
                continue;
            }
            UserSearchVO vo = new UserSearchVO();
            vo.setUserId(id);
            vo.setNickname(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
            vo.setAvatar(u.getAvatar());
            vo.setRealName(realNameMap.get(id));
            vo.setRelation(getRelation(userId, id));
            result.add(vo);
        }
        return result;
    }

    @Override
    public String getRelation(Long userId, Long targetUserId) {
        if (Objects.equals(userId, targetUserId)) {
            return RELATION_SELF;
        }
        if (isFriend(userId, targetUserId)) {
            return RELATION_FRIEND;
        }
        if (findPendingRequest(userId, targetUserId) != null) {
            return RELATION_PENDING_SENT;
        }
        if (findPendingRequest(targetUserId, userId) != null) {
            return RELATION_PENDING_RECEIVED;
        }
        return RELATION_NONE;
    }

    @Override
    public boolean isFriend(Long userId, Long otherUserId) {
        return findRelation(userId, otherUserId) != null;
    }

    @Override
    public void assertFriend(Long userId, Long otherUserId) {
        if (!isFriend(userId, otherUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你们还不是好友");
        }
    }

    /* ==================== 内部工具 ==================== */

    private Friendship findRelation(Long userId, Long otherUserId) {
        if (userId == null || otherUserId == null) {
            return null;
        }
        long min = Math.min(userId, otherUserId);
        long max = Math.max(userId, otherUserId);
        return baseMapper.selectOne(new LambdaQueryWrapper<Friendship>()
            .eq(Friendship::getUserAId, min)
            .eq(Friendship::getUserBId, max)
            .last("LIMIT 1"));
    }

    private FriendRequest findPendingRequest(Long fromUserId, Long toUserId) {
        return friendRequestMapper.selectOne(new LambdaQueryWrapper<FriendRequest>()
            .eq(FriendRequest::getFromUserId, fromUserId)
            .eq(FriendRequest::getToUserId, toUserId)
            .eq(FriendRequest::getStatus, STATUS_PENDING)
            .orderByDesc(FriendRequest::getId)
            .last("LIMIT 1"));
    }

    private List<FriendRequestVO> toRequestVOList(List<FriendRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new HashSet<>();
        requests.forEach(r -> {
            userIds.add(r.getFromUserId());
            userIds.add(r.getToUserId());
        });
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<Long, String> realNameMap = userProfileMapper.selectList(
                new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds))
            .stream().filter(p -> StringUtils.hasText(p.getRealName()))
            .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getRealName, (a, b) -> a));

        return requests.stream().map(r -> {
            FriendRequestVO vo = new FriendRequestVO();
            vo.setId(r.getId());
            vo.setFromUserId(r.getFromUserId());
            vo.setToUserId(r.getToUserId());
            vo.setMessage(r.getMessage());
            vo.setStatus(r.getStatus());
            vo.setCreateTime(r.getCreateTime());
            vo.setHandleTime(r.getHandleTime());
            // 展示信息取申请人：申请列表里关注的是「谁想加我」
            User fromUser = userMap.get(r.getFromUserId());
            if (fromUser != null) {
                vo.setNickname(StringUtils.hasText(fromUser.getNickname())
                    ? fromUser.getNickname() : fromUser.getUsername());
                vo.setAvatar(fromUser.getAvatar());
            }
            vo.setRealName(realNameMap.get(r.getFromUserId()));
            return vo;
        }).toList();
    }

    /** 事务提交后执行，避免推送了但事务回滚 */
    private void afterCommit(Runnable action) {
        TransactionUtil.afterCommit(action);
    }
}
