package com.xuanji.qqbot.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 群全量消息（GROUP_MESSAGE_CREATE），需平台全量群消息权限。
 * <p>
 * 方法第一参数：{@code GroupMessageCreate}。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GroupMessageCreateEvent {
}
