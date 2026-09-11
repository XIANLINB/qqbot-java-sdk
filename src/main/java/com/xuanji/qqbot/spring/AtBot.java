package com.xuanji.qqbot.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 仅当消息 @ 了本机器人时触发。
 * 全量 GROUP_MESSAGE_CREATE 看 looksLikeAtBot；AT 通道恒为 true。
 * 可与 @Command 叠加。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AtBot {
}
