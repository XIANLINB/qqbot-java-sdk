package com.xuanji.qqbot.auth;

import java.util.Objects;

/**
 * 开放平台接入票据。
 * <p>
 * 对应官方「启动接入 · 接入票据」中的 AppID 与 AppSecret。
 * 旧版 Token 鉴权已废弃，请勿使用。
 */
public record Credentials(
        /** 机器人 AppID（开放平台管理端获取） */
        String appId,
        /** 机器人 AppSecret（获取 access_token、Webhook Ed25519 验签用） */
        String appSecret
) {
    public Credentials {
        Objects.requireNonNull(appId, "appId 不可为空");
        Objects.requireNonNull(appSecret, "appSecret 不可为空");
        if (appId.isBlank() || appSecret.isBlank()) {
            throw new IllegalArgumentException("appId / appSecret 不能为空字符串");
        }
    }
}
