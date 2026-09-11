package com.xuanji.qqbot.api;

import com.xuanji.qqbot.http.ApiCall;
import com.xuanji.qqbot.http.Transport;
import com.xuanji.qqbot.model.keyboard.Keyboard;
import com.xuanji.qqbot.model.message.MessageIdResult;
import com.xuanji.qqbot.model.message.PostMessage;
import com.xuanji.qqbot.model.message.Reply;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageApiTest {

    static final class FakeTransport implements Transport {
        final List<Transport.RawRequest> requests = new ArrayList<>();
        final AtomicInteger n = new AtomicInteger();

        @Override
        public Transport.RawResponse exchange(Transport.RawRequest request) {
            requests.add(request);
            return new Transport.RawResponse(200, Map.of(),
                    ("{\"id\":\"msg-" + n.incrementAndGet() + "\",\"timestamp\":\"2026-01-01T00:00:00+08:00\"}")
                            .getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void postGroupTextWithReply() {
        FakeTransport t = new FakeTransport();
        ApiCall call = new ApiCall(t, URI.create("https://api.bot.qq.com"), () -> "TOK");
        MessageApi api = new MessageApi(call);

        MessageIdResult r = api.postGroupText("G123", "你好", Reply.to("M1").seq(2));
        assertEquals("msg-1", r.id());

        Transport.RawRequest last = t.requests.get(0);
        assertEquals("POST", last.method());
        assertTrue(last.uri().getPath().endsWith("/v2/groups/G123/messages"));
        String body = new String(last.body(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"msg_type\":0"));
        assertTrue(body.contains("\"content\":\"你好\""));
        assertTrue(body.contains("\"msg_id\":\"M1\""));
        assertTrue(body.contains("\"msg_seq\":2"));
    }

    @Test
    void postC2cMarkdownWithKeyboard() {
        FakeTransport t = new FakeTransport();
        ApiCall call = new ApiCall(t, URI.create("https://api.bot.qq.com"), () -> "TOK");
        MessageApi api = new MessageApi(call);

        PostMessage msg = PostMessage.markdown("# title")
                .withKeyboard(Keyboard.builder()
                        .row(r -> r.button(b -> b.id("b1").label("点我").command("/hi", true)))
                        .build());
        api.postC2c("U1", msg);

        String body = new String(t.requests.get(0).body(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"msg_type\":2"));
        assertTrue(body.contains("\"markdown\""));
        assertTrue(body.contains("\"keyboard\""));
        assertTrue(body.contains("点我"));
    }
}
