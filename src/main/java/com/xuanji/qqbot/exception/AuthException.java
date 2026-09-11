package com.xuanji.qqbot.exception;

/**
 * 鉴权失败：获取 access_token 失败或凭证无效。
 * <p>
 * 常见官方错误：100007 appid invalid、100016 invalid appid or secret。
 */
public class AuthException extends QqBotException {
    /**
     * @param message 错误说明
     */
    public AuthException(String message) {
        super(message);
    }

    /**
     * @param message 错误说明
     * @param cause   底层原因
     */
    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
