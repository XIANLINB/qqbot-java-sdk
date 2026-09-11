package com.xuanji.qqbot;

import java.time.Duration;

/**
 * 富媒体上传配置：超时与重试。
 * <p>
 * 作用于富媒体上传的全部 HTTP 请求（URL/Base64 上传、分片准备/直传/完成/合并），
 * 与普通 API 请求的 {@code connectTimeout}/{@code requestTimeout} 相互独立。
 * 仅对网络类失败（连接失败/请求超时/IO 中断）重试；业务错误（有明确 err_code 响应）不重试。
 *
 * @param connectTimeout 建立 HTTP 连接超时，默认 60 秒
 * @param requestTimeout 单次请求超时，默认 60 秒
 * @param retry          网络类失败后的额外重试次数，默认 1；0 表示不重试
 */
public record MediaSpec(Duration connectTimeout, Duration requestTimeout, int retry) {
    /** 连接超时默认值：60 秒 */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(60);
    /** 请求超时默认值：60 秒 */
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);
    /** 重试次数默认值：1 */
    public static final int DEFAULT_RETRY = 1;

    /**
     * @return 默认配置：连接 60s / 请求 60s / 重试 1 次
     */
    public static MediaSpec defaults() {
        return new MediaSpec(DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT, DEFAULT_RETRY);
    }

    /**
     * 紧凑构造：空/非法值回退默认。
     */
    public MediaSpec {
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        }
        if (requestTimeout == null || requestTimeout.isNegative() || requestTimeout.isZero()) {
            requestTimeout = DEFAULT_REQUEST_TIMEOUT;
        }
        if (retry < 0) {
            retry = 0;
        }
    }
}
