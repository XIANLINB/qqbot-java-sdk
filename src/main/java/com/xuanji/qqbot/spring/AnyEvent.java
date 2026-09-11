package com.xuanji.qqbot.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接收全部已支持事件（单聊/群聊/互动等）。
 * <p>
 * 与 {@code @GroupMessageCreateEvent} / {@code @C2cMessageCreateEvent} 等专用注解不同，
 * 本注解不做类型过滤，方法内用 {@code event.type()} 自行分支。
 * <p>
 * 方法第一参数建议为 {@code Event}；可选第二参数 {@code Bot}。
 * <pre>
 * &#64;AnyEvent
 * public void on(Event e, Bot bot) {
 *   if (e instanceof GroupMessageCreate g) { ... }
 *   else if (e instanceof C2cMessageCreate c) { ... }
 *   else if ("GROUP_ADD_ROBOT".equals(e.type())) { ... }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AnyEvent {
}
