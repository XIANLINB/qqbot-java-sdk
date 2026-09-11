package com.xuanji.qqbot.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventsTest {

    @Test
    void dispatchesTypedGroupEvent() {
        Events events = new Events(null);
        AtomicReference<GroupAtMessageCreate> got = new AtomicReference<>();
        events.on(GroupAtMessageCreate.class, got::set);

        String json = """
                {
                  "id": "ROBOT1.0_abc",
                  "author": {"member_openid":"M1","username":"u","bot":false},
                  "content": "hello",
                  "group_openid": "G1",
                  "message_type": 0
                }
                """;
        events.dispatchEnvelope(EventType.GROUP_AT_MESSAGE_CREATE,
                com.xuanji.qqbot.json.Json.read(json, com.fasterxml.jackson.databind.JsonNode.class),
                "evt-1");

        GroupAtMessageCreate e = got.get();
        assertNotNull(e);
        assertEquals("G1", e.groupOpenid());
        assertEquals("hello", e.content());
        assertEquals("evt-1", e.eventId());
        assertEquals("M1", e.memberOpenid());
    }

    @Test
    void onAnyAndByType() {
        Events events = new Events(null);
        List<Event> any = new ArrayList<>();
        List<Event> byType = new ArrayList<>();
        events.onAny(any::add);
        events.on(EventType.C2C_MESSAGE_CREATE, byType::add);

        String json = """
                {"id":"1","content":"hi","author":{"user_openid":"U1"}}
                """;
        events.dispatchEnvelope(EventType.C2C_MESSAGE_CREATE,
                com.xuanji.qqbot.json.Json.read(json, com.fasterxml.jackson.databind.JsonNode.class),
                "e1");

        assertEquals(1, any.size());
        assertEquals(1, byType.size());
        assertInstanceOf(C2cMessageCreate.class, byType.get(0));
        assertEquals("U1", ((C2cMessageCreate) byType.get(0)).userOpenid());
    }
}
