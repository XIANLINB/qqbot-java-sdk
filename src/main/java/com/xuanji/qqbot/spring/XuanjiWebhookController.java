package com.xuanji.qqbot.spring;

import com.xuanji.qqbot.json.Json;
import com.xuanji.qqbot.webhook.WebhookCodec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QQ 机器人 Webhook 入口处理器（多机器人）。
 * <p>
 * HTTP 挂载由 {@code XuanjiWebhookAutoConfiguration} 按每台 webhook 机器人的回调路径注册；
 * 处理时按 {@code X-Bot-Appid} 请求头定位机器人（验签用其 secret、事件投递到其总线）；
 * 头缺失且仅配置一个 webhook 机器人时自动使用它。
 */
public class XuanjiWebhookController {
    private static final Logger log = LoggerFactory.getLogger(XuanjiWebhookController.class);

    private final BotRegistry registry;
    private final Map<String, WebhookCodec> codecs = new ConcurrentHashMap<>();

    /**
     * @param registry 多机器人注册表
     */
    public XuanjiWebhookController(BotRegistry registry) {
        this.registry = registry;
    }

    /**
     * Webhook 统一处理（仅接受 POST）。
     *
     * @param request  HTTP 请求（含验签头）
     * @param response 响应
     */
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(405);
            return;
        }
        String signature = request.getHeader("X-Signature-Ed25519");
        String timestamp = request.getHeader("X-Signature-Timestamp");
        String botAppid = request.getHeader("X-Bot-Appid");
        byte[] rawBody = request.getInputStream().readAllBytes();

        BotRegistry.Entry entry = resolveBot(botAppid);
        if (entry == null) {
            log.warn("[xuanji] webhook 无匹配机器人 appid={}", botAppid);
            response.setStatus(404);
            return;
        }
        WebhookCodec codec = codecs.computeIfAbsent(entry.appId(),
                k -> WebhookCodec.of(entry.appSecret()));

        // op=13 回调地址验证
        if (codec.isValidation(rawBody)) {
            Map<String, String> resp = codec.validationResponse(rawBody);
            log.info("[{}][webhook] 回调地址验证通过", entry.tag());
            writeJson(response, 200, resp);
            return;
        }
        // Ed25519 验签
        if (!codec.verify(signature, timestamp, rawBody)) {
            log.warn("[{}][webhook] 验签失败 path={}", entry.tag(), request.getRequestURI());
            response.setStatus(401);
            return;
        }
        // 分发到该机器人自己的事件总线（原始报文统一在 Events 打印，受 logRawPayload 控制）
        codec.dispatch(rawBody, entry.bot().events());
        writeJson(response, 200, WebhookCodec.callbackAck());
    }

    private static void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(Json.writeBytes(body));
        response.getOutputStream().flush();
    }

    private BotRegistry.Entry resolveBot(String headerAppid) {
        BotRegistry.Entry e = registry.byAppId(headerAppid);
        if (e != null && e.isWebhook()) {
            return e;
        }
        List<BotRegistry.Entry> hooks = registry.byMode("webhook");
        return hooks.size() == 1 ? hooks.get(0) : null;
    }
}
