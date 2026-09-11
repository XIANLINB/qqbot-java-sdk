package com.xuanji.qqbot.ws;

import com.xuanji.qqbot.api.GatewayApi;
import com.xuanji.qqbot.event.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 错误码处理参考官方 WebSocket 错误码表。
 */
public final class GatewaySupervisor implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(GatewaySupervisor.class);

    private final GatewayApi gatewayApi;
    private final Supplier<String> tokenSupplier;
    private final long intents;
    private final Events events;
    private final int[] shard;

    private final AtomicBoolean closed = new AtomicBoolean(false);
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
        this(gatewayApi, () -> accessToken, intents, events, new int[]{0, 1});
    }

    /**
     * @param gatewayApi 网关 API
     * @param tokenSupplier 每次连接获取 token
     * @param intents 事件位
     * @param events 事件总线
     */
    public GatewaySupervisor(GatewayApi gatewayApi, Supplier<String> tokenSupplier,
                             long intents, Events events) {
        this(gatewayApi, tokenSupplier, intents, events, new int[]{0, 1});
    }

    /**
     * @param gatewayApi 网关 API
     * @param tokenSupplier token 供应
     * @param intents 事件位
     * @param events 事件总线
     * @param shard 分片 [index, num]，通常 [0,1]
     */
    public GatewaySupervisor(GatewayApi gatewayApi, Supplier<String> tokenSupplier,
                             long intents, Events events, int[] shard) {
        this.gatewayApi = gatewayApi;
        this.tokenSupplier = tokenSupplier;
        this.intents = intents;
        this.events = events;
        this.shard = shard;
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

    /** 建立首连并阻塞至 READY/RESUMED。 */
    public GatewayConnection startAndAwait() {
        GatewayConnection conn = connectOnce();
        GatewayConnection.Ready ready = conn.awaitReady().join();
        if (ready != null) {
            session.set(new GatewayConnection.SessionState(ready.sessionId(), ready.seq()));
            log.info("首连 READY session={} seq={} fresh={}", ready.sessionId(), ready.seq(), ready.fresh());
        }
        attempt.set(0);
        return conn;
    }

    private GatewayConnection connectOnce() {
        String url = GatewayConnection.resolveUrl(gatewayApi);
        GatewayConnection.SessionState restore = session.get();
        boolean canResume = restore != null && restore.sessionId() != null;
        log.info("连接网关 url={} resume={}", url, canResume);
        GatewayConnection conn = GatewayConnection.open(
                url, tokenSupplier.get(), intents, events, shard, canResume ? restore : null,
                () -> scheduleReconnect()
        );
        current.set(conn);
        return conn;
    }

    private void scheduleReconnect() {
        if (closed.get()) {
            return;
        }
        GatewayConnection c = current.get();
        if (c != null) {
            session.set(c.snapshot());
        }
        if (pendingReconnect != null && !pendingReconnect.isDone()) {
            return;
        }
        int n = attempt.incrementAndGet();
        long delayMs = backoffMs(n);
        log.info("将在 {}ms 后重连（第 {} 次）", delayMs, n);
        pendingReconnect = reconnectScheduler.schedule(
                () -> reconnectWorker.submit(this::reconnectNow),
                delayMs, TimeUnit.MILLISECONDS);
    }

    private void reconnectNow() {
        if (closed.get()) {
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
            GatewayConnection.Ready ready = conn.awaitReady().orTimeout(30, TimeUnit.SECONDS).join();
            if (ready != null) {
                session.set(new GatewayConnection.SessionState(ready.sessionId(), ready.seq()));
            }
            attempt.set(0);
            log.info("网关重连成功 session={}", session.get());
        } catch (Exception e) {
            log.warn("网关重连失败: {}", e.getMessage());
            if (!closed.get()) {
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
