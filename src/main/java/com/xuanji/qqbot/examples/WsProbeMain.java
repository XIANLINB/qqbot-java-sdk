package com.xuanji.qqbot.examples;

import com.xuanji.qqbot.Bot;
import com.xuanji.qqbot.api.GatewayApi;
import com.xuanji.qqbot.event.Event;
import com.xuanji.qqbot.event.Events;
import com.xuanji.qqbot.event.Intents;
import com.xuanji.qqbot.ws.GatewaySupervisor;

/**
 * WebSocket 链路探活：拉网关、连 WSS、打印事件。
 * 用法：设置 QQBOT_APP_ID / QQBOT_APP_SECRET 后运行。
 */
public final class WsProbeMain {
    public static void main(String[] args) throws Exception {
        String appId = System.getenv("QQBOT_APP_ID");
        String secret = System.getenv("QQBOT_APP_SECRET");
        System.out.println("[probe] appId len=" + (appId == null ? -1 : appId.length())
                + " secret len=" + (secret == null ? -1 : secret.length()));

        try (Bot bot = Bot.builder().appId(appId).appSecret(secret).build()) {
            String token = bot.accessToken();
            System.out.println("[probe] access_token len=" + (token == null ? -1 : token.length())
                    + " prefix=" + (token == null || token.length() < 4 ? "?" : token.substring(0, 4)));

            GatewayApi.GatewayUrl url = bot.gateway().gateway();
            System.out.println("[probe] gateway url=" + (url == null ? null : url.url()));
            GatewayApi.GatewayBot botGw = bot.gateway().gatewayBot();
            System.out.println("[probe] gatewayBot shards=" + (botGw == null ? null : botGw.shards())
                    + " url=" + (botGw == null ? null : botGw.url()));

            bot.events().onAny((Event e) ->
                    System.out.println("[probe] event " + e.type() + " id=" + e.eventId()));

            GatewaySupervisor sup = bot.connectGateway(Intents.groupAndC2c());
            System.out.println("[probe] connected session=" + sup.session());

            // 保持 20 秒收事件
            Thread.sleep(20_000);
            System.out.println("[probe] final session=" + sup.session());
        }
        System.out.println("[probe] done");
    }

    private WsProbeMain() {
    }
}
