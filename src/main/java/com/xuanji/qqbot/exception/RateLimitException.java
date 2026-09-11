package com.xuanji.qqbot.exception;

/**
 * 限频异常。
 * <p>
 * 触发条件：HTTP 429，或业务码如 20028、40034100（主动消息超频控）等。
 */
public class RateLimitException extends ApiException {
    /**
     * @param errCode    官方 err_code
     * @param message    错误说明
     * @param traceId    链路追踪 ID
     * @param httpStatus HTTP 状态码
     */
    public RateLimitException(int errCode, String message, String traceId, int httpStatus) {
        super(errCode, message, traceId, httpStatus);
    }
}
