package com.xuanji.qqbot.ws;

/**
 * 网关连接失效原因。
 */
public enum WsCloseReason {
    /** 服务端要求重连 op=7 */
    SERVER_RECONNECT,
    /** 会话无效 op=9，需重新 Identify */
    INVALID_SESSION,
    /** 心跳连续未 ACK */
    HEARTBEAT_TIMEOUT,
    /** 发送失败 */
    SEND_FAILED,
    /** Socket 错误 */
    SOCKET_ERROR,
    /** Socket 关闭 */
    SOCKET_CLOSED
}
