package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schoolmate.common.PageResult;
import com.schoolmate.common.ResultCode;
import com.schoolmate.dto.chat.GroupCreateDTO;
import com.schoolmate.dto.chat.GroupUpdateDTO;
import com.schoolmate.entity.ChatGroup;
import com.schoolmate.entity.ChatGroupMember;
import com.schoolmate.entity.ChatSession;
import com.schoolmate.entity.ClassInfo;
import com.schoolmate.entity.ClassMember;
import com.schoolmate.entity.User;
import com.schoolmate.entity.UserProfile;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.mapper.ChatGroupMapper;
import com.schoolmate.mapper.ChatGroupMemberMapper;
import com.schoolmate.mapper.ChatSessionMapper;
import com.schoolmate.mapper.ClassInfoMapper;
import com.schoolmate.mapper.ClassMemberMapper;
import com.schoolmate.mapper.UserMapper;
import com.schoolmate.mapper.UserProfileMapper;
import com.schoolmate.service.ChatService;
import com.schoolmate.service.GroupService;
import com.schoolmate.utils.TransactionUtil;
import com.schoolmate.vo.chat.ChatGroupVO;
import com.schoolmate.vo.chat.GroupMemberVO;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 群聊服务实现。
 *
 * <p>群分两类，共用同一套底层模型，差别只在成员从哪来：
 * <ul>
 *   <li>{@code CLASS} 班级群：随班级自动创建，成员与 class_member 双向同步，不进不退</li>
 *   <li>{@code CUSTOM} 自建群：用户自由创建、拉人、退群、解散</li>
 * </ul>
 *
 * @author Albot
 */
