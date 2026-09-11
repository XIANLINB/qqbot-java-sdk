package com.xuanji.qqbot.spring;

import com.xuanji.qqbot.Bot;
import com.xuanji.qqbot.event.Events;
import com.xuanji.qqbot.webhook.WebhookCodec;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * xuanji Webhook HTTP 入口。
 * 路径由 {@code xuanji.webhook.path} 配置，默认 {@code /xuanji/webhook}。
 */
@RestController
@ConditionalOnProperty(prefix = "xuanji", name = "webhook.enable", havingValue = "true")
public class XuanjiWebhookController {
    private static final Logger log = LoggerFactory.getLogger(XuanjiWebhookController.class);

    private final Bot Bot;
    private final WebhookCodec codec;

    /**
     * @param props 配置
     * @param Bot 客户端
     */
    public XuanjiWebhookController(XuanjiProperties props, Bot Bot) {
        this.Bot = Bot;
        this.codec = WebhookCodec.of(props.resolveAppSecret(false));
    }

    /**
     * @param signature X-Signature-Ed25519
     * @param timestamp X-Signature-Timestamp
     * @param rawBody   原始 Body
     * @param request   HTTP
     * @return 验证结果或 op=12
     */
    @PostMapping("${xuanji.webhook.path:/xuanji/webhook}")
    public ResponseEntity<?> handle(
            @RequestHeader(value = "X-Signature-Ed25519", required = false) String signature,
            @RequestHeader(value = "X-Signature-Timestamp", required = false) String timestamp,
            @RequestBody byte[] rawBody,
            HttpServletRequest request) {
        if (codec.isValidation(rawBody)) {
            Map<String, String> resp = codec.validationResponse(rawBody);
            log.info("[xuanji] webhook 回调验证通过");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resp);
        }
        if (!codec.verify(signature, timestamp, rawBody)) {
            log.warn("[xuanji] webhook 验签失败 path={}", request.getRequestURI());
            return ResponseEntity.status(401).build();
        }
        codec.dispatch(rawBody, Bot.events());
        return ResponseEntity.ok(WebhookCodec.callbackAck());
    }
}
