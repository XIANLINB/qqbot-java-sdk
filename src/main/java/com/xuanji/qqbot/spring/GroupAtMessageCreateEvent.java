package com.xuanji.qqbot.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 群内「@ 机器人」消息事件（GROUP_AT_MESSAGE_CREATE）。
 * <p>
 * 方法第一参数：{@code GroupAtMessageCreate}；可选第二参数 {@code Bot}。
 * 可叠加 {@link Command} 做内容过滤。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GroupAtMessageCreateEvent {
}
