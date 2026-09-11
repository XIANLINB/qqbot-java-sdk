package com.xuanji.qqbot.exception;

/**
 * OpenAPI 业务失败异常。
 * <p>
 * 官方要求以 Body 中的 {@code err_code} 判断成败，不要依赖 {@code message} 文案。
 * 对应失败响应示例：{@code { "err_code": ..., "message": ..., "trace_id": ... }}。
 */
public class ApiException extends QqBotException {
    private final int errCode;
    private final String traceId;
    private final int httpStatus;

    /**
     * @param errCode   业务错误码（官方 err_code）；HTTP 层失败时可能为 0
     * @param message   官方 message 或本地拼接说明
     * @param traceId   全链路追踪 ID（trace_id 或响应头 X-Tps-trace-ID），可为空
     * @param httpStatus HTTP 状态码
     */
    public ApiException(int errCode, String message, String traceId, int httpStatus) {
        super(format(errCode, message, traceId, httpStatus));
        this.errCode = errCode;
        this.traceId = traceId;
        this.httpStatus = httpStatus;
    }

    private static String format(int errCode, String message, String traceId, int httpStatus) {
        return "Bot API error: err_code=" + errCode
                + ", http=" + httpStatus
                + ", message=" + message
                + ", trace_id=" + traceId;
    }

    /** @return 官方 err_code */
    public int errCode() {
        return errCode;
    }

    /** @return 链路追踪 ID，可能为 null */
    public String traceId() {
        return traceId;
    }

    /** @return HTTP 状态码 */
    public int httpStatus() {
        return httpStatus;
    }
}
