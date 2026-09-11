package com.xuanji.qqbot.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * xuanji.* 配置绑定（Shiro 风格：业务工程只写 yml）。
 * <p>
 * 示例：
 * <pre>
 * xuanji:
 *   websocket:
 *     enable: true
 *     app-id: xxx
 *     app-secret: yyy
 *   webhook:
 *     enable: false
 *     app-id: xxx
 *     app-secret: yyy
 * </pre>
 */
@ConfigurationProperties(prefix = "xuanji")
public class XuanjiProperties {

    private final Websocket websocket = new Websocket();
    private final Webhook webhook = new Webhook();

    public Websocket getWebsocket() {
        return websocket;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    /**
     * WebSocket 客户端。intents 默认不配置=订阅 SDK 支持的全部事件
     * （GROUP_AND_C2C_EVENT | INTERACTION）。
     */
    public static class Websocket {
        /** 是否启用 */
        private boolean enable = false;
        /** AppID；为空时回退环境变量 QQBOT_APP_ID */
        private String appId = "";
        /** AppSecret；为空时回退环境变量 QQBOT_APP_SECRET */
        private String appSecret = "";
        /** 可选覆盖；默认空=SDK 全部支持事件 */
        private List<String> intents = new ArrayList<>();

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public List<String> getIntents() {
            return intents;
        }

        public void setIntents(List<String> intents) {
            this.intents = intents;
        }
    }

    /** Webhook 回调接入。 */
    public static class Webhook {
        /** 是否启用 */
        private boolean enable = false;
        private String appId = "";
        private String appSecret = "";
        /** 回调路径，默认 /xuanji/webhook */
        private String path = "/xuanji/webhook";

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    /**
     * 解析实际生效的 AppID。
     *
     * @param fromWs true 取 websocket 节点
     * @return app-id
     */
    public String resolveAppId(boolean fromWs) {
        String v = fromWs ? websocket.getAppId() : webhook.getAppId();
        if (v == null || v.isBlank()) {
            v = System.getenv("QQBOT_APP_ID");
        }
        return v == null ? "" : v.trim();
    }

    /**
     * 解析实际生效的 AppSecret。
     *
     * @param fromWs true 取 websocket 节点
     * @return app-secret
     */
    public String resolveAppSecret(boolean fromWs) {
        String v = fromWs ? websocket.getAppSecret() : webhook.getAppSecret();
        if (v == null || v.isBlank()) {
            v = System.getenv("QQBOT_APP_SECRET");
        }
        return v == null ? "" : v.trim();
    }
}
