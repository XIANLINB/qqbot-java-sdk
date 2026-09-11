package com.xuanji.qqbot.spring;

import com.xuanji.qqbot.Bot;
import com.xuanji.qqbot.event.Events;
import com.xuanji.qqbot.event.Intents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * xuanji.websocket.enable=true 时装配 Bot 并启动网关。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(XuanjiProperties.class)
@ConditionalOnProperty(prefix = "xuanji", name = "websocket.enable", havingValue = "true")
public class XuanjiWebsocketAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(XuanjiWebsocketAutoConfiguration.class);

    /**
     * @param props 配置
     * @return Bot
     */
    @Bean(name = "xuanjiQqBot", destroyMethod = "close")
    @ConditionalOnMissingBean(Bot.class)
    public Bot xuanjiQqBot(XuanjiProperties props) {
        String appId = props.resolveAppId(true);
        String secret = props.resolveAppSecret(true);
        if (appId.isBlank() || secret.isBlank()) {
            throw new IllegalStateException(
                    "xuanji.websocket.app-id / app-secret 未配置，或未设置 QQBOT_APP_ID / QQBOT_APP_SECRET");
        }
        log.info("[xuanji] 创建 Bot（websocket）appId={}", mask(appId));
        return Bot.builder().appId(appId).appSecret(secret).build();
    }

    /**
     * @param bot Bot
     * @return Events
     */
    @Bean
    @ConditionalOnMissingBean(Events.class)
    public Events xuanjiEvents(Bot bot) {
        return bot.events();
    }

    /**
     * @param props 配置
     * @param bot   客户端
     * @return 启动器
     */
    @Bean
    public XuanjiWsLifecycle xuanjiWsLifecycle(XuanjiProperties props, Bot bot) {
        return new XuanjiWsLifecycle(bot, resolveIntents(props));
    }

    /**
     * yml intents 名 → 官方位。
     *
     * @param props 配置
     * @return 组合 intents
     */
    /**
     * 解析 WebSocket intents。
     * yml 未配置时默认订阅 SDK 支持的全部事件（单聊+群聊+互动）。
     *
     * @param props 配置
     * @return intents 位掩码
     */
    public static long resolveIntents(XuanjiProperties props) {
        List<String> names = props.getWebsocket().getIntents();
        if (names == null || names.isEmpty()) {
            log.info("[xuanji] 未配置 intents，默认订阅 SDK 全部事件: {}", Intents.groupC2cAndInteraction());
            return Intents.groupC2cAndInteraction();
        }
        Map<String, Long> map = new HashMap<>();
        map.put("GUILDS", Intents.GUILDS);
        map.put("GUILD_MEMBERS", Intents.GUILD_MEMBERS);
        map.put("GUILD_MESSAGES", Intents.GUILD_MESSAGES);
        map.put("GUILD_MESSAGE_REACTIONS", Intents.GUILD_MESSAGE_REACTIONS);
        map.put("DIRECT_MESSAGE", Intents.DIRECT_MESSAGE);
        map.put("GROUP_AND_C2C_EVENT", Intents.GROUP_AND_C2C_EVENT);
        map.put("GROUP_AND_C2C", Intents.GROUP_AND_C2C_EVENT);
        map.put("GROUP_MEMBER_EVENT", Intents.GROUP_MEMBER_EVENT);
        map.put("GROUP_MEMBER", Intents.GROUP_MEMBER_EVENT);
        map.put("INTERACTION", Intents.INTERACTION);
        map.put("MESSAGE_AUDIT", Intents.MESSAGE_AUDIT);
        map.put("FORUMS_EVENT", Intents.FORUMS_EVENT);
        map.put("AUDIO_ACTION", Intents.AUDIO_ACTION);
        map.put("PUBLIC_GUILD_MESSAGES", Intents.PUBLIC_GUILD_MESSAGES);
        long bits = 0;
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            Long bit = map.get(name.trim().toUpperCase(Locale.ROOT));
            if (bit != null) {
                bits |= bit;
            }
        }
        return bits == 0 ? Intents.groupC2cAndInteraction() : bits;
    }

    static String mask(String s) {
        if (s == null || s.length() < 4) {
            return "***";
        }
        return s.substring(0, 2) + "***" + s.substring(s.length() - 2);
    }
}
