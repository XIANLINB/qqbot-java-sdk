package com.xuanji.qqbot.spring;

import com.xuanji.qqbot.event.Intents;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * WebSocket 模式：为注册表中全部 websocket 机器人建立网关连接。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "xuanji", name = "websocket.enable", havingValue = "true", matchIfMissing = true)
public class XuanjiWebsocketAutoConfiguration {

    /**
     * 启动时连接全部 websocket 机器人。
     *
     * @param registry 注册表
     * @param props    配置
     * @return 生命周期
     */
    @Bean
    public XuanjiWsLifecycle xuanjiWsLifecycle(BotRegistry registry, XuanjiProperties props) {
        return new XuanjiWsLifecycle(registry, props);
    }

    /**
     * yml intents 名 → 官方位。
     *
     * @param names 订阅名列表，空则默认 SDK 全部支持事件
     * @return intents 位掩码
     */
    public static long resolveIntents(java.util.List<String> names) {
        if (names == null || names.isEmpty()) {
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
