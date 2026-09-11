package com.xuanji.qqbot.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDeduplicatorTest {

    @Test
    void dropsDuplicateByMessageId() {
        EventDeduplicator d = new EventDeduplicator();
        Event e1 = sample("MSG1", "ENV1");
        assertFalse(d.isDuplicate(e1));
        // 同 msgId 不同信封 id（典型重复推送）
        assertTrue(d.isDuplicate(sample("MSG1", "ENV2")));
        assertTrue(d.isDuplicate(sample("MSG1", "ENV3")));
    }

    @Test
    void differentMessagesOk() {
        EventDeduplicator d = new EventDeduplicator();
        assertFalse(d.isDuplicate(sample("A", "1")));
        assertFalse(d.isDuplicate(sample("B", "2")));
    }

    @Test
    void usesEnvelopeIdWhenNoMessageId() {
        EventDeduplicator d = new EventDeduplicator();
        Event join = new GroupAddRobot("G1", null, "OP", 0L, "ENV-JOIN");
        assertFalse(d.isDuplicate(join));
        assertTrue(d.isDuplicate(new GroupAddRobot("G1", null, "OP", 0L, "ENV-JOIN")));
    }

    private static Event sample(String msgId, String envelopeId) {
        return new GroupMessageCreate(
                msgId, null, "你好", "g", "g", "t", 0,
                null, null, null, null, null, envelopeId
        );
    }
}
