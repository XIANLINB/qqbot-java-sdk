package com.xuanji.qqbot.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import com.xuanji.qqbot.Bot;

/**
 * xuanji QQ 机器人自动配置入口。
 * <p>
 * 业务工程：
 * <pre>
 * // pom
 * // com.xuanji:Bot-sdk
 *
 * // yml: xuanji.websocket.enable=true + app-id/secret
 *
 * // 插件
 * &#64;Xuanji &#64;Component
 * class Bot {
 *   &#64;GroupAtMessageCreateEvent
 *   &#64;Command("地图工坊")
 *   void on(GroupAtMessageCreate e, Bot bot) { ... }
 * }
 * </pre>
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
}
