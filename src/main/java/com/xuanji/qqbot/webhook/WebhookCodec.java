package com.xuanji.qqbot.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.xuanji.qqbot.event.Events;
import com.xuanji.qqbot.json.Json;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Webhook 编解码。
 * <p>
 * 官方「Webhook 方式」：管理端配置回调地址（80/443/8080/8443）。
 * 验签：X-Signature-Ed25519 + X-Signature-Timestamp，内容为 timestamp+body。
 * 收事件后回 {"op":12}；地址验证为 op=13。
 */
public final class WebhookCodec {
    private final String appSecret;

    private WebhookCodec(String appSecret) {
        this.appSecret = appSecret;
    }

    /**
     * @param appSecret AppSecret
     * @return 编解码器
     */
    public static WebhookCodec of(String appSecret) {
        return new WebhookCodec(appSecret);
    }

    /**
     * @param rawBody 原始 Body
     * @return 是否 op=13 回调验证
     */
    public boolean isValidation(byte[] rawBody) {
        JsonNode node = parse(rawBody);
        return node != null && node.path("op").asInt(-1) == 13;
    }

    /**
     * 构造 op=13 验证响应（plain_token + hex signature）。
     *
     * @param rawBody 原始 Body
     * @return 响应字段
     */
    public Map<String, String> validationResponse(byte[] rawBody) {
        JsonNode d = parse(rawBody).path("d");
        String plainToken = d.path("plain_token").asText(null);
        String eventTs = d.path("event_ts").asText(null);
        if (plainToken == null || eventTs == null) {
            throw new IllegalArgumentException("validation payload missing plain_token/event_ts");
        }
        byte[] msg = (eventTs + plainToken).getBytes(StandardCharsets.UTF_8);
        byte[] sig = Ed25519.sign(appSecret, msg);
        Map<String, String> resp = new LinkedHashMap<>();
        resp.put("plain_token", plainToken);
        resp.put("signature", HexFormat.of().formatHex(sig));
        return resp;
    }

    /**
     * Ed25519 验签。
     *
     * @param signatureHex X-Signature-Ed25519
     * @param timestamp    X-Signature-Timestamp
     * @param rawBody      原始 Body
     * @return 是否通过
     */
    public boolean verify(String signatureHex, String timestamp, byte[] rawBody) {
        if (signatureHex == null || signatureHex.isBlank() || timestamp == null || rawBody == null) {
            return false;
        }
        byte[] sig;
        try {
            sig = HexFormat.of().parseHex(signatureHex.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (sig.length != 64) {
            return false;
        }
        byte[] msg = concat(timestamp.getBytes(StandardCharsets.UTF_8), rawBody);
        return Ed25519.verifyWithSecret(appSecret, msg, sig);
    }

    /**
     * 解析并分发事件。
     *
     * @param rawBody 原始 Body（须已验签）
     * @param events  事件总线
     * @return 事件类型名或 OPxx
     */
    public String dispatch(byte[] rawBody, Events events) {
        JsonNode root = parse(rawBody);
        if (root == null) {
            return null;
        }
        int op = root.path("op").asInt(-1);
        if (op == 12 || op == 13) {
            return "OP" + op;
        }
        if (op != 0) {
            return "OP" + op;
        }
        String t = root.path("t").asText(null);
        String id = root.path("id").asText(null);
        JsonNode d = root.path("d");
        if (t != null) {
            events.dispatchEnvelope(t, d, id);
        }
        return t;
    }

    /**
     * @return {"op":12} HTTP Callback ACK
     */
    public static Map<String, Object> callbackAck() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("op", 12);
        return m;
    }

    private static JsonNode parse(byte[] rawBody) {
        if (rawBody == null || rawBody.length == 0) {
            return null;
        }
        try {
            return Json.mapper().readTree(rawBody);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Envelope {
        @JsonProperty("id")
        public String id;
        @JsonProperty("op")
        public int op;
        @JsonProperty("s")
        public Long s;
        @JsonProperty("t")
        public String t;
        @JsonProperty("d")
        public JsonNode d;
    }
}
