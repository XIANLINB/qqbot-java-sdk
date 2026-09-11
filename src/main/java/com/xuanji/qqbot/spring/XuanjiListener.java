package com.xuanji.qqbot.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法为 QQ 事件回调。
 * 方法参数：第一个为具体事件类型（如 GroupAtMessageCreate），可选 Bot。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XuanjiListener {
    /**
     * 事件名，如 GROUP_AT_MESSAGE_CREATE；空串表示按方法第一参数类型推断。
     */
    String value() default "";
}
