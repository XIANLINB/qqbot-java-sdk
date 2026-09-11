package com.xuanji.qqbot.exception;

import java.io.IOException;

/**
 * 网络/IO 传输失败（非业务 err_code）。
 */
public class TransportException extends QqBotException {
    /**
     * @param message 错误说明
     * @param cause   底层 IOException
     */
    public TransportException(String message, IOException cause) {
        super(message, cause);
    }

    /**
     * @param message 错误说明
     */
    public TransportException(String message) {
        super(message);
    }
}
