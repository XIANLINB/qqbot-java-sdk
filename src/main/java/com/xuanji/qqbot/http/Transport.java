package com.xuanji.qqbot.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

/**
 * 底层 HTTP 传输抽象。
 * <p>
 * 测试可注入假实现；默认实现为 {@link JdkTransport}（JDK HttpClient，零三方 HTTP 库）。
 */
public interface Transport extends AutoCloseable {
    /**
     * 执行一次 HTTP 请求。
     *
     * @param request 方法、URI、请求头、请求体
     * @return 状态码、响应头、响应体
     */
    RawResponse exchange(RawRequest request);

    @Override
    default void close() {
    }

    /**
     * 原始 HTTP 请求。
     *
     * @param method  HTTP 方法，如 GET/POST/DELETE/PUT
     * @param uri     完整 URI
     * @param headers 请求头（如 Authorization）
     * @param body    请求体字节，可为 null/空
     */
    record RawRequest(
            String method,
            URI uri,
            Map<String, String> headers,
            byte[] body
    ) {
        public RawRequest {
            headers = headers == null ? Map.of() : Map.copyOf(headers);
            body = body == null ? new byte[0] : body;
        }
    }

    /**
     * 原始 HTTP 响应。
     *
     * @param status  HTTP 状态码（200/204/201 等见官方 API 调用指南）
     * @param headers 响应头
     * @param body    响应体
     */
    record RawResponse(int status, Map<String, String> headers, byte[] body) {
        /**
         * @return 响应体 UTF-8 字符串
         */
        public String bodyAsString() {
            return body == null ? "" : new String(body, java.nio.charset.StandardCharsets.UTF_8);
        }

        /**
         * 按忽略大小写取响应头。
         *
         * @param name 头名称，如 X-Tps-trace-ID
         * @return 头值，不存在时为 null
         */
        public String header(String name) {
            if (headers == null || name == null) {
                return null;
            }
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue();
                }
            }
            return null;
        }
    }

    /**
     * 基于 JDK {@link HttpClient} 的默认传输实现。
     */
    final class JdkTransport implements Transport {
        private final HttpClient client;
        private final Duration requestTimeout;

        /**
         * @param connectTimeout 连接超时
         * @param requestTimeout 单次请求超时
         */
        public JdkTransport(Duration connectTimeout, Duration requestTimeout) {
            this.client = HttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            this.requestTimeout = requestTimeout;
        }

        @Override
        public RawResponse exchange(RawRequest request) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                        .timeout(requestTimeout)
                        .method(request.method(), request.body().length == 0
                                ? HttpRequest.BodyPublishers.noBody()
                                : HttpRequest.BodyPublishers.ofByteArray(request.body()));
                request.headers().forEach(builder::header);
                HttpResponse<byte[]> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
                var headerMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
                resp.headers().map().forEach((k, v) -> {
                    if (v != null && !v.isEmpty()) {
                        headerMap.put(k, v.get(0));
                    }
                });
                return new RawResponse(resp.statusCode(), headerMap, resp.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new com.xuanji.qqbot.exception.TransportException("HTTP 请求被中断");
            } catch (java.io.IOException e) {
                throw new com.xuanji.qqbot.exception.TransportException(
                        "HTTP " + request.method() + " " + request.uri() + " 失败: " + e.getMessage(), e);
            }
        }
    }
}
