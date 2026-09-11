package com.xuanji.qqbot.spring;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Webhook 模式：按每台 webhook 机器人的回调路径挂载入口 Servlet，
 * 处理逻辑统一交给 {@link XuanjiWebhookController}（按 X-Bot-Appid 头路由到具体机器人）。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "xuanji", name = "webhook.enable", havingValue = "true", matchIfMissing = true)
public class XuanjiWebhookAutoConfiguration {

    /**
     * @param registry 多机器人注册表
     * @return 处理器
     */
    @Bean
    public XuanjiWebhookController xuanjiWebhookController(BotRegistry registry) {
        return new XuanjiWebhookController(registry);
    }

    /**
     * 将 webhook 处理器挂载到每台 webhook 机器人的回调路径（多台同路径自动去重）。
     *
     * @param controller 处理器
     * @param registry   多机器人注册表
     * @return Servlet 注册
     */
    @Bean
    public ServletRegistrationBean<HttpServlet> xuanjiWebhookServlet(XuanjiWebhookController controller,
                                                                     BotRegistry registry) {
        Set<String> paths = new LinkedHashSet<>();
        for (BotRegistry.Entry e : registry.byMode("webhook")) {
            if (e.path() != null && !e.path().isBlank()) {
                paths.add(e.path());
            }
        }
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                controller.service(req, resp);
            }
        };
        ServletRegistrationBean<HttpServlet> reg = new ServletRegistrationBean<>(servlet);
        reg.setName("xuanjiWebhookServlet");
        reg.setUrlMappings(new ArrayList<>(paths));
        return reg;
    }
}
