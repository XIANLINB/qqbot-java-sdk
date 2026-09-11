package com.xuanji.qqbot.spring;

import com.xuanji.qqbot.Bot;
import com.xuanji.qqbot.ws.GatewaySupervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * websocket 模式：应用启动后连接官方网关。
 */
public class XuanjiWsLifecycle implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(XuanjiWsLifecycle.class);

    private final Bot Bot;
    private final long intents;

    /**
     * @param Bot   客户端
     * @param intents 事件位
     */
    public XuanjiWsLifecycle(Bot Bot, long intents) {
        this.Bot = Bot;
        this.intents = intents;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            GatewaySupervisor supervisor = Bot.connectGateway(intents);
            log.info("[xuanji] WebSocket 网关已连接 session={}", supervisor.session());
        } catch (Exception e) {
            log.error("[xuanji] WebSocket 网关连接失败: {}", e.getMessage(), e);
            throw new IllegalStateException("xuanji websocket 连接失败", e);
        }
    }
}
