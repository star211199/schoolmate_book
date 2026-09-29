package com.schoolmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.schoolmate.entity.ChatMessage;
import com.schoolmate.vo.chat.SessionUnreadVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天消息 Mapper。
 *
 * @author Albot
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 一次性统计当前用户所有会话的未读数。
     *
     * <p>会话列表如果有几十个会话，逐个 COUNT 会产生 N+1 查询。
     * 这里借助 chat_session_user.last_read_message_id 与消息表做关联，
     * 用一条 GROUP BY 语句把所有会话的未读数一次性算出来。
     *
     * <p>注意：SQL 中用 {@code !=} 而非 {@code <>}，避免 MyBatis 注解解析时的转义问题。
     *
     * @param userId 当前用户ID
     * @return 每个会话的未读条数
     */
    @Select("SELECT m.session_id AS sessionId, COUNT(*) AS unreadCount "
        + "FROM chat_message m "
        + "JOIN chat_session_user su ON su.session_id = m.session_id "
        + "  AND su.user_id = #{userId} AND su.deleted = 0 "
        + "WHERE m.deleted = 0 "
        + "  AND m.id > su.last_read_message_id "
        + "  AND m.sender_id != #{userId} "
        + "GROUP BY m.session_id")
    List<SessionUnreadVO> countUnreadGroupBySession(@Param("userId") Long userId);
}
