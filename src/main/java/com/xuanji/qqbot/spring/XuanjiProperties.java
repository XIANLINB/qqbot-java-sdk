package com.xuanji.qqbot.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * xuanji.* 配置绑定。全部节点可不配置（使用默认值），配置了以配置为准。
 * <p>
 * 推荐多机器人写法：
 * <pre>
 * xuanji:
 *   media:                          # 富媒体上传（可不配，默认 60s/60s/重试1）
 *     connect-timeout: 60s
 *     request-timeout: 60s
 *     retry: 1
 *   debug:
 *     raw-payload: false            # 原始报文打印（可不配）
 *   webhook:
 *     path: /xuanji/webhook         # 全局兜底路径（可不配）
 *   bots:
 *     - name: ws-bot
 *       mode: websocket
 *       app-id: xxx
 *       app-secret: yyy
 *     - name: hook-bot
 *       mode: webhook
 *       app-id: aaa
 *       app-secret: bbb
 *       path: /xuanji/webhook       # webhook 机器人必配（未配则回退全局，仍无则启动报错）
 * </pre>
 * 旧的单机器人写法（xuanji.websocket / xuanji.webhook）继续兼容：
 * bots 为空时自动从旧节点合成一条记录。
 */
@ConfigurationProperties(prefix = "xuanji")
public class XuanjiProperties {

    /** 多机器人列表 */
    private final List<BotEntry> bots = new ArrayList<>();

    /** 全局 webhook 回调路径（bots[].path 未配置时兜底；旧单机器人写法默认 /xuanji/webhook） */
    private final Webhook webhook = new Webhook();

    /** 调试开关 */
    private final Debug debug = new Debug();

    /** 富媒体上传配置 */
    private final Media media = new Media();

    /** 事件处理线程池配置 */
    private final Event event = new Event();

    /** @deprecated 旧单机器人 WebSocket 配置，bots 为空时兼容使用 */
    @Deprecated
    private final Websocket websocket = new Websocket();

    public List<BotEntry> getBots() {
        return bots;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public Websocket getWebsocket() {
        return websocket;
    }

    public Debug getDebug() {
        return debug;
    }

    public Media getMedia() {
        return media;
    }

    public Event getEvent() {
        return event;
    }

    /**
     * 解析全部机器人：bots 列表优先；为空时从旧 websocket/webhook 节点合成。
     *
     * @return 机器人配置列表
     */
    public List<BotEntry> resolveBots() {
        if (!bots.isEmpty()) {
            return bots;
        }
        List<BotEntry> out = new ArrayList<>();
        if (websocket.isEnable() && !resolveAppId(true).isBlank()) {
            BotEntry e = new BotEntry();
            e.setName("websocket");
            e.setMode("websocket");
            e.setAppId(resolveAppId(true));
            e.setAppSecret(resolveAppSecret(true));
            e.setIntents(websocket.getIntents());
            out.add(e);
        }
        if (webhook.isEnable() && !resolveAppId(false).isBlank()) {
            BotEntry e = new BotEntry();
            e.setName("webhook");
            e.setMode("webhook");
            e.setAppId(resolveAppId(false));
            e.setAppSecret(resolveAppSecret(false));
            // 旧写法保持历史行为：未显式配置 path 时使用历史默认路径
            e.setPath(webhook.getPath() == null || webhook.getPath().isBlank()
                    ? "/xuanji/webhook" : webhook.getPath().trim());
            out.add(e);
        }
        return out;
    }

    /**
     * 单个机器人条目。
     */
    public static class BotEntry {
        /** 机器人名称（日志与注册表键，默认取 app-id） */
        private String name = "";
        /** 接入方式：websocket | webhook */
        private String mode = "websocket";
        /** AppID；为空时回退环境变量 QQBOT_APP_ID */
        private String appId = "";
        /** AppSecret；为空时回退环境变量 QQBOT_APP_SECRET */
        private String appSecret = "";
        /** WebSocket 事件订阅名列表，默认 SDK 全部支持 */
        private List<String> intents = new ArrayList<>();
        /** Webhook 回调路径；mode=webhook 时必配（或配全局 xuanji.webhook.path），websocket 忽略 */
        private String path = "";

        public boolean isEnable() {
            return true;
        }

        public String getName() {
            return name == null || name.isBlank() ? appId : name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getAppId() {
            return appId == null || appId.isBlank()
                    ? System.getenv("QQBOT_APP_ID") == null ? "" : System.getenv("QQBOT_APP_ID").trim()
                    : appId.trim();
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret == null || appSecret.isBlank()
                    ? System.getenv("QQBOT_APP_SECRET") == null ? "" : System.getenv("QQBOT_APP_SECRET").trim()
                    : appSecret.trim();
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

        /** @return Webhook 回调路径（trim 后；空表示未配置） */
        public String getPath() {
            return path == null ? "" : path.trim();
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    /** WebSocket 接入配置（旧单机器人兼容 / intents 默认值参考）。 */
    public static class Websocket {
        /** 是否启用 */
        private boolean enable = false;
        /** AppID */
        private String appId = "";
        /** AppSecret */
        private String appSecret = "";
        /** 事件订阅名列表，默认 SDK 全部支持 */
        private List<String> intents = new ArrayList<>();
        /** 首连失败是否让应用启动失败；false 时转后台退避重试（默认 true） */
        private boolean failFast = true;

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

        public boolean isFailFast() {
            return failFast;
        }

        public void setFailFast(boolean failFast) {
            this.failFast = failFast;
        }
    }

    /** Webhook 接入配置。 */
    public static class Webhook {
        /** 是否启用（旧单机器人兼容） */
        private boolean enable = false;
        private String appId = "";
        private String appSecret = "";
        /** 全局回调路径；bots[].path 未配置时兜底。空表示未配置（旧单机器人写法由 resolveBots 兜底历史默认） */
        private String path = "";

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

    /** 调试开关。 */
    public static class Debug {
        /** 是否打印原始报文（WS 与 Webhook 统一在 Events 输出），默认 false */
        private boolean rawPayload = false;

        public boolean isRawPayload() {
            return rawPayload;
        }

        public void setRawPayload(boolean rawPayload) {
            this.rawPayload = rawPayload;
        }
    }

    /** 富媒体上传配置（连接超时/请求超时/重试，独立于普通 API 的超时）。 */
    public static class Media {
        /** 建立 HTTP 连接超时，默认 60 秒 */
        private Duration connectTimeout = Duration.ofSeconds(60);
        /** 单次请求超时，默认 60 秒 */
        private Duration requestTimeout = Duration.ofSeconds(60);
        /** 网络类失败（连接失败/请求超时/IO）后的额外重试次数，默认 1；0=不重试；业务错误不重试 */
        private int retry = 1;

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }

        public int getRetry() {
            return retry;
        }

        public void setRetry(int retry) {
            this.retry = retry;
        }
    }

    /** 事件回调线程池配置（全部机器人共享）。 */
    public static class Event {
        /** 事件回调线程池大小，默认 8；0=在 WS/Webhook 收线程上同步执行（handler 慢会阻塞收包，不建议） */
        private int poolSize = 8;

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
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
