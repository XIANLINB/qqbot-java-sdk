package com.xuanji.qqbot.api;

import com.xuanji.qqbot.http.ApiCall;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 网关与机器人身份 OpenAPI。
 * <p>
 * WebSocket 事件通道需先：
 * {@code GET /gateway} 或 {@code GET /gateway/bot} 获取 wss 地址，
 * 再按官方 OpCode 建立连接（见 {@code docs/guide/03-事件订阅.md}）。
 */
public final class GatewayApi {
    private final ApiCall api;

    /**
     * @param api 已注入 token 的调用器
     */
    public GatewayApi(ApiCall api) {
        this.api = api;
    }

    /**
     * 获取通用 WSS 接入点。
     * 对应文档「获取通用 WSS 接入点」：{@code GET /gateway}。
     *
     * @return 含 url 的结果
     */
    public GatewayUrl gateway() {
        return api.get("/gateway", GatewayUrl.class);
    }

    /**
     * 获取带分片信息的 WSS 接入点。
     * 对应文档「获取带分片 WSS 接入点」：{@code GET /gateway/bot}。
     * 返回 url、shards、session_start_limit。
     *
     * @return 含分片建议的结果
     */
    public GatewayBot gatewayBot() {
        return api.get("/gateway/bot", GatewayBot.class);
    }

    /**
     * 获取当前机器人信息。
     * 对应 {@code GET /users/@me}。
     *
     * @return 机器人资料
     */
    public BotProfile me() {
        return api.get("/users/@me", BotProfile.class);
    }

    /**
     * 通用网关地址响应。
     *
     * @param url WebSocket 地址，如 wss://api.bot.qq.com/websocket/
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GatewayUrl(
            @JsonProperty("url") String url
    ) {
    }

    /**
     * 带分片的网关响应。
     *
     * @param url                WebSocket 地址
     * @param shards             建议分片数
     * @param sessionStartLimit 会话启动限制（连接数配额）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GatewayBot(
            @JsonProperty("url") String url,
            @JsonProperty("shards") Integer shards,
            @JsonProperty("session_start_limit") SessionStartLimit sessionStartLimit
    ) {
    }

    /**
     * session_start_limit。
     *
     * @param total          总连接配额
     * @param remaining      剩余可建连接数
     * @param resetAfter     重置间隔（毫秒）
     * @param maxConcurrency 同时 identify 并发限制
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionStartLimit(
            @JsonProperty("total") Integer total,
            @JsonProperty("remaining") Integer remaining,
            @JsonProperty("reset_after") Long resetAfter,
            @JsonProperty("max_concurrency") Integer maxConcurrency
    ) {
    }

    /**
     * 机器人详情（实测 GET /users/@me）。
     * 无 bot 字段；含 share_url、welcome_msg。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BotProfile(
            /** 机器人 ID */
            @JsonProperty("id") String id,
            /** 显示名 */
            @JsonProperty("username") String username,
            /** 头像 URL */
            @JsonProperty("avatar") String avatar,
            /** 分享链接 */
            @JsonProperty("share_url") String shareUrl,
            /** 欢迎语，可能为空串 */
            @JsonProperty("welcome_msg") String welcomeMsg
    ) {
    }
}
