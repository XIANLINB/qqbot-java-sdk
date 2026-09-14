package com.xuanji.qqbot.http;

import com.xuanji.qqbot.QqBotOptions;
import com.xuanji.qqbot.auth.Credentials;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenSourceTest {

    static final class FakeTransport implements Transport {
        final AtomicInteger calls = new AtomicInteger();
        final String token1;
        final String token2;

        FakeTransport(String token1, String token2) {
            this.token1 = token1;
            this.token2 = token2;
        }

        @Override
        public Transport.RawResponse exchange(Transport.RawRequest request) {
            int n = calls.incrementAndGet();
            String token = n == 1 ? token1 : token2;
            String body = "{\"access_token\":\"" + token + "\",\"expires_in\":\"7200\"}";
            return new Transport.RawResponse(200, Map.of(), body.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void cachesTokenWithinLifetime() {
        FakeTransport t = new FakeTransport("aaa", "bbb");
        QqBotOptions opts = QqBotOptions.defaults(new Credentials("id", "secret"));
        TokenSource src = new TokenSource(t, opts);
        assertEquals("aaa", src.accessToken());
        assertEquals("aaa", src.accessToken());
        assertEquals(1, t.calls.get());
    }

    @Test
    void refreshesWhenExpired() {
        FakeTransport t = new FakeTransport("aaa", "bbb");
        QqBotOptions opts = new QqBotOptions(
                new Credentials("id", "secret"),
                URI.create("https://api.bot.qq.com"),
                java.time.Duration.ofSeconds(5),
                java.time.Duration.ofSeconds(5),
                java.time.Duration.ofSeconds(10000),
                null,
                new int[]{0, 1},
                null,
                null
        );
        TokenSource src = new TokenSource(t, opts);
        assertEquals("aaa", src.accessToken());
        assertNotEquals("aaa", src.accessToken());
        assertEquals("bbb", src.accessToken());
        assertTrue(t.calls.get() >= 2);
    }
}
