package com.xuanji.qqbot.spring;

/**
 * 指令对「@ 机器人」的要求（三态，写在 {@link Command#at()} 上）。
 * <ul>
 *   <li>{@link #REQUIRED}：必须 @ 本机器人才触发（群全量模式下建议显式声明）</li>
 *   <li>{@link #ANY}：默认；不关心是否 @</li>
 *   <li>{@link #NOT}：必须没有 @ 本机器人（如忽略日志、免打扰统计）</li>
 * </ul>
 * 仅对群消息事件生效；单聊等事件上声明 REQUIRED/NOT 会被忽略并告警。
 */
public enum At {
    /** 必须 @ 机器人 */
    REQUIRED,
    /** 不关心（默认） */
    ANY,
    /** 必须未 @ 机器人 */
    NOT
}
