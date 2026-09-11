package com.xuanji.qqbot.spring;

import com.xuanji.qqbot.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * xuanji QQ 机器人自动配置入口（多机器人）。
 * <p>
 * 业务工程引入 com.xuanji:qqbot-sdk 后，配置 xuanji.bots[] 或旧的单机器人节点即可。
 */
@AutoConfiguration
@ConditionalOnClass(Bot.class)
@EnableConfigurationProperties(XuanjiProperties.class)
@Import({
        XuanjiWebsocketAutoConfiguration.class,
        XuanjiWebhookAutoConfiguration.class,
        XuanjiListenerRegistrar.class
})
public class XuanjiAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(XuanjiAutoConfiguration.class);

    /**
     * 多机器人注册表：按配置创建全部 Bot 实例。
     *
     * @param props         配置
     * @param eventExecutor 事件回调线程池（xuanji.event.pool-size=0 时为 null，回退同步执行）
     * @return 注册表
     */
    @Bean(name = "xuanjiBotRegistry", destroyMethod = "close")
    public BotRegistry xuanjiBotRegistry(XuanjiProperties props,
                                         ObjectProvider<ExecutorService> eventExecutorProvider) {
        ExecutorService eventExecutor = eventExecutorProvider.getIfAvailable();
        BotRegistry registry = new BotRegistry();
        for (XuanjiProperties.BotEntry e : props.resolveBots()) {
            String appId = e.getAppId();
            String secret = e.getAppSecret();
            if (appId.isBlank() || secret.isBlank()) {
                log.warn("[xuanji] 机器人 {} ({}) 缺少 app-id/app-secret，已跳过", e.getName(), e.getMode());
                continue;
            }
            boolean webhook = "webhook".equalsIgnoreCase(e.getMode());
            String path = e.getPath();
            if (webhook && (path == null || path.isBlank())) {
                path = props.getWebhook().getPath();
            }
            if (webhook && (path == null || path.isBlank())) {
                throw new IllegalStateException(
                        "[xuanji] 机器人 " + e.getName() + " 使用 webhook 方式必须配置回调路径："
                                + "xuanji.bots[].path（推荐）或 xuanji.webhook.path");
            }
            XuanjiProperties.Media m = props.getMedia();
            Bot.Builder builder = Bot.builder().appId(appId).appSecret(secret)
                    .media(new com.xuanji.qqbot.MediaSpec(
                            m.getConnectTimeout(), m.getRequestTimeout(), m.getRetry()));
            if (eventExecutor != null) {
                builder.eventExecutor(eventExecutor);
            }
            Bot bot = builder.build();
            bot.events().setBotTag(e.getMode() + "/" + appId);
            bot.events().setLogRawPayload(props.getDebug().isRawPayload());
            registry.register(new BotRegistry.Entry(
                    e.getName(), appId, e.getMode(), bot,
                    XuanjiWebsocketAutoConfiguration.resolveIntents(e.getIntents()), secret, path));
            log.info("[xuanji] 创建机器人 {} ({}) appId={}{}",
                    e.getName(), e.getMode(), XuanjiWebsocketAutoConfiguration.mask(appId),
                    webhook ? " path=" + path : "");
        }
        if (registry.all().isEmpty()) {
            log.warn("[xuanji] 未配置任何机器人（xuanji.bots 或 xuanji.websocket/webhook）");
        }
        return registry;
    }

    /**
     * 事件回调线程池：全部机器人共享，保护 WS 收包线程与 Webhook HTTP 线程不被慢 handler 拖垮。
     * pool-size=0 时不创建（返回 null，事件在收线程同步执行）。
     *
     * @param props 配置
     * @return 线程池或 null
     */
    @Bean(name = "xuanjiEventExecutor", destroyMethod = "shutdown")
    @Nullable
    public ExecutorService xuanjiEventExecutor(XuanjiProperties props) {
        int n = Math.max(0, props.getEvent().getPoolSize());
        if (n == 0) {
            return null;
        }
        AtomicInteger idx = new AtomicInteger();
        return new ThreadPoolExecutor(n, n, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), r -> {
            Thread t = new Thread(r, "xuanji-event-" + idx.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }
}