@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<ChatGroupMapper, ChatGroup> implements GroupService {

    public static final String TYPE_CLASS = "CLASS";
    public static final String TYPE_CUSTOM = "CUSTOM";

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MEMBER = "MEMBER";

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_DISMISSED = "DISMISSED";

    @Resource
    private ChatGroupMemberMapper chatGroupMemberMapper;

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ClassInfoMapper classInfoMapper;

    @Resource
    private ClassMemberMapper classMemberMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserProfileMapper userProfileMapper;

    @Resource
    private ChatService chatService;

    @Resource
    private WsSessionRegistry wsSessionRegistry;

    /* ==================== 班级群 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long ensureClassGroup(Long classId) {
        ChatGroup exist = findByClassId(classId);
        if (exist != null) {
            return exist.getId();
        }

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }

        ChatGroup group = new ChatGroup();
        group.setGroupName(classInfo.getClassName());
        group.setGroupType(TYPE_CLASS);
        group.setClassId(classId);
        group.setOwnerId(classInfo.getOwnerId());
        group.setMemberCount(0);
        group.setMaxMember(500);
        group.setJoinMode("INVITE");
        group.setStatus(STATUS_NORMAL);
        try {
            baseMapper.insert(group);
        } catch (DuplicateKeyException e) {
            // uk_biz(group_type, class_id, deleted) 从库层保证一个班级只有一个群，
            // 并发下失败的一方回查即可，不需要额外的分布式锁
            ChatGroup again = findByClassId(classId);
            if (again == null) {
                throw e;
            }
            return again.getId();
        }

        List<Long> memberIds = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId))
            .stream().map(ClassMember::getUserId).toList();
        if (!memberIds.contains(classInfo.getOwnerId())) {
            memberIds = new ArrayList<>(memberIds);
            memberIds.add(classInfo.getOwnerId());
        }

        insertMembers(group.getId(), memberIds);
        chatService.createGroupSession(group.getId(), memberIds);
        log.info("班级群已创建：classId={}，groupId={}，成员数={}", classId, group.getId(), memberIds.size());
        return group.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncClassMember(Long classId, Long userId, boolean join) {
        ChatGroup group = findByClassId(classId);
        if (group == null) {
            // 班级群尚未创建时无需同步，后续 ensureClassGroup 会把当前成员一并补上
            return;
        }
        if (join) {
            if (findMember(group.getId(), userId) != null) {
                return;
            }
            insertMembers(group.getId(), List.of(userId));
            chatService.addSessionMembers(group.getId(), List.of(userId));
            notifyMembers(group.getId(), userId, "加入了群聊");
        } else {
            removeMemberInternal(group.getId(), userId);
            notifyMembers(group.getId(), userId, "退出了群聊");
        }
    }

    /* ==================== 自建群 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroup(Long userId, GroupCreateDTO dto) {
        ChatGroup group = new ChatGroup();
        group.setGroupName(dto.getGroupName());
        group.setGroupType(TYPE_CUSTOM);
        group.setOwnerId(userId);
        group.setMemberCount(0);
        group.setMaxMember(200);
        group.setJoinMode("INVITE");
        group.setStatus(STATUS_NORMAL);
        baseMapper.insert(group);

        List<Long> memberIds = new ArrayList<>();
        memberIds.add(userId);
        if (dto.getMemberIds() != null) {
            dto.getMemberIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(id -> !Objects.equals(id, userId))
                .forEach(memberIds::add);
        }

        insertMembers(group.getId(), memberIds);
        chatService.createGroupSession(group.getId(), memberIds);

        // 通知被拉进群的成员，让他们的会话列表立刻出现这个群
        Long sessionId = findSessionId(group.getId());
        List<Long> invited = memberIds.stream().filter(id -> !Objects.equals(id, userId)).toList();
        if (!invited.isEmpty()) {
            Map<String, Object> payload = new HashMap<>(4);
            payload.put("groupId", group.getId());
            payload.put("groupName", group.getGroupName());
            payload.put("sessionId", sessionId);
            TransactionUtil.afterCommit(() -> wsSessionRegistry.pushToUsers(invited,
                WsFrame.of(WsFrameType.S2C_GROUP_INVITE, payload), null));
        }
        return group.getId();
    }

    @Override
    public ChatGroupVO getGroupDetail(Long userId, Long groupId) {
        ChatGroup group = baseMapper.selectById(groupId);
        if (group == null || STATUS_DISMISSED.equals(group.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "群不存在或已解散");
        }
        ChatGroupMember me = findMember(groupId, userId);
        if (me == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你不在该群中");
        }
        return toGroupVO(group, me);
    }

    @Override
    public List<ChatGroupVO> listMyGroups(Long userId) {
        List<ChatGroupMember> members = chatGroupMemberMapper.selectList(
            new LambdaQueryWrapper<ChatGroupMember>().eq(ChatGroupMember::getUserId, userId));
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, ChatGroupMember> memberMap = members.stream()
            .collect(Collectors.toMap(ChatGroupMember::getGroupId, m -> m, (a, b) -> a));
        List<ChatGroup> groups = baseMapper.selectBatchIds(memberMap.keySet());
        return groups.stream()
            .filter(g -> !STATUS_DISMISSED.equals(g.getStatus()))
            .map(g -> toGroupVO(g, memberMap.get(g.getId())))
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatGroupVO updateGroup(Long userId, Long groupId, GroupUpdateDTO dto) {
        ChatGroup group = baseMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "群不存在");
        }
        ChatGroupMember me = assertCanManage(userId, groupId);

        StringBuilder change = new StringBuilder();
        if (StringUtils.hasText(dto.getGroupName()) && !dto.getGroupName().equals(group.getGroupName())) {
            change.append("群名称修改为「").append(dto.getGroupName()).append("」");
            group.setGroupName(dto.getGroupName());
        }
        if (dto.getNotice() != null) {
            if (change.length() > 0) {
                change.append("，");
            }
            change.append("群公告已更新");
            group.setNotice(dto.getNotice());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            group.setAvatar(dto.getAvatar());
        }
        baseMapper.updateById(group);

        if (change.length() > 0) {
            chatService.sendSystemMessage(findSessionId(groupId), change.toString());
        }
        return toGroupVO(group, me);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dismissGroup(Long userId, Long groupId) {
        ChatGroup group = baseMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "群不存在");
        }
        if (!Objects.equals(group.getOwnerId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有群主可以解散群");
        }
        if (TYPE_CLASS.equals(group.getGroupType())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "班级群不能解散，请通过退出班级来处理");
        }

        List<Long> memberIds = chatService.listSessionMemberIds(findSessionId(groupId));
        group.setStatus(STATUS_DISMISSED);
        baseMapper.updateById(group);
        chatGroupMemberMapper.delete(new LambdaQueryWrapper<ChatGroupMember>()
            .eq(ChatGroupMember::getGroupId, groupId));
        chatService.removeGroupSession(groupId);
        baseMapper.deleteById(groupId);

        Map<String, Object> payload = new HashMap<>(2);
        payload.put("groupId", groupId);
        payload.put("reason", "DISMISSED");
        TransactionUtil.afterCommit(() -> wsSessionRegistry.pushToUsers(memberIds,
            WsFrame.of(WsFrameType.S2C_GROUP_CHANGED, payload), null));
    }

    /* ==================== 成员管理 ==================== */

    @Override
    public PageResult<GroupMemberVO> pageMembers(Long userId, Long groupId, Long pageNum, Long pageSize) {
        assertGroupMember(userId, groupId);

        // 群主排最前，其后是管理员，最后按加入时间
        IPage<ChatGroupMember> page = chatGroupMemberMapper.selectPage(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<ChatGroupMember>()
                .eq(ChatGroupMember::getGroupId, groupId)
                .last("ORDER BY FIELD(member_role, 'OWNER', 'ADMIN', 'MEMBER'), join_time ASC"));

        List<ChatGroupMember> records = page.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), List.of());
        }

        Set<Long> userIds = records.stream().map(ChatGroupMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<Long, String> realNameMap = userProfileMapper.selectList(
                new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds))
            .stream().filter(p -> StringUtils.hasText(p.getRealName()))
            .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getRealName, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();
        List<GroupMemberVO> voList = records.stream().map(m -> {
            GroupMemberVO vo = new GroupMemberVO();
            vo.setUserId(m.getUserId());
            vo.setMemberRole(m.getMemberRole());
            vo.setGroupNickname(m.getGroupNickname());
            vo.setJoinTime(m.getJoinTime());
            vo.setRealName(realNameMap.get(m.getUserId()));
            vo.setOnline(wsSessionRegistry.isOnline(m.getUserId()));
            vo.setMuted(m.getMuteUntil() != null && m.getMuteUntil().isAfter(now));
            User u = userMap.get(m.getUserId());
            if (u != null) {
                vo.setNickname(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
                vo.setAvatar(u.getAvatar());
            }
            return vo;
        }).toList();

        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembers(Long userId, Long groupId, List<Long> userIds) {
        ChatGroup group = baseMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "群不存在");
        }
        assertCanManage(userId, groupId);

        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        // 只保留尚未入群的用户
        List<Long> toAdd = userIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .filter(id -> findMember(groupId, id) == null)
            .toList();
        if (toAdd.isEmpty()) {
            return;
        }
        if (group.getMemberCount() != null && group.getMemberCount() + toAdd.size() > group.getMaxMember()) {
            throw new BusinessException(ResultCode.CONFLICT, "群成员已达上限");
        }

        insertMembers(groupId, toAdd);
        chatService.addSessionMembers(groupId, toAdd);
        for (Long id : toAdd) {
            notifyMembers(groupId, id, "加入了群聊");
        }

        Map<String, Object> payload = new HashMap<>(3);
        payload.put("groupId", groupId);
        payload.put("groupName", group.getGroupName());
        payload.put("sessionId", findSessionId(groupId));
        TransactionUtil.afterCommit(() -> wsSessionRegistry.pushToUsers(toAdd,
            WsFrame.of(WsFrameType.S2C_GROUP_INVITE, payload), null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long userId, Long groupId, Long targetUserId) {
        // 移出自己等同于退群
        if (targetUserId == null || Objects.equals(userId, targetUserId)) {
            quitGroup(userId, groupId);
            return;
        }
        assertCanManage(userId, groupId);

        ChatGroupMember target = findMember(groupId, targetUserId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该成员不在群中");
        }
        if (ROLE_OWNER.equals(target.getMemberRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能移出群主");
        }

        removeMemberInternal(groupId, targetUserId);
        notifyMembers(groupId, targetUserId, "被移出了群聊");

        Map<String, Object> payload = new HashMap<>(2);
        payload.put("groupId", groupId);
        payload.put("reason", "KICKED");
        TransactionUtil.afterCommit(() -> wsSessionRegistry.pushToUser(targetUserId,
            WsFrame.of(WsFrameType.S2C_GROUP_CHANGED, payload)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quitGroup(Long userId, Long groupId) {
        ChatGroup group = baseMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "群不存在");
        }
        ChatGroupMember me = findMember(groupId, userId);
        if (me == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "你不在该群中");
        }
        if (TYPE_CLASS.equals(group.getGroupType())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "班级群不能主动退出，请先退出班级");
        }
        if (ROLE_OWNER.equals(me.getMemberRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "群主不能直接退群，请先解散该群");
        }

        removeMemberInternal(groupId, userId);
        notifyMembers(groupId, userId, "退出了群聊");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dismissClassGroup(Long classId) {
        ChatGroup group = findByClassId(classId);
        if (group == null) {
            return;
        }
        List<Long> memberIds = chatService.listSessionMemberIds(findSessionId(group.getId()));
        group.setStatus(STATUS_DISMISSED);
        baseMapper.updateById(group);
        chatGroupMemberMapper.delete(new LambdaQueryWrapper<ChatGroupMember>()
            .eq(ChatGroupMember::getGroupId, group.getId()));
        chatService.removeGroupSession(group.getId());
        baseMapper.deleteById(group.getId());
        log.info("班级群已随班级删除而解散：classId={}，groupId={}", classId, group.getId());

        Map<String, Object> payload = new HashMap<>(2);
        payload.put("groupId", group.getId());
        payload.put("reason", "DISMISSED");
        TransactionUtil.afterCommit(() -> wsSessionRegistry.pushToUsers(memberIds,
            WsFrame.of(WsFrameType.S2C_GROUP_CHANGED, payload), null));
    }

    @Override
    public void assertGroupMember(Long userId, Long groupId) {
        if (findMember(groupId, userId) == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你不在该群中");
        }
    }

    /* ==================== 内部工具 ==================== */

    private ChatGroup findByClassId(Long classId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<ChatGroup>()
            .eq(ChatGroup::getGroupType, TYPE_CLASS)
            .eq(ChatGroup::getClassId, classId)
            .last("LIMIT 1"));
    }

    private ChatGroupMember findMember(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            return null;
        }
        return chatGroupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMember>()
            .eq(ChatGroupMember::getGroupId, groupId)
            .eq(ChatGroupMember::getUserId, userId)
            .last("LIMIT 1"));
    }

    /** 校验群主 / 管理员权限，返回操作者的成员记录 */
    private ChatGroupMember assertCanManage(Long userId, Long groupId) {
        ChatGroupMember me = findMember(groupId, userId);
        if (me == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你不在该群中");
        }
        if (!ROLE_OWNER.equals(me.getMemberRole()) && !ROLE_ADMIN.equals(me.getMemberRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "需要群主或管理员权限");
        }
        return me;
    }

    /** 插入成员记录（不做权限校验，供内部与班级同步复用） */
    private void insertMembers(Long groupId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        ChatGroup group = baseMapper.selectById(groupId);
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            ChatGroupMember member = new ChatGroupMember();
            member.setGroupId(groupId);
            member.setUserId(userId);
            // 班级群的创建者 / 自建群的创建者都是群主
            member.setMemberRole(group != null && Objects.equals(group.getOwnerId(), userId)
                ? ROLE_OWNER : ROLE_MEMBER);
            member.setLastReadMessageId(0L);
            member.setJoinTime(LocalDateTime.now());
            try {
                chatGroupMemberMapper.insert(member);
            } catch (DuplicateKeyException e) {
                log.debug("群成员已存在，跳过：groupId={}，userId={}", groupId, userId);
            }
        }
        refreshMemberCount(groupId);
    }

    private void removeMemberInternal(Long groupId, Long userId) {
        chatGroupMemberMapper.delete(new LambdaQueryWrapper<ChatGroupMember>()
            .eq(ChatGroupMember::getGroupId, groupId)
            .eq(ChatGroupMember::getUserId, userId));
        chatService.removeSessionMember(groupId, userId);
        refreshMemberCount(groupId);
    }

    /** 成员数用实际条数重算，避免加减法在并发下算错 */
    private void refreshMemberCount(Long groupId) {
        Long count = chatGroupMemberMapper.selectCount(new LambdaQueryWrapper<ChatGroupMember>()
            .eq(ChatGroupMember::getGroupId, groupId));
        ChatGroup group = baseMapper.selectById(groupId);
        if (group != null) {
            group.setMemberCount(count == null ? 0 : count.intValue());
            baseMapper.updateById(group);
        }
    }

    private Long findSessionId(Long groupId) {
        ChatSession session = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
            .eq(ChatSession::getBizKey, "G_" + groupId)
            .last("LIMIT 1"));
        return session == null ? null : session.getId();
    }

    /** 在群里发一条系统提示，比如「XXX 加入了群聊」 */
    private void notifyMembers(Long groupId, Long actorId, String action) {
        Long sessionId = findSessionId(groupId);
        if (sessionId == null) {
            return;
        }
        ChatGroupMember actor = findMember(groupId, actorId);
        User user = userMapper.selectById(actorId);
        String name = actor != null && StringUtils.hasText(actor.getGroupNickname()) ? actor.getGroupNickname()
            : (user != null && StringUtils.hasText(user.getNickname()) ? user.getNickname()
                : (user != null ? user.getUsername() : "某位同学"));
        chatService.sendSystemMessage(sessionId, name + " " + action);
    }

    private ChatGroupVO toGroupVO(ChatGroup group, ChatGroupMember me) {
        ChatGroupVO vo = new ChatGroupVO();
        vo.setId(group.getId());
        vo.setGroupName(group.getGroupName());
        vo.setAvatar(group.getAvatar());
        vo.setGroupType(group.getGroupType());
        vo.setClassId(group.getClassId());
        vo.setOwnerId(group.getOwnerId());
        vo.setNotice(group.getNotice());
        vo.setMemberCount(group.getMemberCount());
        vo.setMaxMember(group.getMaxMember());
        vo.setJoinMode(group.getJoinMode());
        vo.setCreateTime(group.getCreateTime());
        vo.setMyRole(me == null ? null : me.getMemberRole());
        vo.setSessionId(findSessionId(group.getId()));
        return vo;
    }
}
