package com.schoolmate.service;

import com.schoolmate.dto.chat.MessageSendDTO;
import com.schoolmate.vo.chat.ChatMessageVO;
import com.schoolmate.vo.chat.ChatSessionVO;

import java.util.List;

/**
 * 会话与消息服务。
 *
 * <p>私聊与群聊在本服务内被统一处理：会话表用 sessionType 区分，消息表两者共用。
 *
 * @author Albot
 */
public interface ChatService {

    /* ---------------- 会话 ---------------- */

    /**
     * 获取或创建两人之间的私聊会话（幂等）。
     *
     * <p>bizKey 固定为 {@code P_{小id}_{大id}}，配合唯一索引保证不会重复创建。
     */
    Long getOrCreatePrivateSession(Long userId, Long targetUserId);

    /** 我的会话列表（含未读数、最后一条消息摘要，按置顶 + 最后消息时间排序） */
    List<ChatSessionVO> listSessions(Long userId);

    /** 会话详情，会校验当前用户是否为该会话成员 */
    ChatSessionVO getSession(Long userId, Long sessionId);

    /* ---------------- 消息 ---------------- */

    /**
     * 历史消息分页。
     *
     * @param beforeId 游标：查询 id 小于该值的消息；为空则取最新一页（聊天记录向上翻页）
     */
    List<ChatMessageVO> pageMessages(Long userId, Long sessionId, Long beforeId, Long size);

    /**
     * 增量拉取（重连后补齐断线期间的消息）。
     *
     * @param afterId 拉取 id 大于该值的消息
     */
    List<ChatMessageVO> listMessagesAfter(Long userId, Long sessionId, Long afterId);

    /**
     * 发送消息：校验成员身份与禁言状态 → 幂等检查 → 落库 → 更新会话 → 扇出推送 → 回执。
     *
     * @return 落库后的消息 VO（含真实 messageId）
     */
    ChatMessageVO sendMessage(Long senderId, MessageSendDTO dto);

    /** 上报已读位点 */
    void markRead(Long userId, Long sessionId, Long lastReadMessageId);

    /** 撤回消息（仅发送者本人，限 2 分钟内） */
    ChatMessageVO recallMessage(Long userId, Long messageId);

    /** 发送一条系统提示消息（如「XXX 加入了群聊」），不触发未读 */
    ChatMessageVO sendSystemMessage(Long sessionId, String content);

    /* ---------------- 供群聊模块调用 ---------------- */

    /** 为新建的群创建会话，并为所有成员初始化会话状态 */
    Long createGroupSession(Long groupId, List<Long> memberIds);

    /** 群新增成员时，同步为其创建会话状态 */
    void addSessionMembers(Long groupId, List<Long> userIds);

    /** 成员退群时，从会话中移除其会话状态 */
    void removeSessionMember(Long groupId, Long userId);

    /** 群解散时，移除所有成员的会话状态 */
    void removeGroupSession(Long groupId);

    /** 校验用户是否为会话成员 */
    void assertSessionMember(Long userId, Long sessionId);

    /** 会话的全部成员用户ID */
    List<Long> listSessionMemberIds(Long sessionId);
}
