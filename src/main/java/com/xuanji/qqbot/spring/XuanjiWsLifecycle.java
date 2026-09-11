package com.xuanji.qqbot.spring;

import com.xuanji.qqbot.ws.GatewaySupervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 应用启动后连接注册表内全部 websocket 机器人。
 * 首连失败行为由 {@code xuanji.websocket.fail-fast} 决定：true 抛异常终止启动，false 转后台退避重试。
 */
public class XuanjiWsLifecycle implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(XuanjiWsLifecycle.class);

    private final BotRegistry registry;
    private final XuanjiProperties props;

    /**
     * @param registry 多机器人注册表
     * @param props    配置
     */
    public XuanjiWsLifecycle(BotRegistry registry, XuanjiProperties props) {
        this.registry = registry;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        var wsBots = registry.byMode("websocket");
        if (wsBots.isEmpty()) {
            log.info("[xuanji] 无 websocket 机器人，跳过网关连接");
            return;
        }
        boolean failFast = props.getWebsocket().isFailFast();
        for (BotRegistry.Entry e : wsBots) {
            try {
                GatewaySupervisor supervisor = e.bot().connectGateway(e.intents(), failFast);
                log.info("[xuanji][websocket][{}] 网关已连接 session={}",
                        e.tag(), supervisor.session());
            } catch (Exception ex) {
                log.error("[xuanji][websocket][{}] 网关连接失败: {}", e.tag(), ex.getMessage(), ex);
                if (failFast) {
                    throw new IllegalStateException("xuanji websocket 连接失败: " + e.name(), ex);
                }
            }
        }
    }
}
