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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 容器刷新后扫描 {@link Xuanji}（及废弃的 {@link XuanjiComponent}）插件，
 * 将事件注解方法注册到注册表内<b>每一个</b>机器人的事件总线上。
 * <p>
 * 同一套插件代码作用于全部机器人；方法参数 {@code Bot} 注入的是收到该事件的那台机器人，
 * 因此 {@code bot.reply(event, ...)} 天然路由正确。
 * 每个插件类的注册结果合并为一条日志。
 */
public class XuanjiListenerRegistrar
        implements SmartInitializingSingleton, ApplicationListener<ContextRefreshedEvent>, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(XuanjiListenerRegistrar.class);

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Map<Object, Boolean> registered = new ConcurrentHashMap<>();

    @Override
    public void afterSingletonsInstantiated() {
        // 注册放到 ContextRefreshedEvent，保证全部 Bot 已创建
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
        BotRegistry registry = firstOrNull(ctx, BotRegistry.class);
        if (beans.isEmpty()) {
            log.info("[xuanji] 未找到 @Xuanji 插件 Bean");
            return;
        }
        if (registry == null || registry.all().isEmpty()) {
            log.warn("[xuanji] 找到 {} 个插件，但未配置任何机器人（xuanji.bots）", beans.size());
            return;
        }
        List<String> botTags = new ArrayList<>();
        for (BotRegistry.Entry e : registry.all()) {
            botTags.add(e.tag());
        }
        int pluginCount = 0;
        int listenerCount = 0;
        // 插件顺序：类上的 @Order / Ordered（Spring 语义），默认最后
        List<Object> pluginBeans = new ArrayList<>(beans.values());
        org.springframework.core.annotation.AnnotationAwareOrderComparator.sort(pluginBeans);
        for (Object bean : pluginBeans) {
            if (registered.putIfAbsent(bean, Boolean.TRUE) != null) {
                continue;
            }
            List<String> summaries = new ArrayList<>();
            int[] count = {0};
            // 方法顺序：方法上的 @Order，默认最后；同名方法按名称稳定排序
            List<Method> methods = new ArrayList<>();
            ReflectionUtils.doWithMethods(bean.getClass(), methods::add);
            methods.sort(java.util.Comparator
                    .comparingInt(XuanjiListenerRegistrar::methodOrder)
                    .thenComparing(Method::getName));
            for (Method method : methods) {
                String desc = register(registry, bean, method);
                if (desc != null) {
                    summaries.add(desc);
                    count[0]++;
                }
            }
            if (!summaries.isEmpty()) {
                log.info("[xuanji] 注册插件 {}: {} 个监听 [{}] -> {}",
                        bean.getClass().getSimpleName(), count[0],
                        String.join(", ", summaries), String.join(", ", botTags));
            }
            pluginCount++;
            listenerCount += count[0];
        }
        log.info("[xuanji] 已注册 {} 个插件 / {} 个监听，作用于机器人 {}",
                pluginCount, listenerCount, botTags);
    }

    /** 方法上的 @Order 值；无注解为 Ordered.LOWEST_PRECEDENCE。 */
    private static int methodOrder(Method m) {
        org.springframework.core.annotation.Order o = m.getAnnotation(org.springframework.core.annotation.Order.class);
        return o != null ? o.value() : org.springframework.core.Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 把单个方法注册到全部机器人，返回日志描述；非事件方法返回 null。
     */
    private String register(BotRegistry registry, Object bean, Method method) {
        boolean any = method.isAnnotationPresent(AnyEvent.class);
        XuanjiEventBindings.Binding binding = any ? null : XuanjiEventBindings.find(method);
        if (!any && binding == null) {
            return null;
        }
        method.setAccessible(true);
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 0) {
            throw new IllegalStateException("事件方法至少需要一个事件参数: " + method);
        }
        boolean useBot = false;
        for (int i = 1; i < params.length; i++) {
            if (params[i] == Bot.class) {
                useBot = true;
            }
        }
        List<Command> commands = XuanjiEventBindings.commands(method);
        boolean requireAtBot = commands.stream().anyMatch(c -> c.at() == At.REQUIRED);
        boolean requireNotAtBot = commands.stream().anyMatch(c -> c.at() == At.NOT);
        if (requireAtBot && requireNotAtBot) {
            throw new IllegalStateException("@Command 的 at=REQUIRED 与 at=NOT 不能同时使用: " + method);
        }
        if ((requireAtBot || requireNotAtBot) && binding != null
                && !binding.eventType().isAssignableFrom(com.xuanji.qqbot.event.GroupMessageCreate.class)
                && !binding.eventType().isAssignableFrom(com.xuanji.qqbot.event.GroupAtMessageCreate.class)) {
            log.warn("[xuanji] 方法 {} 的事件 {} 不支持 at 判断，该属性已忽略",
                    method.getName(), binding.eventName());
        }

        for (BotRegistry.Entry e : registry.all()) {
            final Bot bot = e.bot();
            final Events events = bot.events();
            final boolean injectBot = useBot;
            final java.util.function.Function<com.xuanji.qqbot.event.Event, String> extractor =
                    any ? null : binding.contentExtractor();
            if (any) {
                final Class<?> eventParam = params[0];
                events.onAny(event -> {
                    if (event == null) {
                        return;
                    }
                    if (eventParam != Event.class && !eventParam.isInstance(event)) {
                        return;
                    }
                    invoke(bean, method, event, bot, injectBot, null);
                });
            } else {
                events.on(binding.eventType(), event -> {
                    if (event == null || !binding.eventType().isInstance(event)) {
                        return;
                    }
                    if (requireAtBot && !isAtBot(event)) {
                        return;
                    }
                    if (requireNotAtBot && isAtBot(event)) {
                        return;
                    }
                    String content = extractor.apply(event);
                    if (!XuanjiEventBindings.matchesCommands(commands, content)) {
                        return;
                    }
                    invoke(bean, method, event, bot, injectBot, content);
                });
            }
        }

        String target = any ? "@AnyEvent" : binding.eventName();
        String flags = (requireAtBot ? ",at=REQUIRED" : "") + (requireNotAtBot ? ",at=NOT" : "");
        String cmdInfo = commands.isEmpty() ? "" : ",cmd" + commands.size();
        String flagPart = flags.isEmpty() ? "" : flags;
        return method.getName() + "[" + target + flagPart + cmdInfo + "]";
    }

    private static boolean isAtBot(Event event) {
        if (event instanceof com.xuanji.qqbot.event.GroupMessageCreate g) {
            return g.looksLikeAtBot();
        }
        return event instanceof com.xuanji.qqbot.event.GroupAtMessageCreate;
    }

    /**
     * 执行监听方法：参数注入事件/Bot/正文(String)；返回 String 或 PostMessage 时自动被动回复。
     */
    private static void invoke(Object bean, Method method, Event event, Bot bot,
                               boolean injectBot, String content) {
        try {
            Class<?>[] params = method.getParameterTypes();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                if (i == 0) {
                    args[i] = event;
                } else if (params[i] == Bot.class) {
                    args[i] = bot;
                } else if (params[i] == String.class) {
                    args[i] = content;
                } else {
                    args[i] = null;
                }
            }
            Object result = method.invoke(bean, args);
            if (result instanceof String s && !s.isBlank()) {
                bot.reply(event, s);
            } else if (result instanceof com.xuanji.qqbot.model.message.PostMessage pm) {
                bot.reply(event, pm);
            }
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
