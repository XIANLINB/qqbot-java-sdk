package com.xuanji.qqbot.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.exception.ApiException;
import com.xuanji.qqbot.exception.RateLimitException;
import com.xuanji.qqbot.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 带 access_token 鉴权的 OpenAPI 调用器。
 * <p>
 * 统一处理：Header {@code Authorization: QQBot {ACCESS_TOKEN}}、
 * 成功/失败响应解析（err_code / trace_id）、429 映射。
 */
public final class ApiCall {
    private static final Logger log = LoggerFactory.getLogger(ApiCall.class);

    private final Transport transport;
    private final URI baseUri;
    private final Supplier<String> tokenSupplier;

    /**
     * @param transport     HTTP 传输
     * @param baseUri       OpenAPI 基址，默认 https://api.bot.qq.com
     * @param tokenSupplier 返回裸 access_token（不含 QQBot 前缀）
     */
    public ApiCall(Transport transport, URI baseUri, Supplier<String> tokenSupplier) {
        this.transport = transport;
        this.baseUri = baseUri;
        this.tokenSupplier = tokenSupplier;
    }

    /**
     * GET 请求并反序列化。
     *
     * @param path 相对路径，如 /gateway
     * @param type 响应类型
     * @return 业务数据
     */
    public <T> T get(String path, Class<T> type) {
        return exchange("GET", path, null, type);
    }

    /**
     * POST JSON 请求并反序列化。
     *
     * @param path 相对路径
     * @param body 请求体对象（Jackson 序列化）
     * @param type 响应类型
     * @return 业务数据
     */
    public <T> T post(String path, Object body, Class<T> type) {
        return exchange("POST", path, body, type);
    }

    /**
     * DELETE 请求并反序列化。
     *
     * @param path 相对路径
     * @param type 响应类型
     * @return 业务数据
     */
    public <T> T delete(String path, Class<T> type) {
        return exchange("DELETE", path, null, type);
    }

    /**
     * PUT JSON 请求并反序列化。
     *
     * @param path 相对路径
     * @param body 请求体
     * @param type 响应类型
     * @return 业务数据
     */
    public <T> T put(String path, Object body, Class<T> type) {
        return exchange("PUT", path, body, type);
    }

    /**
     * PATCH JSON 请求并反序列化。
     *
     * @param path 相对路径
     * @param body 请求体
     * @param type 响应类型
     * @return 业务数据
     */
    public <T> T patch(String path, Object body, Class<T> type) {
        return exchange("PATCH", path, body, type);
    }

    /**
     * 通用 HTTP 调用。
     *
     * @param method HTTP 方法
     * @param path   相对路径
     * @param body   请求体；null 表示无 JSON body
     * @param type   响应类型
     * @return 业务数据
     */
    public <T> T exchange(String method, String path, Object body, Class<T> type) {
        Transport.RawResponse raw = exchangeRaw(method, path, body);
        return decode(raw, type, method + " " + path);
    }

    /**
     * 只发请求并返回原始响应（不解析业务体）。
     *
     * @param method HTTP 方法
     * @param path   相对路径
     * @param body   请求体或 null
     * @return 原始响应
     */
    public Transport.RawResponse exchangeRaw(String method, String path, Object body) {
        URI uri = baseUri.resolve(path.startsWith("/") ? path : "/" + path);
        Map<String, String> headers = new LinkedHashMap<>();
        // 官方：Authorization: QQBot {ACCESS_TOKEN}
        headers.put("Authorization", "QQBot " + tokenSupplier.get());
        byte[] payload = null;
        if (body != null) {
            payload = Json.writeBytes(body);
            headers.put("Content-Type", "application/json; charset=utf-8");
        } else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            headers.put("Content-Type", "application/json; charset=utf-8");
            payload = new byte[0];
        }
        return transport.exchange(new Transport.RawRequest(method, uri, headers, payload));
    }

    /**
     * 解析响应：成功返回业务对象，失败抛 {@link ApiException}。
     *
     * @param raw  原始响应
     * @param type 目标类型；Void 表示忽略 body
     * @return 业务数据
     */
    @SuppressWarnings("unchecked")
    public <T> T decode(Transport.RawResponse raw, Class<T> type) {
        return decode(raw, type, "?");
    }

    /**
     * 解析响应并打印原始 JSON。
     *
     * @param raw  原始响应
     * @param type 目标类型
     * @param path 请求描述（用于日志）
     * @return 业务数据
     */
    @SuppressWarnings("unchecked")
    public <T> T decode(Transport.RawResponse raw, Class<T> type, String path) {
        int status = raw.status();
        String body = raw.bodyAsString();
        String traceId = raw.header("X-Tps-trace-ID");

        if (status == 204) {
            if (type == Void.class || type == Transport.RawResponse.class) {
                return (T) (type == Void.class ? null : raw);
            }
        }

        ErrorBody err = null;
        if (!body.isBlank()) {
            try {
                err = Json.read(body, ErrorBody.class);
            } catch (RuntimeException ignored) {
                // 不是错误响应体
            }
        }

        boolean hasErr = err != null && err.errCode != null && err.errCode != 0;
        if (hasErr) {
            int code = err.errCode;
            String msg = err.message != null ? err.message : "";
            String tid = err.traceId != null ? err.traceId : traceId;
            if (status == 429 || code == 20028 || code == 40034100) {
                throw new RateLimitException(code, msg, tid, status);
            }
            throw new ApiException(code, msg, tid, status);
        }

        if (status >= 400) {
            throw new ApiException(0, "HTTP " + status + ": " + truncate(body), traceId, status);
        }

        if (type == Void.class || body.isBlank()) {
            return null;
        }
        if (type == Transport.RawResponse.class) {
            return (T) raw;
        }
        if (type == String.class) {
            return (T) body;
        }
        // 打印成功响应原始 JSON，便于对照官方字段完善模型
        if (log.isInfoEnabled() && !body.isBlank()) {
            log.info("========== OpenAPI 响应 BEGIN {} status={} ==========", path, status);
            log.info("{}", prettyJson(body));
            log.info("========== OpenAPI 响应 END ==========");
        }
        return Json.read(body, type);
    }

    private static String prettyJson(String body) {
        try {
            return Json.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(
                    Json.mapper().readTree(body));
        } catch (Exception e) {
            return body;
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }

    /**
     * 路径段 URL 编码（openid 等）。
     *
     * @param segment 原始路径段
     * @return 编码结果
     */
    public static String pathEncode(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8);
    }

    /**
     * 失败响应通用结构。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ErrorBody {
        /** 官方业务错误码 */
        @JsonProperty("err_code")
        public Integer errCode;
        /** 错误文案（可能变更，勿用于判断） */
        @JsonProperty("message")
        public String message;
        /** 链路追踪 ID */
        @JsonProperty("trace_id")
        public String traceId;
    }
}
