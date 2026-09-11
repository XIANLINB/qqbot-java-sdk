package com.xuanji.qqbot.ws;

/**
 * WebSocket 网关连接状态。
 */
public enum ConnectState {
    /** 未发起连接（含 webhook 接入的机器人） */
    NOT_CONNECTED,
    /** 连接建立中（握手/鉴权） */
    CONNECTING,
    /** 已连接且 READY/RESUMED 完成 */
    CONNECTED,
    /** 连接失效，正在退避重连 */
    RECONNECTING,
    /** 已关闭或被网关拒绝，不再重连 */
    CLOSED
}
