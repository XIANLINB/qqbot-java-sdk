package com.xuanji.qqbot.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.exception.ApiException;
import com.xuanji.qqbot.exception.RateLimitException;
import com.xuanji.qqbot.json.Json;
import com.xuanji.qqbot.model.message.ArkMsg;
import com.xuanji.qqbot.model.message.ImageTextCard;
import com.xuanji.qqbot.model.message.PostMessage;
import com.xuanji.qqbot.model.message.StreamMessage;
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
 * <p>
 * 同时是所有动作的统一日志出口（与事件侧 {@code Events.dispatchEnvelope} 的 [IN] 对称）：
 * 发送前打 [OUT] 请求日志（动作中文名/来源/对象/类型/内容预览），
 * 失败打 WARN，响应原始报文受 {@code rawPayloadSupplier} 开关控制。
 */
public final class ApiCall {
    private static final Logger log = LoggerFactory.getLogger(ApiCall.class);

    private final Transport transport;
    private final URI baseUri;
    private final Supplier<String> tokenSupplier;
    /** 机器人标识（方式/appId/名称），读自 Bot 的 Events */
    private final Supplier<String> botTagSupplier;
    /** 是否打印原始报文 */
    private final Supplier<Boolean> rawPayloadSupplier;

    /**
     * @param transport     HTTP 传输
     * @param baseUri       OpenAPI 基址，默认 https://api.bot.qq.com
     * @param tokenSupplier 返回裸 access_token（不含 QQBot 前缀）
     */
    public ApiCall(Transport transport, URI baseUri, Supplier<String> tokenSupplier) {
        this(transport, baseUri, tokenSupplier, null, null);
    }

    /**
     * @param transport          HTTP 传输
     * @param baseUri            OpenAPI 基址
     * @param tokenSupplier      返回裸 access_token（不含 QQBot 前缀）
     * @param botTagSupplier     机器人标识，如 websocket/1905134745/落落；可 null
     * @param rawPayloadSupplier 是否打印响应原始报文；可 null
     */
    public ApiCall(Transport transport, URI baseUri, Supplier<String> tokenSupplier,
                   Supplier<String> botTagSupplier, Supplier<Boolean> rawPayloadSupplier) {
        this.transport = transport;
        this.baseUri = baseUri;
        this.tokenSupplier = tokenSupplier;
        this.botTagSupplier = botTagSupplier;
        this.rawPayloadSupplier = rawPayloadSupplier;
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
        try {
            Transport.RawResponse raw = exchangeRaw(method, path, body);
            return decode(raw, type, method + " " + path);
        } catch (RuntimeException e) {
            log.warn("[{}][OUT][{}] 调用失败: {}", botTag(), actionZh(method, path), e.getMessage());
            throw e;
        }
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
        logOut(method, path, body);
        return transport.exchange(new Transport.RawRequest(method, uri, headers, payload));
    }

    /**
     * 打印 [OUT] 请求日志（统一出口，发送前输出，网络错误也有记录）。
     * <p>
     * 格式：[标识][OUT][动作中文名][来源=..][群ID/用户ID=..][类型][内容=..]
     */
    private void logOut(String method, String path, Object body) {
        try {
            String desc = bodyDesc(body);
            log.info("[{}][OUT][{}]{}{}", botTag(), actionZh(method, path), openidKv(path),
                    desc.isEmpty() ? "" : " " + desc);
        } catch (RuntimeException e) {
            log.debug("[OUT] 日志构造失败", e);
        }
    }

    private String botTag() {
        return botTagSupplier == null ? "" : String.valueOf(botTagSupplier.get());
    }

    private boolean rawPayload() {
        return rawPayloadSupplier != null && Boolean.TRUE.equals(rawPayloadSupplier.get());
    }

    /**
     * 请求体 → [来源][类型][内容=..] 描述；仅消息发送/流式/富媒体上传类有内容。
     */
    private static String bodyDesc(Object body) {
        if (body instanceof PostMessage p) {
            StringBuilder sb = new StringBuilder();
            sb.append(p.msgId() != null || p.eventId() != null ? "[来源=被动回复]" : "[来源=主动发送]");
            int code = p.msgType() == null ? -1 : p.msgType();
            sb.append('[').append(switch (code) {
                case 0 -> "文本";
                case 2 -> "Markdown";
                case 3 -> "Ark";
                case 6 -> "输入中";
                case 7 -> "富媒体";
                case 8 -> "图文卡片";
                default -> "消息";
            }).append(']');
            sb.append(preview(switch (code) {
                case 0 -> p.content();
                case 2 -> p.markdown() == null ? null : p.markdown().content();
                case 3 -> arkPreview(p.ark());
                case 7 -> p.media() == null ? null : p.media().fileInfo();
                case 8 -> cardPreview(p.card());
                default -> null;
            }));
            return sb.toString();
        }
        if (body instanceof StreamMessage s) {
            StringBuilder sb = new StringBuilder();
            sb.append(s.msgId() != null || s.eventId() != null ? "[来源=被动回复]" : "[来源=主动发送]");
            sb.append("[流式").append(StreamMessage.CONTENT_MARKDOWN.equals(s.contentType()) ? "Markdown" : "文本").append(']');
            sb.append(preview(s.contentRaw()));
            return sb.toString();
        }
        if (body instanceof Map<?, ?> map) {
            Object ft = map.get("file_type");
            int code = ft instanceof Number n ? n.intValue() : 0;
            StringBuilder sb = new StringBuilder('[' + switch (code) {
                case 1 -> "图片";
                case 2 -> "视频";
                case 3 -> "语音";
                case 4 -> "文件";
                default -> "富媒体";
            } + ']');
            Object url = map.get("url");
            if (url != null) {
                sb.append("[内容=").append(truncate(String.valueOf(url))).append(']');
            } else if (map.get("base64") != null) {
                sb.append("[Base64数据]");
            }
            return sb.toString();
        }
        return "";
    }

