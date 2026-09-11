package com.xuanji.qqbot.examples;

import com.xuanji.qqbot.Bot;
import com.xuanji.qqbot.event.EventType;
import com.xuanji.qqbot.event.GroupAtMessageCreate;
import com.xuanji.qqbot.event.Intents;
/**
 * 最简群聊 Echo 机器人。需要环境变量 QQBOT_APP_ID / QQBOT_APP_SECRET。
 */
public final class GroupEchoMain {
    public static void main(String[] args) throws Exception {
        String appId = System.getenv("QQBOT_APP_ID");
        String secret = System.getenv("QQBOT_APP_SECRET");
        if (appId == null || secret == null) {
            System.err.println("请先设置环境变量 QQBOT_APP_ID / QQBOT_APP_SECRET");
            return;
        }

        try (Bot bot = Bot.builder().appId(appId).appSecret(secret).build()) {
            bot.events().on(EventType.GROUP_AT_MESSAGE_CREATE, event -> {
                if (event instanceof GroupAtMessageCreate msg) {
                    bot.passiveMsgGroup(msg.groupOpenidOrId(), msg.id(), "echo: " + msg.contentAsText(), 1);
                }
            });

            bot.connectGateway(Intents.groupAndC2c());
            System.out.println("已就绪，等待群内 @ 消息… Ctrl+C 退出。");
            Thread.currentThread().join();
        }
    }

    private GroupEchoMain() {
    }
}
