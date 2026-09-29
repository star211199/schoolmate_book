package com.schoolmate.vo.chat;

import lombok.Data;

/**
 * 会话未读数统计结果（配合 ChatMessageMapper 的 GROUP BY 查询使用）。
 *
 * @author Albot
 */
@Data
public class SessionUnreadVO {

    /** 会话ID */
    private Long sessionId;

    /** 该会话的未读条数 */
    private Long unreadCount;
}