    private static String arkPreview(ArkMsg ark) {
        if (ark == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("模板").append(ark.templateId());
        if (ark.kv() != null) {
            ark.kv().stream()
                    .filter(kv -> kv.value() != null && !kv.value().isBlank())
                    .findFirst()
                    .ifPresent(kv -> sb.append(' ').append(kv.value()));
        }
        return sb.toString();
    }

    private static String cardPreview(ImageTextCard card) {
        return card == null || card.content() == null ? null : card.content().title();
    }

    /** 内容预览段：[内容=xxx]，截断 50 字符，换行转 \n。 */
    private static String preview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String prev = content.length() > 50 ? content.substring(0, 50) + "…" : content;
        return "[内容=" + prev.replace("\n", "\\n").replace("\r", "") + "]";
    }

    /**
     * 从路径提取群/用户 openid，返回如 [群ID=xxx]；无则空串。
     */
    private static String openidKv(String path) {
        String[] seg = path.split("/");
        if (seg.length >= 4 && (seg[2].equals("groups") || seg[2].equals("users"))) {
            String t = seg[3];
            if (!t.equals("join_approval_strategy") && !t.equals("@me")) {
                return "[" + (seg[2].equals("groups") ? "群ID=" : "用户ID=") + t + "]";
            }
        }
        return "";
    }

    /**
     * 请求路径 → 动作中文名；未知路径兜底显示 method + path。
     */
    static String actionZh(String method, String path) {
        String m = method == null ? "" : method.toUpperCase();
        String p = path == null ? "" : path.split("\\?")[0];
        String[] seg = p.split("/");
        // /v2/groups|users/{openid}/xxx 或 /v2/groups/join_approval_strategy/...
        if (seg.length >= 4 && seg[1].equals("v2") && (seg[2].equals("groups") || seg[2].equals("users"))) {
            boolean group = seg[2].equals("groups");
            String scope = group ? "群聊" : "单聊";
            String third = seg[3];
            if (third.equals("join_approval_strategy")) {
                return "GET".equals(m) ? "查询入群审批策略" : "设置入群审批策略";
            }
            if (third.equals("@me")) {
                return "获取机器人信息";
            }
            String resource = seg.length >= 5 ? seg[4] : "";
            return switch (resource) {
                case "messages" -> switch (m) {
                    case "POST" -> scope + "消息";
                    case "DELETE" -> "撤回" + scope + "消息";
                    default -> scope + "消息接口";
                };
                case "files" -> "上传" + scope + "富媒体";
                case "stream_messages" -> "单聊流式消息";
                case "upload_prepare" -> "分片上传准备";
                case "upload_part_finish" -> "分片上传完成";
                case "members" -> "GET".equals(m) ? "查询" + scope + "成员" : scope + "成员接口";
                case "member_blacklist" -> switch (m) {
                    case "GET" -> "查询群黑名单";
                    case "DELETE" -> "移出群黑名单";
                    default -> "群黑名单操作";
                };
                case "batch_remove_members" -> "批量移出群成员";
                case "info" -> "查询群信息";
                case "bot_state" -> "查询机器人群状态";
                case "restrict_chat_setting" -> "GET".equals(m) ? "查询禁言设置" : scope + "禁言";
                case "join_request_list" -> "查询入群申请";
                case "approval_join_request" -> "入群审批";
                default -> scope + "接口 " + m + " " + resource;
            };
        }
        if (p.startsWith("/interactions/")) {
            return "互动回应";
        }
        if (p.equals("/users/@me")) {
            return "获取机器人信息";
        }
        if (p.equals("/gateway")) {
            return "获取网关地址";
        }
        if (p.equals("/gateway/bot")) {
            return "获取网关地址(含鉴权)";
        }
        if (p.equals("/v2/generate_url_link")) {
            return "生成分享链接";
        }
        if (p.equals("/v2/menu")) {
            return "GET".equals(m) ? "查询菜单" : "更新菜单";
        }
        if (p.startsWith("/v2/panels")) {
            return switch (m) {
                case "POST" -> "创建面板";
                case "PUT" -> p.endsWith("/target") ? "更新面板投放" : "更新面板";
                case "DELETE" -> "删除面板";
                default -> "查询面板";
            };
        }
        return m + " " + p;
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
        // 成功响应原始报文（与 IN 侧同一开关）
        if (rawPayload() && !body.isBlank()) {
            log.info("[{}][OUT][{}][原始报文] {}", botTag(), actionZhOfDesc(path), body.trim());
        }
        return Json.read(body, type);
    }

    /** decode 的 path 描述（"POST /v2/xxx"）→ 动作中文名。 */
    private static String actionZhOfDesc(String desc) {
        int i = desc == null ? -1 : desc.indexOf(' ');
        return i > 0 ? actionZh(desc.substring(0, i), desc.substring(i + 1)) : String.valueOf(desc);
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
