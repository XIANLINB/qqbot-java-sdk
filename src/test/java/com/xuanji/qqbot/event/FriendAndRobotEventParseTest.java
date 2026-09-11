package com.xuanji.qqbot.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.xuanji.qqbot.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FriendAndRobotEventParseTest {

    @Test
    void friendAdd() {
        Event e = Events.parse(EventType.FRIEND_ADD, Json.read("""
                {
                  "author": {"union_openid": ""},
                  "openid": "F5C011165910EB4FF2D63A9A582445F7",
                  "timestamp": 1789019102
                }
                """, JsonNode.class), "FRIEND_ADD:env1");
        FriendAdd f = (FriendAdd) e;
        assertEquals("F5C011165910EB4FF2D63A9A582445F7", f.userOpenid());
        assertEquals("FRIEND_ADD:env1", f.eventId());
        assertEquals("1789019102", f.timestampAsString());
    }

    @Test
    void friendDel() {
        Event e = Events.parse(EventType.FRIEND_DEL, Json.read("""
                {"author":{"union_openid":""},"openid":"U1","timestamp":1789019155}
                """, JsonNode.class), "FRIEND_DEL:env2");
        FriendDel f = (FriendDel) e;
        assertEquals("U1", f.userOpenid());
        assertEquals("FRIEND_DEL:env2", f.eventId());
    }

    @Test
    void groupAddDelRobot() {
        GroupAddRobot add = (GroupAddRobot) Events.parse(EventType.GROUP_ADD_ROBOT, Json.read("""
                {
                  "group_openid": "G1",
                  "op_member_openid": "OP1",
                  "timestamp": 1789019330
                }
                """, JsonNode.class), "GROUP_ADD_ROBOT:e3");
        assertEquals("G1", add.groupOpenid());
        assertEquals("OP1", add.operatorOpenid());
        assertEquals("GROUP_ADD_ROBOT:e3", add.eventId());
        assertNotNull(add.timestampAsString());

        GroupDelRobot del = (GroupDelRobot) Events.parse(EventType.GROUP_DEL_ROBOT, Json.read("""
                {"group_openid":"G1","op_member_openid":"OP1","timestamp":1789019296}
                """, JsonNode.class), "GROUP_DEL_ROBOT:e4");
        assertEquals("G1", del.groupOpenid());
        assertEquals("OP1", del.operatorOpenid());
    }
}
