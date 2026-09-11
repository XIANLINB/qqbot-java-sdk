package com.xuanji.qqbot.spring;

/**
 * {@link Command} 匹配方式。必须显式指定，避免 value 语义含糊。
 */
public enum CommandMatchType {
    /** 内容以给定字符串开头（前缀） */
    PREFIX,
    /** 内容以给定字符串结尾（后缀） */
    SUFFIX,
    /** 内容与给定字符串完全一致（去首尾空白后） */
    EXACT,
    /** 内容整串匹配正则 */
    REGEX
}
