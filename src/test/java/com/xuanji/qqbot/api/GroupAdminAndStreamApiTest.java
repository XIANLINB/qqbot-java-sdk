package com.xuanji.qqbot.api;

import com.xuanji.qqbot.http.ApiCall;
import com.xuanji.qqbot.http.Transport;
import com.xuanji.qqbot.model.group.SetMemberMuteState;
import com.xuanji.qqbot.model.message.StreamMessage;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupAdminAndStreamApiTest {

    static final class FakeTransport implements Transport {
        final List<Transport.RawRequest> requests = new ArrayList<>();

        @Override
        public Transport.RawResponse exchange(Transport.RawRequest request) {
            requests.add(request);
            if (request.uri().getPath().contains("stream_messages")) {
                return new Transport.RawResponse(200, Map.of(),
                        "{\"id\":\"stream-1\",\"timestamp\":\"t\"}".getBytes(StandardCharsets.UTF_8));
            }
            return new Transport.RawResponse(200, Map.of(), "{}".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void listMembersUsesQuery() {
        FakeTransport t = new FakeTransport();
        GroupAdminApi api = new GroupAdminApi(new ApiCall(t, URI.create("https://api.bot.qq.com"), () -> "T"));
        api.listMembers("G1", "abc");
        assertTrue(t.requests.get(0).uri().getPath().endsWith("/v2/groups/G1/members"));
        assertTrue(t.requests.get(0).uri().getQuery().contains("cursor=abc"));
    }

    @Test
    void muteBody() {
        FakeTransport t = new FakeTransport();
        GroupAdminApi api = new GroupAdminApi(new ApiCall(t, URI.create("https://api.bot.qq.com"), () -> "T"));
        api.setMemberMute("G1", List.of(SetMemberMuteState.add("M1", "2026-01-01T00:00:00+08:00")));
        String body = new String(t.requests.get(0).body(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"op\":\"add\""));
        assertTrue(body.contains("M1"));
    }

    @Test
    void streamFirstChunk() {
        FakeTransport t = new FakeTransport();
        MessageApi api = new MessageApi(new ApiCall(t, URI.create("https://api.bot.qq.com"), () -> "T"));
        api.postC2cStream("U1", StreamMessage.first("hi", StreamMessage.CONTENT_MARKDOWN, "M1", 1));
        String body = new String(t.requests.get(0).body(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"input_state\":1"));
        assertTrue(body.contains("\"index\":0"));
        assertTrue(t.requests.get(0).uri().getPath().contains("stream_messages"));
    }

    @Test
    void approveJoin() {
        FakeTransport t = new FakeTransport();
        GroupAdminApi api = new GroupAdminApi(new ApiCall(t, URI.create("https://api.bot.qq.com"), () -> "T"));
        api.approveJoinRequest("G1", "M1", "req1");
        assertTrue(t.requests.get(0).uri().getPath().endsWith("/v2/groups/G1/approval_join_request/M1"));
        String body = new String(t.requests.get(0).body(), StandardCharsets.UTF_8);
        assertTrue(body.contains("approve"));
    }
}
