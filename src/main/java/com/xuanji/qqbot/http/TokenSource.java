package com.xuanji.qqbot.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.QqBotOptions;
import com.xuanji.qqbot.auth.AccessToken;
import com.xuanji.qqbot.exception.AuthException;
import com.xuanji.qqbot.json.Json;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * access_token 获取与缓存。
 * <p>
 * 对应官方接口：
 * {@code POST https://api.bot.qq.com/app/getAppAccessToken}
 * 请求体 {@code { appId, clientSecret }}，响应 {@code { access_token, expires_in }}。
 * <p>
 * 行为：懒加载；过期前 margin 秒强制刷新；同一时刻仅一个线程真正请求（单飞）。
 */
public final class TokenSource {
    private final Transport transport;
    private final URI baseUri;
    private final String appId;
    private final String appSecret;
    private final Duration margin;

    private final Object lock = new Object();
    private volatile AccessToken cached;

    /**
     * @param transport HTTP 传输
     * @param options   客户端配置（含 credentials、baseUri、tokenRefreshMargin）
     */
    public TokenSource(Transport transport, QqBotOptions options) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.baseUri = options.baseUri();
        this.appId = options.credentials().appId();
        this.appSecret = options.credentials().appSecret();
        this.margin = options.tokenRefreshMargin();
    }

    /**
     * 获取当前可用 access_token（必要时自动刷新）。
     *
     * @return 裸 token 字符串
     */
    public String accessToken() {
        Instant now = Instant.now();
        AccessToken current = cached;
        if (current != null && current.isUsable(now, margin.toSeconds())) {
            return current.token();
        }
        synchronized (lock) {
            now = Instant.now();
            current = cached;
            if (current != null && current.isUsable(now, margin.toSeconds())) {
                return current.token();
            }
            cached = fetch();
            return cached.token();
        }
    }

    private AccessToken fetch() {
        byte[] body = Json.writeBytes(Map.of("appId", appId, "clientSecret", appSecret));
        Transport.RawResponse resp = transport.exchange(new Transport.RawRequest(
                "POST",
                baseUri.resolve("/app/getAppAccessToken"),
                Map.of("Content-Type", "application/json; charset=utf-8"),
                body
        ));
        if (resp.status() >= 400) {
            throw new AuthException("getAppAccessToken HTTP " + resp.status() + ": " + resp.bodyAsString());
        }
        TokenResponse tr;
        try {
            tr = Json.read(resp.body(), TokenResponse.class);
        } catch (RuntimeException e) {
            throw new AuthException("getAppAccessToken 响应解析失败: " + resp.bodyAsString(), e);
        }
        if (tr.access_token == null || tr.access_token.isBlank()) {
            throw new AuthException("getAppAccessToken 返回空 token");
        }
        long seconds = 7200;
        if (tr.expires_in != null) {
            try {
                seconds = Long.parseLong(String.valueOf(tr.expires_in).trim());
            } catch (NumberFormatException ignored) {
                // 保持默认有效期
            }
        }
        return new AccessToken(tr.access_token, Instant.now().plusSeconds(Math.max(seconds, 60)));
    }

    /** 官方 token 响应体。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class TokenResponse {
        /** access_token 字符串 */
        @JsonProperty("access_token")
        public String access_token;
        /** 有效期（秒），官方当前约 7200 */
        @JsonProperty("expires_in")
        public Object expires_in;
    }
}
