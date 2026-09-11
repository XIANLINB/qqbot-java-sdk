package com.xuanji.qqbot.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记本类为 xuanji 机器人插件（Shiro 的 {@code @Shiro} 对应物）。
 * 其中带事件注解的方法会被自动注册。
 * <p>
 * 通常还需是 Spring {@code @Component}（或 {@code @Service}）以便进入容器。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Xuanji {
}
