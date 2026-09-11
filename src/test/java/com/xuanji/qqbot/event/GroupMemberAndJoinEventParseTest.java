package com.xuanji.qqbot.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.xuanji.qqbot.json.Json;
import com.xuanji.qqbot.model.group.JoinVerifyInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupMemberAndJoinEventParseTest {

    @Test
    void memberAddRemove() {
        GroupMemberAdd add = (GroupMemberAdd) Events.parse(EventType.GROUP_MEMBER_ADD, Json.read("""
                {
                  "group_openid": "G1",
                  "member_openid": "M1",
                  "timestamp": 1789020966
                }
                """, JsonNode.class), "GROUP_MEMBER_ADD:e1");
        assertEquals("G1", add.groupOpenid());
        assertEquals("M1", add.openid());
        assertEquals("1789020966", add.timestampAsString());
        assertEquals("GROUP_MEMBER_ADD:e1", add.eventId());

        GroupMemberRemove rm = (GroupMemberRemove) Events.parse(EventType.GROUP_MEMBER_REMOVE, Json.read("""
                {"group_openid":"G1","member_openid":"M1","timestamp":1789020998}
                """, JsonNode.class), "GROUP_MEMBER_REMOVE:e2");
        assertEquals("M1", rm.openid());
    }

    @Test
    void joinRequestVerifyMessage() {
        GroupJoinRequest req = (GroupJoinRequest) Events.parse(EventType.GROUP_JOIN_REQUEST, Json.read("""
                {
                  "apply_at": "2026-09-10T14:17:31+08:00",
                  "apply_source": "self_apply",
                  "group_openid": "G1",
                  "join_request_id": "REQ1",
                  "member_openid": "U1",
                  "username": "借晚风叙旧",
                  "verify_info": {
                    "method": "verify_message",
                    "verify_message": "哈哈"
                  }
                }
                """, JsonNode.class), "GROUP_JOIN_REQUEST:e3");
        assertEquals("REQ1", req.requestId());
        assertEquals("U1", req.applicantOpenid());
        assertTrue(req.isVerifyMessage());
        assertFalse(req.isReviewQa());
        assertEquals("哈哈", req.verifyMessage());
        assertEquals("2026-09-10T14:17:31+08:00", req.applyAt());
    }

    @Test
    void joinRequestReviewQa() {
        GroupJoinRequest req = (GroupJoinRequest) Events.parse(EventType.GROUP_JOIN_REQUEST, Json.read("""
                {
                  "apply_at": "2026-09-10T14:18:24+08:00",
                  "apply_source": "self_apply",
                  "group_openid": "G1",
                  "join_request_id": "REQ2",
                  "member_openid": "U1",
                  "username": "借晚风叙旧",
                  "verify_info": {
                    "method": "admin_review_qa",
                    "review_qa_list": [
                      {"answer": "2", "question": "一加一等于几？"}
                    ]
                  }
                }
                """, JsonNode.class), "GROUP_JOIN_REQUEST:e4");
        assertTrue(req.isReviewQa());
        assertFalse(req.isVerifyMessage());
        assertEquals(1, req.reviewQuestions().size());
        assertEquals("一加一等于几？", req.reviewQuestions().get(0).question());
        assertEquals("2", req.reviewQuestions().get(0).answer());
        assertNotNull(req.verify());
        JoinVerifyInfo v = req.verify();
        assertEquals("admin_review_qa", v.method());
    }

    @Test
    void invitedJoin() {
        GroupJoinRequest req = (GroupJoinRequest) Events.parse(EventType.GROUP_JOIN_REQUEST, Json.read("""
                {
                  "apply_at": "2026-09-10T14:24:59+08:00",
                  "apply_source": "invited",
                  "group_openid": "G1",
                  "invited_by": "INVITER1",
                  "join_request_id": "REQ-INV",
                  "member_openid": "U2",
                  "username": "！！！"
                }
                """, JsonNode.class), "GROUP_JOIN_REQUEST:inv");
        assertTrue(req.isInvited());
        assertFalse(req.isSelfApply());
        assertEquals("INVITER1", req.inviterOpenid());
        assertEquals("U2", req.applicantOpenid());
        assertFalse(req.isAutoApproved());
    }

    @Test
    void autoApprovedJoin() {
        GroupJoinRequest req = (GroupJoinRequest) Events.parse(EventType.GROUP_JOIN_REQUEST, Json.read("""
                {
                  "apply_at": "2026-09-10T14:20:00+08:00",
                  "apply_source": "self_apply",
                  "group_openid": "G1",
                  "join_request_id": "REQ-AUTO",
                  "member_openid": "U3",
                  "username": "自动通过用户",
                  "verify_info": {
                    "method": "verify_message",
                    "verify_message": "健健康康"
                  },
                  "auto_approved": {
                    "strategy_id": "st_7c0b77d442"
                  }
                }
                """, JsonNode.class), "GROUP_JOIN_REQUEST:auto");
        assertTrue(req.isSelfApply());
        assertTrue(req.isVerifyMessage());
        assertEquals("健健康康", req.verifyMessage());
        assertTrue(req.isAutoApproved());
        assertEquals("st_7c0b77d442", req.strategyId());
    }
}
