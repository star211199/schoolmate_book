package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmate.common.ResultCode;
import com.schoolmate.dto.chat.MessageSendDTO;
import com.schoolmate.entity.ChatGroup;
import com.schoolmate.entity.ChatGroupMember;
import com.schoolmate.entity.ChatMessage;
import com.schoolmate.entity.ChatSession;
import com.schoolmate.entity.ChatSessionUser;
import com.schoolmate.entity.User;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.mapper.ChatGroupMapper;
import com.schoolmate.mapper.ChatGroupMemberMapper;
import com.schoolmate.mapper.ChatMessageMapper;
import com.schoolmate.mapper.ChatSessionMapper;
import com.schoolmate.mapper.ChatSessionUserMapper;
import com.schoolmate.mapper.UserMapper;
import com.schoolmate.service.ChatService;
import com.schoolmate.utils.TransactionUtil;
import com.schoolmate.vo.chat.ChatMessageVO;
import com.schoolmate.vo.chat.ChatSessionVO;
import com.schoolmate.vo.chat.SessionUnreadVO;
import com.schoolmate.ws.WsFrame;
import com.schoolmate.ws.WsFrameType;
import com.schoolmate.ws.WsSessionRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 会话与消息服务实现。
 *
 * <p>私聊与群聊在这里被统一处理：会话用 sessionType 区分，消息共用一张表，
 * 前端因此可以用同一套组件渲染两种场景。
 *
 * <p>核心链路 —— 发送消息九步：
 * <ol>
 *   <li>校验会话存在</li>
 *   <li>校验发送者是会话成员</li>
 *   <li>群聊时校验是否被禁言</li>
 *   <li>clientMsgId 幂等去重（命中则只补发 ACK，不再重复扇出）</li>
 *   <li>内容 XSS 转义后落库</li>
 *   <li>更新会话的最后消息（会话列表排序依据）</li>
 *   <li>事务提交后再推送（避免事务回滚后对方已收到「幽灵消息」）</li>
 *   <li>向所有成员扇出 CHAT_MESSAGE</li>
 *   <li>给发送者回 MESSAGE_ACK，前端把气泡从「发送中」改为「已发送」</li>
 * </ol>
 *
 * @author Albot
 */
