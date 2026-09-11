package com.xuanji.qqbot.spring;

import com.xuanji.qqbot.Bot;
import com.xuanji.qqbot.event.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * xuanji.webhook.enable=true 时装配 Bot（不连 WS），由 {@link XuanjiWebhookController} 收回调。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(XuanjiProperties.class)
@Import(XuanjiWebhookController.class)
@ConditionalOnProperty(prefix = "xuanji", name = "webhook.enable", havingValue = "true")
public class XuanjiWebhookAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(XuanjiWebhookAutoConfiguration.class);

    /**
     * @param props 配置
     * @return Bot
     */
    @Bean(name = "xuanjiQqBot", destroyMethod = "close")
    @ConditionalOnMissingBean(Bot.class)
    public Bot xuanjiQqBotWebhook(XuanjiProperties props) {
        String appId = props.resolveAppId(false);
        String secret = props.resolveAppSecret(false);
        if (appId.isBlank() || secret.isBlank()) {
            throw new IllegalStateException("xuanji.webhook.app-id / app-secret 未配置");
        }
        log.info("[xuanji] 创建 Bot（webhook）appId={}", XuanjiWebsocketAutoConfiguration.mask(appId));
        return Bot.builder().appId(appId).appSecret(secret).build();
    }

    /**
     * @param bot Bot
     * @return Events
     */
    @Bean
    @ConditionalOnMissingBean(Events.class)
    public Events xuanjiWebhookEvents(Bot bot) {
        return bot.events();
    }
}
