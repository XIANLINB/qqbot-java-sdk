package com.xuanji.qqbot.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消息内容过滤（可与消息事件注解叠加）。多个 {@code @Command} 为「或」。
 * <p>
 * <b>必须</b>指定 {@link #type()}：{@link CommandMatchType#PREFIX 前缀} /
 * {@link CommandMatchType#SUFFIX 后缀} /
 * {@link CommandMatchType#EXACT 精确} /
 * {@link CommandMatchType#REGEX 正则}。
 * <pre>
 * &#64;Command(type = CommandMatchType.PREFIX, value = "地图工坊")
 * &#64;Command(type = CommandMatchType.EXACT, value = "你好")
 * &#64;Command(type = CommandMatchType.SUFFIX, value = "！")
 * &#64;Command(type = CommandMatchType.REGEX, value = "签到\\d+")
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Command.Commands.class)
@Documented
public @interface Command {
    /**
     * 匹配方式（必填）。
     */
    CommandMatchType type();

    /**
     * 匹配串或正则（必填）。
     */
    String value();

    /**
     * 是否忽略大小写，默认 true。
     */
    boolean ignoreCase() default true;

    /**
     * 容器注解（勿手写）。
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface Commands {
        Command[] value();
    }
}
