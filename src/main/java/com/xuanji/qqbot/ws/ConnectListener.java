package com.xuanji.qqbot.ws;

/**
 * WebSocket 连接生命周期回调（框架/WebUI/插件感知掉线与重连用）。
 * <p>
 * 回调运行在 WS 收包线程或重连线程上，实现应轻量，勿长时间阻塞。
 * 仅 websocket 接入的机器人触发；webhook 机器人不产生连接事件。
 */
public interface ConnectListener {

    /**
     * 网关就绪（首连 READY 或断线 RESUMED 完成）。
     *
     * @param sessionId 就绪后的会话 id
     */
    default void onConnected(String sessionId) {
    }

    /**
     * 连接失效（主动关闭/服务端踢下线/心跳超时/发送失败等）。
     *
     * @param reason    失效原因
     * @param closeCode 服务端 close code；非关闭帧场景为 -1
     */
    default void onDisconnected(WsCloseReason reason, int closeCode) {
    }

    /**
     * 已调度重连（指数退避，第 attempt 次）。
     *
     * @param attempt 第几次重连尝试（从 1 起）
     */
    default void onReconnecting(int attempt) {
    }
}