@Slf4j
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatService {

    public static final String TYPE_PRIVATE = "PRIVATE";
    public static final String TYPE_GROUP = "GROUP";

    public static final String MSG_TEXT = "TEXT";
    public static final String MSG_IMAGE = "IMAGE";
    public static final String MSG_FILE = "FILE";
    public static final String MSG_SYSTEM = "SYSTEM";
    public static final String MSG_RECALL = "RECALL";

    /** 系统消息的发送者ID */
    private static final Long SYSTEM_SENDER_ID = 0L;

    /** 允许撤回的时间窗口（分钟） */
    private static final long RECALL_WINDOW_MINUTES = 2L;

    /** 历史消息单页最大条数，防止前端传入过大的 size 拖垮数据库 */
    private static final long MAX_PAGE_SIZE = 100L;

    /**
     * 单条文本消息最大长度，与 chat_message.content 的 varchar(2000) 保持一致。
     *
     * <p>必须在 Service 层显式校验：WebSocket 通道不走 Bean Validation，
     * 若直接放行超长内容，会撞到数据库列长度限制抛出一个语焉不详的「服务端处理失败」。
     */
    private static final int MAX_CONTENT_LENGTH = 2000;

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatSessionUserMapper chatSessionUserMapper;

    @Resource
    private ChatGroupMapper chatGroupMapper;

    @Resource
    private ChatGroupMemberMapper chatGroupMemberMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private WsSessionRegistry wsSessionRegistry;

    @Resource
    private ObjectMapper objectMapper;

    /* ==================== 会话 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long getOrCreatePrivateSession(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户参数不能为空");
        }
        if (Objects.equals(userId, targetUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能和自己发起聊天");
        }
        if (userMapper.selectById(targetUserId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "对方用户不存在");
        }

        long min = Math.min(userId, targetUserId);
        long max = Math.max(userId, targetUserId);
        String bizKey = "P_" + min + "_" + max;

        ChatSession exist = findByBizKey(bizKey);
        if (exist != null) {
            return exist.getId();
        }

        ChatSession session = new ChatSession();
        session.setBizKey(bizKey);
        session.setSessionType(TYPE_PRIVATE);
        session.setTargetUserId(max);
        session.setLastMessageId(0L);
        try {
            chatSessionMapper.insert(session);
        } catch (DuplicateKeyException e) {
            // 并发下两个请求同时创建，唯一索引只放行一个；这里回查拿已存在的那条
            ChatSession again = findByBizKey(bizKey);
            if (again == null) {
                throw e;
            }
            return again.getId();
        }

        // 会话是双方共享的，但「已读位点、置顶」是各自私有的，所以两边各插一行状态
        insertSessionUser(session.getId(), min);
        insertSessionUser(session.getId(), max);
        return session.getId();
    }

    @Override
    public List<ChatSessionVO> listSessions(Long userId) {
        List<ChatSessionUser> states = chatSessionUserMapper.selectList(
            new LambdaQueryWrapper<ChatSessionUser>().eq(ChatSessionUser::getUserId, userId));
        if (states.isEmpty()) {
            return List.of();
        }

        Map<Long, ChatSessionUser> stateMap = states.stream()
            .collect(Collectors.toMap(ChatSessionUser::getSessionId, s -> s, (a, b) -> a));

        List<ChatSession> sessions = chatSessionMapper.selectBatchIds(stateMap.keySet());
        if (sessions.isEmpty()) {
            return List.of();
        }

        // 未读数：一条 GROUP BY 查出所有会话的未读，避免 N+1
        Map<Long, Long> unreadMap = baseMapper.countUnreadGroupBySession(userId).stream()
            .collect(Collectors.toMap(SessionUnreadVO::getSessionId,
                v -> v.getUnreadCount() == null ? 0L : v.getUnreadCount(), (a, b) -> a));

        // 最后一条消息（批量查，不逐个 selectById）
        List<Long> lastMsgIds = sessions.stream()
            .map(ChatSession::getLastMessageId)
            .filter(id -> id != null && id > 0L)
            .distinct()
            .toList();
        Map<Long, ChatMessage> lastMsgMap = lastMsgIds.isEmpty() ? Map.of()
            : this.listByIds(lastMsgIds).stream()
                .collect(Collectors.toMap(ChatMessage::getId, m -> m, (a, b) -> a));

        // 私聊对方用户 / 群聊群资料，也批量查
        Set<Long> targetUserIds = new HashSet<>();
        Set<Long> groupIds = new HashSet<>();
        for (ChatSession s : sessions) {
            if (TYPE_PRIVATE.equals(s.getSessionType())) {
                Long other = parsePrivateOther(s.getBizKey(), userId);
                if (other != null) {
                    targetUserIds.add(other);
                }
            } else if (s.getGroupId() != null) {
                groupIds.add(s.getGroupId());
            }
        }
        Map<Long, User> userMap = targetUserIds.isEmpty() ? Map.of()
            : userMapper.selectBatchIds(targetUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<Long, ChatGroup> groupMap = groupIds.isEmpty() ? Map.of()
            : chatGroupMapper.selectBatchIds(groupIds).stream()
                .collect(Collectors.toMap(ChatGroup::getId, g -> g, (a, b) -> a));

        List<ChatSessionVO> result = new ArrayList<>(sessions.size());
        for (ChatSession s : sessions) {
            ChatSessionUser state = stateMap.get(s.getId());
            if (state == null) {
                continue;
            }
            result.add(buildSessionVO(s, state, userId, userMap, groupMap, unreadMap, lastMsgMap));
        }

        // 排序：置顶优先，其次按最后消息时间倒序
        result.sort(Comparator
            .comparingInt((ChatSessionVO v) -> Boolean.TRUE.equals(v.getPinned()) ? 0 : 1)
            .thenComparing(v -> v.getLastMessageTime() == null ? LocalDateTime.MIN : v.getLastMessageTime(),
                Comparator.reverseOrder()));
        return result;
    }

    @Override
    public ChatSessionVO getSession(Long userId, Long sessionId) {
        assertSessionMember(userId, sessionId);
        ChatSession session = chatSessionMapper.selectById(sessionId);
        ChatSessionUser state = chatSessionUserMapper.selectOne(
            new LambdaQueryWrapper<ChatSessionUser>()
                .eq(ChatSessionUser::getSessionId, sessionId)
                .eq(ChatSessionUser::getUserId, userId));

        Map<Long, User> userMap = new HashMap<>(2);
        Map<Long, ChatGroup> groupMap = new HashMap<>(2);
        if (TYPE_PRIVATE.equals(session.getSessionType())) {
            Long other = parsePrivateOther(session.getBizKey(), userId);
            if (other != null) {
                User u = userMapper.selectById(other);
                if (u != null) {
                    userMap.put(other, u);
                }
            }
        } else if (session.getGroupId() != null) {
            ChatGroup g = chatGroupMapper.selectById(session.getGroupId());
            if (g != null) {
                groupMap.put(g.getId(), g);
            }
        }
        return buildSessionVO(session, state, userId, userMap, groupMap, Map.of(), Map.of());
    }

    /* ==================== 消息 ==================== */

    @Override
    public List<ChatMessageVO> pageMessages(Long userId, Long sessionId, Long beforeId, Long size) {
        assertSessionMember(userId, sessionId);

        long limit = (size == null || size <= 0L) ? 30L : Math.min(size, MAX_PAGE_SIZE);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getSessionId, sessionId)
            .orderByDesc(ChatMessage::getId)
            .last("LIMIT " + limit);
        if (beforeId != null && beforeId > 0L) {
            wrapper.lt(ChatMessage::getId, beforeId);
        }

        List<ChatMessage> messages = baseMapper.selectList(wrapper);
        // 查询是倒序取最新的 N 条，返回给前端要转回正序（从上到下展示）
        Collections.reverse(messages);
        return convertMessages(messages, sessionId);
    }

    @Override
    public List<ChatMessageVO> listMessagesAfter(Long userId, Long sessionId, Long afterId) {
        assertSessionMember(userId, sessionId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
            .eq(ChatMessage::getSessionId, sessionId)
            .orderByAsc(ChatMessage::getId)
            .last("LIMIT " + MAX_PAGE_SIZE);
        if (afterId != null && afterId > 0L) {
            wrapper.gt(ChatMessage::getId, afterId);
        }
        return convertMessages(baseMapper.selectList(wrapper), sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendMessage(Long senderId, MessageSendDTO dto) {
        ChatSession session = chatSessionMapper.selectById(dto.getSessionId());
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        assertSessionMember(senderId, dto.getSessionId());

        // 群聊：被禁言的成员不能发言
        if (TYPE_GROUP.equals(session.getSessionType())) {
            ChatGroupMember member = findGroupMember(session.getGroupId(), senderId);
            if (member != null && member.getMuteUntil() != null
                && member.getMuteUntil().isAfter(LocalDateTime.now())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "你已被禁言，暂时无法发送消息");
            }
        }

        String msgType = StringUtils.hasText(dto.getMsgType()) ? dto.getMsgType().toUpperCase() : MSG_TEXT;
        // 客户端只允许发这三种类型。SYSTEM 是服务端专用的（成员变动提示等），
        // 放行会让任何人伪造「系统提示」污染聊天记录；其余未知类型同样一律按文本处理，
        // 否则只要把 msgType 换个值就能绕过下面的内容转义。
        if (!MSG_TEXT.equals(msgType) && !MSG_IMAGE.equals(msgType) && !MSG_FILE.equals(msgType)) {
            msgType = MSG_TEXT;
        }
        if (MSG_TEXT.equals(msgType) && !StringUtils.hasText(dto.getContent())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "消息内容不能为空");
        }
        if (dto.getContent() != null && dto.getContent().length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                "消息内容不能超过 " + MAX_CONTENT_LENGTH + " 字");
        }
        if (dto.getExtra() != null && dto.getExtra().length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "扩展信息过长");
        }
        if (StringUtils.hasText(dto.getExtra()) && dto.getExtra().toLowerCase().contains("javascript:")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "消息内容包含非法链接");
        }

        // 幂等：同一 clientMsgId 重复提交（网络抖动重发）时直接返回已落库的那条
        if (StringUtils.hasText(dto.getClientMsgId())) {
            ChatMessage existed = baseMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, dto.getSessionId())
                .eq(ChatMessage::getSenderId, senderId)
                .eq(ChatMessage::getClientMsgId, dto.getClientMsgId())
                .last("LIMIT 1"));
            if (existed != null) {
                log.debug("命中幂等消息，直接返回：clientMsgId={}", dto.getClientMsgId());
                ChatMessageVO existedVo = toMessageVO(existed, session);
                // 幂等命中时不再向会话成员重复扇出（对方首次已收到），
                // 但必须补发一条 ACK 给发送者，否则发送方的乐观消息会永远停在「发送中」
                // —— 重发通常正是因为上一次的 ACK 丢包，这条 ACK 才是它等待的东西。
                wsSessionRegistry.pushToUser(senderId,
                    WsFrame.of(WsFrameType.S2C_MESSAGE_ACK, existedVo));
                return existedVo;
            }
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(dto.getSessionId());
        message.setSenderId(senderId);
        message.setMsgType(msgType);
        message.setContent(escapeContent(dto.getContent(), msgType));
        message.setExtra(dto.getExtra());
        message.setReplyToId(dto.getReplyToId());
        message.setClientMsgId(dto.getClientMsgId());
        message.setSendTime(LocalDateTime.now());
        this.save(message);

        // 更新会话的最后消息，会话列表的排序与预览都依赖它
        session.setLastMessageId(message.getId());
        session.setLastMessageTime(message.getSendTime());
        chatSessionMapper.updateById(session);

        ChatMessageVO vo = toMessageVO(message, session);
        List<Long> memberIds = listSessionMemberIds(dto.getSessionId());

        // 事务提交后再推送：否则一旦后续回滚，对方已经收到了这条不存在于库里的「幽灵消息」
        afterCommit(() -> {
            wsSessionRegistry.pushToUsers(memberIds, WsFrame.of(WsFrameType.S2C_CHAT_MESSAGE, vo), null);
            wsSessionRegistry.pushToUser(senderId, WsFrame.of(WsFrameType.S2C_MESSAGE_ACK, vo));
        });

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long sessionId, Long lastReadMessageId) {
        if (sessionId == null || lastReadMessageId == null || lastReadMessageId <= 0L) {
            return;
        }
        ChatSessionUser state = chatSessionUserMapper.selectOne(new LambdaQueryWrapper<ChatSessionUser>()
            .eq(ChatSessionUser::getSessionId, sessionId)
            .eq(ChatSessionUser::getUserId, userId));
        if (state == null) {
            return;
        }
        // 位点只前进不后退，避免乱序上报把已读拉回去
        if (state.getLastReadMessageId() != null && state.getLastReadMessageId() >= lastReadMessageId) {
            return;
        }
        state.setLastReadMessageId(lastReadMessageId);
        chatSessionUserMapper.updateById(state);

        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }

        if (TYPE_PRIVATE.equals(session.getSessionType())) {
            // 私聊：通知对方「我已读」以便展示已读状态
            Long other = parsePrivateOther(session.getBizKey(), userId);
            if (other != null) {
                Map<String, Object> payload = new HashMap<>(4);
                payload.put("sessionId", sessionId);
                payload.put("readerId", userId);
                payload.put("lastReadMessageId", lastReadMessageId);
                afterCommit(() -> wsSessionRegistry.pushToUser(other,
                    WsFrame.of(WsFrameType.S2C_READ_NOTIFY, payload)));
            }
        } else {
            // 群聊：同步群成员表上的已读位点，保持两处数据一致
            ChatGroupMember member = findGroupMember(session.getGroupId(), userId);
            if (member != null) {
                member.setLastReadMessageId(lastReadMessageId);
                chatGroupMemberMapper.updateById(member);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO recallMessage(Long userId, Long messageId) {
        ChatMessage message = this.getById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
        if (!Objects.equals(message.getSenderId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能撤回自己发送的消息");
        }
        if (MSG_SYSTEM.equals(message.getMsgType())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "系统消息不能撤回");
        }
        if (message.getSendTime() != null
            && Duration.between(message.getSendTime(), LocalDateTime.now()).toMinutes() >= RECALL_WINDOW_MINUTES) {
            throw new BusinessException(ResultCode.FORBIDDEN, "超过 2 分钟的消息不能撤回");
        }

        // 撤回不删记录：硬删会破坏「已读位点 = 消息ID」的连续性，导致未读数算错
        message.setMsgType(MSG_RECALL);
        message.setContent(null);
        message.setExtra(null);
        this.updateById(message);

        ChatSession session = chatSessionMapper.selectById(message.getSessionId());
        ChatMessageVO vo = toMessageVO(message, session);
        List<Long> memberIds = listSessionMemberIds(message.getSessionId());
        afterCommit(() -> wsSessionRegistry.pushToUsers(memberIds,
            WsFrame.of(WsFrameType.S2C_CHAT_MESSAGE, vo), null));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendSystemMessage(Long sessionId, String content) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return null;
        }
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderId(SYSTEM_SENDER_ID);
        message.setMsgType(MSG_SYSTEM);
        message.setContent(content);
        message.setSendTime(LocalDateTime.now());
        this.save(message);

        session.setLastMessageId(message.getId());
        session.setLastMessageTime(message.getSendTime());
        chatSessionMapper.updateById(session);

        ChatMessageVO vo = toMessageVO(message, session);
        List<Long> memberIds = listSessionMemberIds(sessionId);
        afterCommit(() -> wsSessionRegistry.pushToUsers(memberIds,
            WsFrame.of(WsFrameType.S2C_CHAT_MESSAGE, vo), null));
        return vo;
    }

    /* ==================== 供群聊模块调用 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroupSession(Long groupId, List<Long> memberIds) {
        String bizKey = "G_" + groupId;
        ChatSession exist = findByBizKey(bizKey);
        if (exist != null) {
            if (memberIds != null && !memberIds.isEmpty()) {
                addSessionMembers(groupId, memberIds);
            }
            return exist.getId();
        }

        ChatSession session = new ChatSession();
        session.setBizKey(bizKey);
        session.setSessionType(TYPE_GROUP);
        session.setGroupId(groupId);
        session.setLastMessageId(0L);
        try {
            chatSessionMapper.insert(session);
        } catch (DuplicateKeyException e) {
            ChatSession again = findByBizKey(bizKey);
            if (again == null) {
                throw e;
            }
            return again.getId();
        }

        if (memberIds != null && !memberIds.isEmpty()) {
            addSessionMembers(groupId, memberIds);
        }
        return session.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSessionMembers(Long groupId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        ChatSession session = findByBizKey("G_" + groupId);
        if (session == null) {
            return;
        }
        // 一次查出已存在的成员，避免逐个 selectCount
        Set<Long> existed = chatSessionUserMapper.selectList(new LambdaQueryWrapper<ChatSessionUser>()
                .eq(ChatSessionUser::getSessionId, session.getId()))
            .stream().map(ChatSessionUser::getUserId).collect(Collectors.toSet());

        for (Long userId : userIds) {
            if (userId == null || existed.contains(userId)) {
                continue;
            }
            insertSessionUser(session.getId(), userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSessionMember(Long groupId, Long userId) {
        ChatSession session = findByBizKey("G_" + groupId);
        if (session == null) {
            return;
        }
        chatSessionUserMapper.delete(new LambdaQueryWrapper<ChatSessionUser>()
            .eq(ChatSessionUser::getSessionId, session.getId())
            .eq(ChatSessionUser::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeGroupSession(Long groupId) {
        ChatSession session = findByBizKey("G_" + groupId);
        if (session == null) {
            return;
        }
        chatSessionUserMapper.delete(new LambdaQueryWrapper<ChatSessionUser>()
            .eq(ChatSessionUser::getSessionId, session.getId()));
        chatSessionMapper.deleteById(session.getId());
    }

    @Override
    public void assertSessionMember(Long userId, Long sessionId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!listSessionMemberIds(sessionId).contains(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你不在该会话中");
        }
    }

    @Override
    public List<Long> listSessionMemberIds(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return List.of();
        }
        if (TYPE_PRIVATE.equals(session.getSessionType())) {
            // 私聊成员直接从 bizKey 解析，无需查库
            String[] parts = session.getBizKey().split("_");
            if (parts.length == 3) {
                try {
                    return List.of(Long.parseLong(parts[1]), Long.parseLong(parts[2]));
                } catch (NumberFormatException e) {
                    log.warn("会话 bizKey 格式异常：{}", session.getBizKey());
                }
            }
            return List.of();
        }
        if (session.getGroupId() == null) {
            return List.of();
        }
        return chatGroupMemberMapper.selectList(new LambdaQueryWrapper<ChatGroupMember>()
                .eq(ChatGroupMember::getGroupId, session.getGroupId()))
            .stream().map(ChatGroupMember::getUserId).toList();
    }

    /* ==================== 内部工具 ==================== */

    /** 事务提交后执行，避免「推送成功但事务回滚」导致对方收到幽灵消息 */
    private void afterCommit(Runnable action) {
        TransactionUtil.afterCommit(action);
    }

    private ChatSession findByBizKey(String bizKey) {
        return chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
            .eq(ChatSession::getBizKey, bizKey));
    }

    private void insertSessionUser(Long sessionId, Long userId) {
        ChatSessionUser state = new ChatSessionUser();
        state.setSessionId(sessionId);
        state.setUserId(userId);
        state.setLastReadMessageId(0L);
        state.setPinned(0);
        state.setMuted(0);
        try {
            chatSessionUserMapper.insert(state);
        } catch (DuplicateKeyException e) {
            log.debug("会话成员状态已存在，跳过：sessionId={}，userId={}", sessionId, userId);
        }
    }

    private ChatGroupMember findGroupMember(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            return null;
        }
        return chatGroupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMember>()
            .eq(ChatGroupMember::getGroupId, groupId)
            .eq(ChatGroupMember::getUserId, userId)
            .last("LIMIT 1"));
    }

    /**
     * 从私聊会话的 bizKey 中解析出「对方」的用户ID。
     *
     * <p>bizKey 形如 {@code P_2_5}（小ID在前），当前用户是 2 则对方是 5。
     *
     * @param me 当前用户ID；传 null 时返回较小的那个ID（仅用于取成员列表）
     */
    private Long parsePrivateOther(String bizKey, Long me) {
        if (!StringUtils.hasText(bizKey) || !bizKey.startsWith("P_")) {
            return null;
        }
        String[] parts = bizKey.split("_");
        if (parts.length != 3) {
            return null;
        }
        try {
            long a = Long.parseLong(parts[1]);
            long b = Long.parseLong(parts[2]);
            if (me == null) {
                return a;
            }
            return a == me.longValue() ? b : a;
        } catch (NumberFormatException e) {
            log.warn("私聊会话 bizKey 解析失败：{}", bizKey);
            return null;
        }
    }

    /**
     * 消息内容落库前转义，作为 XSS 的兜底防线。
     *
     * <p><b>为什么不能只转 TEXT：</b>前端聊天气泡用 {@code v-html} 渲染（为了保留换行），
     * 而 v-html 的渲染分支只区分 SYSTEM / RECALL / 其他，IMAGE、FILE 与 TEXT 走的是
     * 同一段 {@code v-html}。此前只对 TEXT 转义，于是把 msgType 改成 IMAGE 再把
     * HTML 塞进 content，就能在对方浏览器里执行脚本 —— 一条存储型 XSS。
     * 所以：**客户端可提交的消息类型都必须转义**，只有服务端生成的 SYSTEM 例外
     * （它在前端用插值渲染，且内容由服务端拼接）。
     */
    private String escapeContent(String raw, String msgType) {
        if (!StringUtils.hasText(raw) || MSG_SYSTEM.equals(msgType)) {
            return raw;
        }
        return HtmlUtils.htmlEscape(raw);
    }

    private ChatSessionVO buildSessionVO(ChatSession session, ChatSessionUser state, Long userId,
                                         Map<Long, User> userMap, Map<Long, ChatGroup> groupMap,
                                         Map<Long, Long> unreadMap, Map<Long, ChatMessage> lastMsgMap) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setSessionType(session.getSessionType());
        vo.setBizKey(session.getBizKey());
        vo.setLastMessageId(session.getLastMessageId());
        vo.setLastMessageTime(session.getLastMessageTime());
        vo.setUnreadCount(unreadMap.getOrDefault(session.getId(), 0L));
        vo.setPinned(state != null && state.getPinned() != null && state.getPinned() == 1);
        vo.setMuted(state != null && state.getMuted() != null && state.getMuted() == 1);

        if (TYPE_PRIVATE.equals(session.getSessionType())) {
            Long other = parsePrivateOther(session.getBizKey(), userId);
            vo.setTargetUserId(other);
            User user = other == null ? null : userMap.get(other);
            if (user != null) {
                vo.setTitle(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
                vo.setAvatar(user.getAvatar());
            }
            vo.setOnline(wsSessionRegistry.isOnline(other));
        } else {
            vo.setGroupId(session.getGroupId());
            ChatGroup group = session.getGroupId() == null ? null : groupMap.get(session.getGroupId());
            if (group != null) {
                vo.setTitle(group.getGroupName());
                vo.setAvatar(group.getAvatar());
                vo.setMemberCount(group.getMemberCount());
            }
        }

        ChatMessage last = session.getLastMessageId() == null ? null : lastMsgMap.get(session.getLastMessageId());
        vo.setLastMessagePreview(buildPreview(last, session, userId));
        return vo;
    }

    /** 会话列表的最后一条消息摘要 */
    private String buildPreview(ChatMessage message, ChatSession session, Long viewerId) {
        if (message == null) {
            return "";
        }
        String prefix = "";
        // 群聊里标注发送者，让人一眼看出是谁说的
        if (session != null && TYPE_GROUP.equals(session.getSessionType())
            && !MSG_SYSTEM.equals(message.getMsgType())) {
            if (Objects.equals(message.getSenderId(), viewerId)) {
                prefix = "我：";
            } else {
                User sender = userMapper.selectById(message.getSenderId());
                if (sender != null) {
                    String name = StringUtils.hasText(sender.getNickname()) ? sender.getNickname() : sender.getUsername();
                    prefix = name + "：";
                }
            }
        }

        String body = switch (message.getMsgType()) {
            case MSG_SYSTEM -> message.getContent() == null ? "" : message.getContent();
            case MSG_RECALL -> "消息已撤回";
            case MSG_IMAGE -> "[图片]";
            case MSG_FILE -> "[文件]";
            default -> message.getContent() == null ? "" : message.getContent();
        };
        body = HtmlUtils.htmlUnescape(body);
        if (body.length() > 30) {
            body = body.substring(0, 30) + "...";
        }
        return prefix + body;
    }

    private List<ChatMessageVO> convertMessages(List<ChatMessage> messages, Long sessionId) {
        if (messages.isEmpty()) {
            return List.of();
        }
        Set<Long> senderIds = messages.stream()
            .map(ChatMessage::getSenderId)
            .filter(id -> id != null && id > 0L)
            .collect(Collectors.toSet());
        Map<Long, User> userMap = senderIds.isEmpty() ? Map.of()
            : userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        ChatSession session = chatSessionMapper.selectById(sessionId);
        Map<Long, ChatGroupMember> memberMap = new HashMap<>();
        if (session != null && TYPE_GROUP.equals(session.getSessionType()) && session.getGroupId() != null
            && !senderIds.isEmpty()) {
            chatGroupMemberMapper.selectList(new LambdaQueryWrapper<ChatGroupMember>()
                    .eq(ChatGroupMember::getGroupId, session.getGroupId())
                    .in(ChatGroupMember::getUserId, senderIds))
                .forEach(m -> memberMap.put(m.getUserId(), m));
        }

        return messages.stream()
            .map(m -> assembleMessageVO(m, userMap.get(m.getSenderId()), memberMap.get(m.getSenderId())))
            .toList();
    }

    private ChatMessageVO toMessageVO(ChatMessage message, ChatSession session) {
        User sender = message.getSenderId() == null || message.getSenderId() <= 0L
            ? null : userMapper.selectById(message.getSenderId());
        ChatGroupMember member = null;
        if (session != null && TYPE_GROUP.equals(session.getSessionType()) && sender != null) {
            member = findGroupMember(session.getGroupId(), sender.getId());
        }
        return assembleMessageVO(message, sender, member);
    }

    private ChatMessageVO assembleMessageVO(ChatMessage message, User sender, ChatGroupMember member) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setSenderId(message.getSenderId());
        vo.setMsgType(message.getMsgType());
        vo.setContent(message.getContent());
        vo.setExtra(message.getExtra());
        vo.setReplyToId(message.getReplyToId());
        vo.setClientMsgId(message.getClientMsgId());
        vo.setSendTime(message.getSendTime());
        vo.setExtraImages(parseImages(message.getExtra()));

        if (sender != null) {
            String name = StringUtils.hasText(sender.getNickname()) ? sender.getNickname() : sender.getUsername();
            vo.setSenderNickname(name);
            vo.setSenderAvatar(sender.getAvatar());
            // 群聊优先展示群昵称
            vo.setSenderDisplayName(member != null && StringUtils.hasText(member.getGroupNickname())
                ? member.getGroupNickname() : name);
        }
        return vo;
    }

    /** 从 extra JSON 中取出图片地址列表，格式约定为 {"images":["/upload/a.png"]} */
    private List<String> parseImages(String extra) {
        if (!StringUtils.hasText(extra)) {
            return List.of();
        }
        try {
            Map<?, ?> map = objectMapper.readValue(extra, Map.class);
            Object images = map.get("images");
            if (images instanceof List<?> list) {
                return list.stream().filter(Objects::nonNull).map(Object::toString).toList();
            }
        } catch (Exception e) {
            log.debug("extra 解析失败，忽略：{}", extra);
        }
        return List.of();
    }
}
