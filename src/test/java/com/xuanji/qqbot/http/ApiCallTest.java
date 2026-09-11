package com.xuanji.qqbot.http;

import com.xuanji.qqbot.exception.ApiException;
import com.xuanji.qqbot.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiCallTest {

    static final class FakeTransport implements Transport {
        final List<Transport.RawRequest> requests = new ArrayList<>();
        final List<Transport.RawResponse> responses = new ArrayList<>();
        final AtomicInteger cursor = new AtomicInteger();

        @Override
        public Transport.RawResponse exchange(Transport.RawRequest request) {
            requests.add(request);
            return responses.get(cursor.getAndIncrement());
        }
    }

    @Test
    void injectsAuthorizationHeader() {
        FakeTransport t = new FakeTransport();
        t.responses.add(new Transport.RawResponse(200, Map.of(),
                "{\"id\":\"m1\",\"timestamp\":\"t\"}".getBytes(StandardCharsets.UTF_8)));
        ApiCall call = new ApiCall(t, URI.create("https://api.bot.qq.com"), () -> "TOKEN123");
        call.get("/users/@me", String.class);
        assertEquals("QQBot TOKEN123", t.requests.get(0).headers().get("Authorization"));
        assertTrue(t.requests.get(0).uri().getPath().endsWith("/users/@me"));
    }

    @Test
    void mapsErrCodeToApiException() {
        FakeTransport t = new FakeTransport();
        t.responses.add(new Transport.RawResponse(200, Map.of(),
                "{\"err_code\":40034005,\"message\":\"expired\",\"trace_id\":\"abc\"}"
                        .getBytes(StandardCharsets.UTF_8)));
        ApiCall call = new ApiCall(t, URI.create("https://api.bot.qq.com"), () -> "T");
        ApiException ex = assertThrows(ApiException.class, () -> call.get("/x", String.class));
        assertEquals(40034005, ex.errCode());
        assertEquals("abc", ex.traceId());
    }

    @Test
    void mapsRateLimit() {
        FakeTransport t = new FakeTransport();
        t.responses.add(new Transport.RawResponse(429, Map.of(),
                "{\"err_code\":40034100,\"message\":\"rate\",\"trace_id\":null}"
                        .getBytes(StandardCharsets.UTF_8)));
        ApiCall call = new ApiCall(t, URI.create("https://api.bot.qq.com"), () -> "T");
        assertThrows(RateLimitException.class, () -> call.get("/x", String.class));
    }
}
