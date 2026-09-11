package com.xuanji.qqbot.spring;

import com.xuanji.qqbot.Bot;
import com.xuanji.qqbot.event.Event;
import com.xuanji.qqbot.event.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 容器刷新后扫描 {@link Xuanji}（及废弃的 {@link XuanjiComponent}）插件，
 * 将 {@code @GroupAtMessageCreateEvent} 等方法挂到 SDK 事件总线。
 * 支持 {@link Command} 前缀/后缀/正则过滤。
 */
public class XuanjiListenerRegistrar
        implements SmartInitializingSingleton, ApplicationListener<ContextRefreshedEvent>, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(XuanjiListenerRegistrar.class);

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Map<Object, Boolean> registered = new ConcurrentHashMap<>();

    @Override
    public void afterSingletonsInstantiated() {
        // 注册放到 ContextRefreshedEvent，保证 Bot/Events 已就绪
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        try {
            registerAll(event.getApplicationContext());
        } catch (Exception e) {
            log.error("[xuanji] 注册监听失败: {}", e.getMessage(), e);
        }
    }

    private void registerAll(ApplicationContext ctx) {
        Map<String, Object> beans = ctx.getBeansWithAnnotation(Xuanji.class);
        if (beans.isEmpty()) {
            beans = ctx.getBeansWithAnnotation(XuanjiComponent.class);
        }
        Events events = firstOrNull(ctx, Events.class);
        Bot bot = firstOrNull(ctx, Bot.class);
        if (beans.isEmpty()) {
            log.info("[xuanji] 未找到 @Xuanji 插件 Bean");
            return;
        }
        if (events == null) {
            log.warn("[xuanji] 找到 {} 个插件，但无 Events Bean（请 enable xuanji.websocket 或 webhook）", beans.size());
            return;
        }
        int n = 0;
        for (Object bean : beans.values()) {
            if (registered.putIfAbsent(bean, Boolean.TRUE) != null) {
                continue;
            }
            ReflectionUtils.doWithMethods(bean.getClass(), method -> register(events, bot, bean, method));
            n++;
        }
        events.onAny(e -> {
            if (e.type() != null && e.type().contains("MESSAGE")) {
                log.info("[xuanji] 事件 type={} id={}", e.type(), e.eventId());
            }
        });
        log.info("[xuanji] 已注册 {} 个 @Xuanji 插件", n);
    }

    private void register(Events events, Bot bot, Object bean, Method method) {
        if (method.isAnnotationPresent(AnyEvent.class)) {
            registerAny(events, bot, bean, method);
            return;
        }
        XuanjiEventBindings.Binding binding = XuanjiEventBindings.find(method);
        if (binding == null) {
            return;
        }
        method.setAccessible(true);
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 0) {
            throw new IllegalStateException("事件方法至少需要一个事件参数: " + method);
        }
        boolean injectBot = false;
        for (int i = 1; i < params.length; i++) {
            if (params[i] == Bot.class) {
                injectBot = true;
            }
        }
        final boolean useBot = injectBot;
        List<Command> commands = XuanjiEventBindings.commands(method);
        boolean requireAtBot = method.isAnnotationPresent(AtBot.class);
        boolean requireNotAtBot = method.isAnnotationPresent(NotAtBot.class);
        if (requireAtBot && requireNotAtBot) {
            throw new IllegalStateException("@AtBot 与 @NotAtBot 不能同时使用: " + method);
        }
        events.on(binding.eventType(), event -> {
            if (event == null || !binding.eventType().isInstance(event)) {
                return;
            }
            boolean at = isAtBot(event);
            if (requireAtBot && !at) {
                return;
            }
            if (requireNotAtBot && at) {
                return;
            }
            String content = binding.contentExtractor().apply(event);
            if (!XuanjiEventBindings.matchesCommands(commands, content)) {
                return;
            }
            invoke(bean, method, event, bot, useBot);
        });
        String flags = (requireAtBot ? " @AtBot" : "") + (requireNotAtBot ? " @NotAtBot" : "");
        log.info("[xuanji] 注册 {}#{} {}{} cmdRules={}",
                bean.getClass().getSimpleName(), method.getName(), binding.eventName(), flags, commands.size());
    }

    private void registerAny(Events events, Bot bot, Object bean, Method method) {
        method.setAccessible(true);
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 0) {
            throw new IllegalStateException("@AnyEvent 方法至少需要一个 Event 参数: " + method);
        }
        if (!Event.class.isAssignableFrom(params[0])) {
            throw new IllegalStateException("@AnyEvent 第一个参数必须是 Event 或其子类: " + method);
        }
        boolean injectBot = false;
        for (int i = 1; i < params.length; i++) {
            if (params[i] == Bot.class) {
                injectBot = true;
            }
        }
        final boolean useBot = injectBot;
        events.onAny(event -> {
            if (event == null) {
                return;
            }
            if (!params[0].isInstance(event) && params[0] != Event.class) {
                return;
            }
            invoke(bean, method, event, bot, useBot);
        });
        log.info("[xuanji] 注册 {}#{} @AnyEvent", bean.getClass().getSimpleName(), method.getName());
    }

    private static boolean isAtBot(Event event) {
        if (event instanceof com.xuanji.qqbot.event.GroupMessageCreate g) {
            return g.looksLikeAtBot();
        }
        if (event instanceof com.xuanji.qqbot.event.GroupAtMessageCreate) {
            return true;
        }
        return false;
    }

    private static void invoke(Object bean, Method method, Event event, Bot bot, boolean injectBot) {
        try {
            Class<?>[] params = method.getParameterTypes();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                if (i == 0) {
                    args[i] = event;
                } else if (params[i] == Bot.class) {
                    args[i] = bot;
                } else {
                    args[i] = null;
                }
            }
            method.invoke(bean, args);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("xuanji 监听方法执行失败: " + method, cause);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T firstOrNull(ApplicationContext ctx, Class<T> type) {
        Map<String, T> map = ctx.getBeansOfType(type);
        return map.isEmpty() ? null : map.values().iterator().next();
    }

    @Override
    public void destroy() {
        // no-op
    }
}
