package com.xuanji.qqbot.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.xuanji.qqbot.api.GatewayApi;
import com.xuanji.qqbot.event.Events;
import com.xuanji.qqbot.exception.QqBotException;
import com.xuanji.qqbot.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 单次网关 WebSocket 连接：Hello → Identify/Resume → Ready；心跳。
 * 断线与错误码处理交给 {@link GatewaySupervisor}。
 */
public final class GatewayConnection implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(GatewayConnection.class);

    private final String accessToken;
    private final long intents;
    private final Events events;
    private final int[] shard;
    private final WebSocket webSocket;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean readyFired = new AtomicBoolean(false);
    private final AtomicLong seq = new AtomicLong(-1);
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> heartbeat = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> ackWatchdog = new AtomicReference<>();
    private final AtomicBoolean ackReceived = new AtomicBoolean(true);
    private final AtomicInteger reconnectHint = new AtomicInteger(0);
    private final CompletableFuture<Ready> ready = new CompletableFuture<>();
    private final Runnable onDead;
    private final boolean resumeCapable;
    private volatile long heartbeatIntervalMs = 45_000L;
    private volatile WebSocket currentWs;

    GatewayConnection(String accessToken, long intents, Events events, int[] shard,
                      WebSocket webSocket, boolean resumeCapable, Runnable onDead) {
        this.accessToken = accessToken;
        this.intents = intents;
        this.events = events;
        this.shard = shard;
        this.currentWs = webSocket;
        this.webSocket = webSocket;
        this.resumeCapable = resumeCapable;
        this.onDead = onDead;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Bot-ws-heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    static GatewayConnection open(String wssUrl, String accessToken, long intents, Events events,
                                  int[] shard, SessionState restore, Runnable onDead) {
        HttpClient client = HttpClient.newBuilder().build();
        ListenerHolder holder = new ListenerHolder();
        CompletableFuture<WebSocket> future = client.newWebSocketBuilder()
                .buildAsync(URI.create(wssUrl), holder);
        WebSocket ws;
        try {
            ws = future.get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new QqBotException("websocket connect failed: " + e.getMessage(), e);
        }
        boolean resume = restore != null && restore.sessionId() != null;
        GatewayConnection conn = new GatewayConnection(accessToken, intents, events, shard, ws, resume, onDead);
        if (restore != null && restore.sessionId() != null) {
            conn.sessionId.set(restore.sessionId());
            conn.seq.set(restore.seq());
        }
        holder.bind(conn);
        return conn;
    }

    static GatewayConnection connect(GatewayApi gateway, String accessToken, long intents, Events events) {
        return open(resolveUrl(gateway), accessToken, intents, events, new int[]{0, 1}, null, () -> {
        });
    }

    static String resolveUrl(GatewayApi gateway) {
        GatewayApi.GatewayUrl url = gateway.gateway();
        if (url == null || url.url() == null || url.url().isBlank()) {
            throw new QqBotException("网关地址为空");
        }
        return url.url();
    }

    void onText(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        JsonNode root;
        try {
            root = Json.mapper().readTree(text);
        } catch (Exception e) {
            log.debug("ws json parse error: {}", e.getMessage());
            return;
        }
        int op = root.path("op").asInt(-1);
        // 打印官方文档完整 envelope：id / op / s / t / d（见 payload.html）
        if (op == 0) {
            String t = root.path("t").asText("");
            if (!"READY".equals(t) && !"RESUMED".equals(t)) {
                log.info("========== 原始 WS 完整报文 BEGIN ==========");
                log.info("{}", pretty(root));
                log.info("========== 原始 WS 完整报文 END t={} ==========", t);
            }
        }
        switch (op) {
            case 10 -> {
                heartbeatIntervalMs = root.path("d").path("heartbeat_interval").asLong(45_000L);
                log.info("收到 Hello heartbeat_interval={}ms，发送 {}", heartbeatIntervalMs,
                        (resumeCapable && sessionId.get() != null) ? "Resume" : "Identify");
                if (resumeCapable && sessionId.get() != null) {
                    sendResume();
                } else {
                    sendIdentify();
                }
                startHeartbeat();
            }
            case 0 -> onDispatch(root, text);
            case 7 -> {
                // 官方合法行为：通知客户端重连；用 info 降低噪音
                log.info("服务端要求重连 (op=7)，将自动重连");
                markDead(WsCloseReason.SERVER_RECONNECT);
            }
            case 9 -> {
                log.warn("会话无效 (op=9)，需重新 Identify");
                sessionId.set(null);
                markDead(WsCloseReason.INVALID_SESSION);
            }
            case 11 -> {
                ackReceived.set(true);
            }
            default -> log.info("收到 WS op={} t={} d={}", op, root.path("t").asText(null),
                    truncate(root.path("d").toString(), 200));
        }
    }

    private void onDispatch(JsonNode root, String rawText) {
        long s = root.path("s").asLong(-1);
        if (s >= 0) {
            seq.set(s);
        }
        String t = root.path("t").asText(null);
        JsonNode d = root.path("d");
        String id = root.path("id").asText(null);
        if ("READY".equals(t)) {
            String sid = d.path("session_id").asText(null);
            sessionId.set(sid);
            log.info("收到 READY session_id={} seq={} user={}", sid, s,
                    d.path("user").path("username").asText(null));
            if (readyFired.compareAndSet(false, true)) {
                ready.complete(new Ready(sid, seq.get(), d, true));
            }
            events.dispatchEnvelope(t, d, id);
        } else if ("RESUMED".equals(t)) {
            log.info("收到 RESUMED session={} seq={}", sessionId.get(), s);
            if (readyFired.compareAndSet(false, true)) {
                ready.complete(new Ready(sessionId.get(), seq.get(), d, false));
            }
            events.dispatchEnvelope(t, d, id);
        } else if (t != null) {
            if (log.isInfoEnabled()) {
                log.info("========== 原始 WS 文本（单行，便于复制）==========");
                log.info("{}", rawText);
            }
            events.dispatchEnvelope(t, d, id);
        }
    }

    private void sendIdentify() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("token", "QQBot " + accessToken);
        d.put("intents", intents);
        d.put("shard", shard);
        d.put("properties", Map.of("$os", "java", "$browser", "Bot-sdk", "$device", "Bot-sdk"));
        sendRaw(Map.of("op", 2, "d", d));
    }

    private void sendResume() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("token", "QQBot " + accessToken);
        d.put("session_id", sessionId.get());
        d.put("seq", seq.get());
        sendRaw(Map.of("op", 6, "d", d));
        log.info("发送 Resume session={} seq={}", sessionId.get(), seq.get());
    }

    private void startHeartbeat() {
        cancelTimer(heartbeat);
        cancelTimer(ackWatchdog);
        ackReceived.set(true);
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!ackReceived.get() && reconnectHint.incrementAndGet() >= 2) {
                    log.warn("心跳连续未 ACK，判定连接失效");
                    markDead(WsCloseReason.HEARTBEAT_TIMEOUT);
                    return;
                }
                long s = seq.get();
                String payload = s >= 0
                        ? "{\"op\":1,\"d\":" + s + "}"
                        : "{\"op\":1,\"d\":null}";
                currentWs.sendText(payload, true).join();
                ackReceived.set(false);
            } catch (Exception e) {
                log.warn("心跳发送失败: {}", e.getMessage());
                markDead(WsCloseReason.SEND_FAILED);
            }
        }, heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
        heartbeat.set(task);
    }

    private void sendRaw(Map<String, Object> payload) {
        try {
            currentWs.sendText(Json.write(payload), true).join();
        } catch (Exception e) {
            log.warn("ws send failed: {}", e.getMessage());
            markDead(WsCloseReason.SEND_FAILED);
        }
    }

    void markDead(WsCloseReason reason) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        cancelTimer(heartbeat);
        cancelTimer(ackWatchdog);
        log.info("连接标记失效: {}", reason);
        try {
            currentWs.abort();
        } catch (Exception ignored) {
            // 忽略
        }
        // abort 之后再触发重连，避免重入时再次 markDead
        if (onDead != null) {
            try {
                onDead.run();
            } catch (Exception e) {
                log.warn("onDead 回调异常: {}", e.getMessage());
            }
        }
    }

    private static void cancelTimer(AtomicReference<ScheduledFuture<?>> ref) {
        ScheduledFuture<?> t = ref.getAndSet(null);
        if (t != null) {
            t.cancel(false);
        }
    }

    public CompletableFuture<Ready> awaitReady() {
        return ready;
    }

    public String sessionId() {
        return sessionId.get();
    }

    public long lastSeq() {
        return seq.get();
    }

    public SessionState snapshot() {
        return new SessionState(sessionId.get(), seq.get());
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        cancelTimer(heartbeat);
        cancelTimer(ackWatchdog);
        try {
            currentWs.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
                    .toCompletableFuture()
                    .get(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // 忽略
        }
        try {
            currentWs.abort();
        } catch (Exception ignored) {
            // 忽略
        }
        scheduler.shutdownNow();
        if (!ready.isDone()) {
            ready.completeExceptionally(new QqBotException("连接已关闭"));
        }
    }

    public record Ready(String sessionId, long seq, JsonNode data, boolean fresh) {
    }

    public record SessionState(String sessionId, long seq) {
    }

    private static String truncate(String s, int n) {
        if (s == null) {
            return null;
        }
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }

    private static String pretty(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "null";
        }
        try {
            return com.xuanji.qqbot.json.Json.mapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(node);
        } catch (Exception e) {
            return String.valueOf(node);
        }
    }

    private static final class ListenerHolder implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();
        private volatile GatewayConnection conn;

        void bind(GatewayConnection conn) {
            this.conn = conn;
            synchronized (buffer) {
                if (buffer.length() > 0) {
                    String text = buffer.toString();
                    buffer.setLength(0);
                    conn.onText(text);
                }
            }
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            synchronized (buffer) {
                buffer.append(data);
                if (last) {
                    String text = buffer.toString();
                    buffer.setLength(0);
                    GatewayConnection c = conn;
                    if (c != null) {
                        c.onText(text);
                    } else {
                        buffer.append(text);
                    }
                }
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.debug("ws closed: {} {}", statusCode, reason);
            GatewayConnection c = conn;
            if (c != null) {
                c.markDead(statusCode == WebSocket.NORMAL_CLOSURE
                        ? WsCloseReason.SOCKET_CLOSED
                        : WsCloseReason.SOCKET_CLOSED);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("ws error: {}", error.getMessage());
            GatewayConnection c = conn;
            if (c != null) {
                c.markDead(WsCloseReason.SOCKET_ERROR);
            }
        }
    }
}
