package com.xuanji.qqbot.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.xuanji.qqbot.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GroupMessageCreateParseTest {

    /** 用户提供的真实网关报文（GROUP_MESSAGE_CREATE）。 */
    private static final String SAMPLE = """
            {
              "author": {
                "bot": false,
                "id": "F5C011165910EB4FF2D63A9A582445F7",
                "member_openid": "F5C011165910EB4FF2D63A9A582445F7",
                "member_role": "owner",
                "union_openid": "",
                "username": "Yolo.H"
              },
              "content": "你好",
              "group_id": "6D6C0B27E33AFA31F014E5959BF27F8C",
              "group_openid": "6D6C0B27E33AFA31F014E5959BF27F8C",
              "id": "ROBOT1.0_8ME9Uf1xPG9APoyt7Pltd6FwELS7kgdtTQkUuU4LtdK4SI6YCQTLlVCb85HbeFrHSG6nirbih69xSSRWp-eWiufc9CcD.q-wjum3kSu84kM!",
              "message_scene": {
                "ext": [
                  "msg_idx=REFIDX_F4nVwfSyFJFR+o9pEAWKaQwVBoBqe31OanR9tPUepCNgL7WJDaREGlolvpVisyfP/HTpWLZTud9WwCvwKeQ/v4XRX/dPDQem4jf+NVrOVdzg9fm5K0Hr/wHDbBrE7Tbx",
                  "auth_token=T6ZKOyRFydgPy4dzsFIb1MtELtOmiAygbHFTTPzNBtiloGlsOBG74spKCB626-T_J3bS2UIUuh1eOGK_--uhEdKYVvisGh4GhN7PpF1aEWwas5ooiC1_5WcEhwyi"
                ],
                "source": "default"
              },
              "message_type": 0,
              "timestamp": "2026-09-10T12:04:14+08:00"
            }
            """;

    @Test
    void parseRealGroupMessage() {
        JsonNode d = Json.read(SAMPLE, JsonNode.class);
        Event e = Events.parse(EventType.GROUP_MESSAGE_CREATE, d, "env-1");
        assertNotNull(e);
        assertInstanceOfGroup(e);
        GroupMessageCreate m = (GroupMessageCreate) e;
        assertEquals("你好", m.content());
        assertEquals("6D6C0B27E33AFA31F014E5959BF27F8C", m.groupId());
        assertEquals("6D6C0B27E33AFA31F014E5959BF27F8C", m.groupOpenid());
        assertEquals("6D6C0B27E33AFA31F014E5959BF27F8C", m.groupOpenidOrId());
        assertNotNull(m.author());
        assertEquals("Yolo.H", m.author().username());
        assertEquals("owner", m.author().memberRole());
        assertFalse(m.author().isBot());
        assertEquals(0, m.messageType());
        assertEquals("2026-09-10T12:04:14+08:00", m.timestamp());
        assertNotNull(m.messageScene());
        assertEquals("default", m.messageScene().source());
        assertEquals("env-1", m.eventId());
        assertFalse(m.looksLikeAtBot());
    }

    private static void assertInstanceOfGroup(Event e) {
        if (!(e instanceof GroupMessageCreate)) {
            throw new AssertionError("expected GroupMessageCreate but was " + e.getClass());
        }
    }
}
