package com.schoolmate.ws;

/**
 * WebSocket 帧类型常量。
 *
 * <p>客户端 → 服务端（C2S）与服务端 → 客户端（S2C）分两段命名，避免混淆。
 *
 * @author Albot
 */
public final class WsFrameType {

    private WsFrameType() {
    }

    /* ---------------- 客户端 → 服务端 ---------------- */

    /** 发送聊天消息 */
    public static final String C2S_CHAT_SEND = "CHAT_SEND";
    /** 上报已读 */
    public static final String C2S_READ_ACK = "READ_ACK";
    /** 正在输入 */
    public static final String C2S_TYPING = "TYPING";
    /** 撤回消息 */
    public static final String C2S_RECALL = "RECALL";
    /** 心跳 */
    public static final String C2S_PING = "PING";

    /* ---------------- 服务端 → 客户端 ---------------- */

    /** 新消息推送 */
    public static final String S2C_CHAT_MESSAGE = "CHAT_MESSAGE";
    /** 消息发送回执（带 clientMsgId，前端据此把气泡标为已发送） */
    public static final String S2C_MESSAGE_ACK = "MESSAGE_ACK";
    /** 对方已读通知 */
    public static final String S2C_READ_NOTIFY = "READ_NOTIFY";
    /** 收到新的好友申请 */
    public static final String S2C_FRIEND_REQUEST = "FRIEND_REQUEST";
    /** 好友申请通过 */
    public static final String S2C_FRIEND_ACCEPTED = "FRIEND_ACCEPTED";
    /** 被拉入新群 */
    public static final String S2C_GROUP_INVITE = "GROUP_INVITE";
    /** 群资料变更 / 被移出 / 群解散 */
    public static final String S2C_GROUP_CHANGED = "GROUP_CHANGED";
    /** 好友上下线 */
    public static final String S2C_ONLINE_STATUS = "ONLINE_STATUS";
    /** 心跳响应 */
    public static final String S2C_PONG = "PONG";
    /** 错误帧 */
    public static final String S2C_ERROR = "ERROR";
    /** 连接建立成功 */
    public static final String S2C_CONNECTED = "CONNECTED";
}
