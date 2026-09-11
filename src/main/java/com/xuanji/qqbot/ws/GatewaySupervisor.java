package com.xuanji.qqbot.ws;

import com.xuanji.qqbot.api.GatewayApi;
import com.xuanji.qqbot.event.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 网关连接守护。
 * <p>
 * 断线/心跳超时/错误后指数退避重连；有 session_id+seq 时 Resume，否则 Identify。
 * 重连会重新取 access_token，避免长期运行后凭证过期。
 * <p>
 * 按官方 WebSocket 错误码表决策（见 decide）：
 * 4009/4008 可 Resume；4006/4007/4900~4913 丢弃会话改 Identify；
 * 4914/4915（下架/封禁）及 4001/4002/4010~4014 停止重连。
 */
public final class GatewaySupervisor implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(GatewaySupervisor.class);
    /** READY 等待超时 */
    private static final long READY_TIMEOUT_SECONDS = 30;

    private final GatewayApi gatewayApi;
    private final Supplier<String> tokenSupplier;
    private final long intents;
    private final Events events;
    private final int[] shard;
    /** 日志标识基底：websocket/appId */
    private final String botTagBase;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicInteger attempt = new AtomicInteger(0);
    private final AtomicReference<GatewayConnection.SessionState> session =
            new AtomicReference<>(new GatewayConnection.SessionState(null, -1));
    private final AtomicReference<GatewayConnection> current = new AtomicReference<>();
    private final ScheduledExecutorService reconnectScheduler;
    private final ExecutorService reconnectWorker;
    private volatile ScheduledFuture<?> pendingReconnect;

    /**
     * @param gatewayApi 网关 API
     * @param accessToken 固定 token（不推荐长期用）
     * @param intents 事件位
     * @param events 事件总线
     */
    public GatewaySupervisor(GatewayApi gatewayApi, String accessToken, long intents, Events events) {
        this(gatewayApi, () -> accessToken, intents, events, new int[]{0, 1}, "websocket");
    }

    /**
     * @param gatewayApi 网关 API
     * @param tokenSupplier 每次连接获取 token
     * @param intents 事件位
     * @param events 事件总线
     */
    public GatewaySupervisor(GatewayApi gatewayApi, Supplier<String> tokenSupplier,
                             long intents, Events events) {
        this(gatewayApi, tokenSupplier, intents, events, new int[]{0, 1}, "websocket");
    }

    /**
     * @param gatewayApi 网关 API
     * @param tokenSupplier token 供应
     * @param intents 事件位
     * @param events 事件总线
     * @param shard 分片 [index, num]，通常 [0,1]
     * @param botTagBase 日志标识基底（websocket/appId）
     */
    public GatewaySupervisor(GatewayApi gatewayApi, Supplier<String> tokenSupplier,
                             long intents, Events events, int[] shard, String botTagBase) {
        this.gatewayApi = gatewayApi;
        this.tokenSupplier = tokenSupplier;
        this.intents = intents;
        this.events = events;
        this.shard = shard;
        this.botTagBase = botTagBase;
        this.reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Bot-ws-reconnect-timer");
            t.setDaemon(true);
            return t;
        });
        this.reconnectWorker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Bot-ws-reconnect");
            t.setDaemon(true);
            return t;
        });
    }

    /** 官方 close code → 重连策略。 */
    enum Action {
        /** 保持会话，Resume 续传 */
        RESUME,
        /** 丢弃会话，重新 Identify */
        IDENTIFY,
        /** 停止重连（配置/权限/封禁类错误，重试无意义） */
        STOP
    }

    /**
     * 按官方 WebSocket 错误码表决策。
     *
     * @param closeCode 服务端关闭码，-1 表示无（op7/op9/心跳超时/网络错误）
     * @return RESUME / IDENTIFY / STOP
     */
    static Action decide(int closeCode) {
        if (closeCode < 0) {
            return Action.RESUME;
        }
        return switch (closeCode) {
            case 4009, 4008 -> Action.RESUME;
            case 4006, 4007 -> Action.IDENTIFY;
            case 4914, 4915, 4001, 4002, 4010, 4011, 4012, 4013, 4014 -> Action.STOP;
            default -> closeCode >= 4900 && closeCode <= 4913 ? Action.IDENTIFY : Action.RESUME;
        };
    }

    private static String describe(int closeCode) {
        return switch (closeCode) {
            case 4001 -> "无效的 opcode";
            case 4002 -> "无效的 payload";
            case 4006 -> "无效的 session id";
            case 4007 -> "seq 错误";
            case 4008 -> "发送过快";
            case 4009 -> "连接过期";
            case 4010 -> "无效的 shard";
            case 4011 -> "guild 过多需分片";
            case 4012 -> "无效的 version";
            case 4013 -> "无效的 intent";
            case 4014 -> "intent 无权限";
            case 4914 -> "机器人已下架（仅沙箱可用）";
            case 4915 -> "机器人已封禁";
            default -> "";
        };
    }

    /** 建立首连并阻塞至 READY/RESUMED（30s 超时）；失败抛异常（fail-fast）。 */
    public GatewayConnection startAndAwait() {
        GatewayConnection conn = connectOnce();
        if (!awaitReady(conn)) {
            throw new com.xuanji.qqbot.exception.QqBotException("网关就绪失败（30s 超时）");
        }
        return conn;
    }

    /**
     * 建立首连但不阻塞：失败/超时进入后台退避重试，应用启动不被网络状况拖死。
     *
     * @return 连接（可能随后就绪）；创建失败返回 null（重连调度已安排）
     */
    public GatewayConnection startBackground() {
        try {
            GatewayConnection conn = connectOnce();
            awaitReady(conn);
            return conn;
        } catch (Exception e) {
            log.warn("[{}] 首连失败，转入后台重试: {}", botTagBase, e.getMessage());
            scheduleReconnect();
            return null;
        }
    }

    /** 等待就绪（30s 超时）；成功刷新 session，失败标记连接死亡并触发重连调度。 */
    private boolean awaitReady(GatewayConnection conn) {
        try {
            GatewayConnection.Ready r = conn.awaitReady()
                    .orTimeout(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
            session.set(new GatewayConnection.SessionState(r.sessionId(), r.seq()));
            attempt.set(0);
            log.info("[{}] 网关就绪 session={} seq={} fresh={}",
                    botTagBase, r.sessionId(), r.seq(), r.fresh());
            return true;
        } catch (Exception e) {
            log.warn("[{}] 网关就绪失败: {}", botTagBase, e.getMessage());
            try {
                conn.markDead(WsCloseReason.READY_TIMEOUT);
            } catch (Exception ignored) {
                // 忽略
            }
            return false;
        }
    }

    private GatewayConnection connectOnce() {
        String url = GatewayConnection.resolveUrl(gatewayApi);
        GatewayConnection.SessionState restore = session.get();
        boolean canResume = restore != null && restore.sessionId() != null;
        log.info("[{}] 连接网关 url={} resume={}", botTagBase, url, canResume);
        GatewayConnection conn = GatewayConnection.open(
                url, tokenSupplier.get(), intents, events, shard, canResume ? restore : null,
                botTagBase,
                this::onDead
        );
        current.set(conn);
        return conn;
    }

    private void onDead(GatewayConnection.Disconnect d) {
        if (closed.get()) {
            return;
        }
        Action action = decide(d.closeCode());
        if (action == Action.STOP) {
            stopped.set(true);
            log.error("[{}] 连接被网关拒绝（close={} {}），停止重连，请检查机器人状态与配置",
                    botTagBase, d.closeCode(), describe(d.closeCode()));
            return;
        }
        if (action == Action.IDENTIFY) {
            log.warn("[{}] close={} {}，丢弃会话，重连后重新 Identify",
                    botTagBase, d.closeCode(), describe(d.closeCode()));
            session.set(new GatewayConnection.SessionState(null, -1));
        }
        GatewayConnection c = current.get();
        if (c != null) {
            session.set(c.snapshot());
        }
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (closed.get() || stopped.get()) {
            return;
        }
        if (pendingReconnect != null && !pendingReconnect.isDone()) {
            return;
        }
        int n = attempt.incrementAndGet();
        long delayMs = backoffMs(n);
        log.info("[{}] 将在 {}ms 后重连（第 {} 次）", botTagBase, delayMs, n);
        pendingReconnect = reconnectScheduler.schedule(
                () -> reconnectWorker.submit(this::reconnectNow),
                delayMs, TimeUnit.MILLISECONDS);
    }

    private void reconnectNow() {
        if (closed.get() || stopped.get()) {
            return;
        }
        GatewayConnection old = current.getAndSet(null);
        if (old != null) {
            try {
                old.close();
            } catch (Exception ignored) {
                // 忽略
            }
        }
        try {
            GatewayConnection conn = connectOnce();
            awaitReady(conn);
        } catch (Exception e) {
            log.warn("[{}] 网关重连失败: {}", botTagBase, e.getMessage());
            if (!closed.get() && !stopped.get()) {
                scheduleReconnect();
            }
        }
    }

    static long backoffMs(int attempt) {
        int exp = Math.min(Math.max(attempt - 1, 0), 6);
        long base = 1000L << exp;
        long delay = Math.min(base, 60_000L);
        return delay + (long) (Math.random() * 300);
    }

    /**
     * @return 当前连接，可能为 null
     */
    public GatewayConnection current() {
        return current.get();
    }

    /**
     * @return 用于 Resume 的 session_id + seq 快照
     */
    public GatewayConnection.SessionState session() {
        return session.get();
    }

    /**
     * @return 当前连接状态
     */
    public ConnectState state() {
        if (closed.get() || stopped.get()) {
            return ConnectState.CLOSED;
        }
        GatewayConnection c = current.get();
        if (c == null || c.isClosed()) {
            return ConnectState.RECONNECTING;
        }
        CompletableFuture<GatewayConnection.Ready> r = c.awaitReady();
        if (r.isDone() && !r.isCompletedExceptionally()) {
            return ConnectState.CONNECTED;
        }
        return ConnectState.CONNECTING;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (pendingReconnect != null) {
            pendingReconnect.cancel(false);
        }
        GatewayConnection c = current.getAndSet(null);
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
                // 忽略
            }
        }
        reconnectScheduler.shutdownNow();
        reconnectWorker.shutdownNow();
    }
}
