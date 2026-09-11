package com.xuanji.qqbot;

import com.xuanji.qqbot.auth.Credentials;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * 客户端不可变配置。
 * <p>
 * 用于构造 {@link QqBot}。默认对接官方 OpenAPI 域名
 * {@code https://api.bot.qq.com}（见文档「API 调用指南 · 统一请求地址」）。
 */
public record QqBotOptions(
        /** 开放平台接入票据：AppID + AppSecret */
        Credentials credentials,
        /** OpenAPI 统一请求地址，默认 https://api.bot.qq.com */
        URI baseUri,
        /** 建立 HTTP 连接超时 */
        Duration connectTimeout,
        /** 单次 API 请求超时 */
        Duration requestTimeout,
        /** access_token 提前刷新窗口（官方约 7200s 有效，建议 90s 前刷新） */
        Duration tokenRefreshMargin,
        /** 事件回调执行器；null 表示在 WS/Webhook 收线程上直接执行 */
        Executor eventExecutor,
        /** 是否使用沙箱环境（预留，当前路径未切换） */
        boolean sandbox,
        /** 富媒体上传配置（超时/重试）；null 表示使用 {@link MediaSpec#defaults()} */
        MediaSpec media
) {
    /** 官方 OpenAPI 默认域名 */
    public static final URI DEFAULT_BASE_URI = URI.create("https://api.bot.qq.com");

    public QqBotOptions {
        Objects.requireNonNull(credentials, "credentials 不可为空");
        Objects.requireNonNull(baseUri, "baseUri 不可为空");
        Objects.requireNonNull(connectTimeout, "connectTimeout 不可为空");
        Objects.requireNonNull(requestTimeout, "requestTimeout 不可为空");
        Objects.requireNonNull(tokenRefreshMargin, "tokenRefreshMargin 不可为空");
    }

    /**
     * 以官方默认域名与推荐超时生成配置。
     *
     * @param credentials 开放平台 AppID / AppSecret
     * @return 不可变配置对象
     */
    public static QqBotOptions defaults(Credentials credentials) {
        return new QqBotOptions(
                credentials,
                DEFAULT_BASE_URI,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ofSeconds(90),
                null,
                false,
                MediaSpec.defaults()
        );
    }
}
