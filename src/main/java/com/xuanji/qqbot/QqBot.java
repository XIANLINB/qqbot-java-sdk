package com.xuanji.qqbot;

import com.xuanji.qqbot.http.Transport;

import java.util.concurrent.Executor;

/**
 * @deprecated 请使用 {@link Bot}。本类仅作兼容别名，构建请用 {@link Bot#builder()}。
 */
@Deprecated
public final class QqBot {
    private final Bot bot;

    private QqBot(Bot bot) {
        this.bot = bot;
    }

    /**
     * @param options 配置
     * @return 兼容包装
     */
    public static QqBot create(QqBotOptions options) {
        return new QqBot(Bot.create(options));
    }

    /**
     * @deprecated 用 {@link Bot#builder()}
     */
    @Deprecated
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return 内部 Bot
     */
    public Bot unwrap() {
        return bot;
    }

    /**
     * @deprecated 委托到 Bot.Builder
     */
    @Deprecated
    public static final class Builder {
        private final Bot.Builder d = Bot.builder();

        public Builder appId(String appId) {
            d.appId(appId);
            return this;
        }

        public Builder appSecret(String appSecret) {
            d.appSecret(appSecret);
            return this;
        }

        public Builder options(QqBotOptions options) {
            d.options(options);
            return this;
        }

        public Builder transport(Transport transport) {
            d.transport(transport);
            return this;
        }

        public Builder eventExecutor(Executor eventExecutor) {
            d.eventExecutor(eventExecutor);
            return this;
        }

        public QqBot build() {
            return new QqBot(d.build());
        }
    }
}
