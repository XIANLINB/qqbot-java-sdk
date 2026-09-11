package com.xuanji.qqbot.exception;

/**
 * SDK 基础非检查异常。
 * <p>
 * 所有由 SDK 抛出的运行时错误的父类。
 */
public class QqBotException extends RuntimeException {
    /**
     * @param message 可读错误说明
     */
    public QqBotException(String message) {
        super(message);
    }

    /**
     * @param message 可读错误说明
     * @param cause   底层原因
     */
    public QqBotException(String message, Throwable cause) {
        super(message, cause);
    }
}
