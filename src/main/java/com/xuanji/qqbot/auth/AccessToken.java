package com.xuanji.qqbot.auth;

import java.time.Instant;

/**
 * 已缓存的 access_token。
 * <p>
 * 官方：有效期约 7200 秒；有效期内重复获取返回同一 token；
 * 过期前约 60 秒可换新 token，旧 token 在窗口内仍可用。
 */
public record AccessToken(
        /** 凭证字符串（调用 OpenAPI 时拼为 Authorization: Bot {token}） */
        String token,
        /** 绝对过期时刻 */
        Instant expiresAt
) {
    /**
     * 判断在 {@code now} 时刻是否仍可直接使用。
     *
     * @param now           当前时间
     * @param marginSeconds 提前失效秒数（建议 ≥ 90，覆盖官方 60s 双活窗口）
     * @return true 表示无需刷新
     */
    public boolean isUsable(Instant now, long marginSeconds) {
        return token != null && !token.isBlank()
                && now.isBefore(expiresAt.minusSeconds(marginSeconds));
    }
}
