package com.xuanji.qqbot.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.xuanji.qqbot.json.Json;
import com.xuanji.qqbot.model.group.GroupBotState;
import com.xuanji.qqbot.model.group.GroupInfo;
import com.xuanji.qqbot.model.group.JoinRequestPage;
import com.xuanji.qqbot.model.group.RestrictChatSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 用真实 OpenAPI 报文校验群管理/机器人详情模型。 */
class GroupAdminRealPayloadTest {

    @Test
    void groupInfo() {
        GroupInfo g = Json.read("""
                {
                  "group_openid": "6D6C0B27E33AFA31F014E5959BF27F8C",
                  "group_name": "璇玑bot测试群",
                  "group_finger_memo": "璇玑bot测试群",
                  "group_class_text": "游戏",
                  "group_tags": [],
                  "group_member_num": 5
                }
                """, GroupInfo.class);
        assertEquals("璇玑bot测试群", g.groupName());
        assertEquals("游戏", g.groupClassText());
        assertEquals(5, g.groupMemberNum());
        assertTrue(g.groupTags() == null || g.groupTags().isEmpty());
    }

    @Test
    void groupBotState() {
        GroupBotState s = Json.read("""
                {
                  "member_openid": "FCD53C9F723CD8C1F97ADF7186C5D05A",
                  "joined_at": "2026-09-10T13:48:50+08:00",
                  "allow_proactive_msg": true,
                  "recv_msg_setting": "all",
                  "member_role": "admin"
                }
                """, GroupBotState.class);
        assertEquals("admin", s.memberRole());
        assertTrue(s.allowProactiveMsg());
        assertEquals("all", s.recvMsgSetting());
        assertEquals("2026-09-10T13:48:50+08:00", s.joinedAt());
    }

    @Test
    void joinRequestList() {
        JoinRequestPage page = Json.read("""
                {
                  "list": [
                    {
                      "join_request_id": "REQ1",
                      "risk_tips": "",
                      "union_openid": "",
                      "member_openid": "367F363415A19238E4CA84B526923F86",
                      "username": "借晚风叙旧",
                      "apply_at": "2026-09-10T14:54:02+08:00",
                      "apply_source": "self_apply",
                      "invited_by": "",
                      "bot": false,
                      "verify_info": {
                        "method": "admin_review_qa",
                        "verify_message": "",
                        "review_qa_list": [{"question": "一加一等于几？", "answer": "瞅瞅"}]
                      }
                    },
                    {
                      "join_request_id": "REQ2",
                      "risk_tips": "",
                      "union_openid": "",
                      "member_openid": "6468F8B87AD0D41DA2031EF7F714AD4D",
                      "username": "！！！",
                      "apply_at": "2026-09-10T14:24:59+08:00",
                      "apply_source": "invited",
                      "invited_by": "367F363415A19238E4CA84B526923F86",
                      "bot": false,
                      "verify_info": null
                    }
                  ],
                  "next_cursor": ""
                }
                """, JoinRequestPage.class);
        assertEquals(2, page.list().size());
        assertTrue(page.list().get(0).isSelfApply());
        assertEquals("admin_review_qa", page.list().get(0).verifyInfo().method());
        assertNull(page.list().get(1).verifyInfo());
        assertTrue(page.list().get(1).isInvited());
        assertEquals("367F363415A19238E4CA84B526923F86", page.list().get(1).invitedBy());
        assertEquals("", page.nextCursor());
    }

    @Test
    void restrictChat() {
        RestrictChatSetting s = Json.read("""
                {
                  "global_rule": {"mode": "none", "schedule_rules": [], "recurring_rules": []},
                  "members": [{
                    "member_openid": "367F363415A19238E4CA84B526923F86",
                    "mute_expire_at": "2026-09-10T15:03:01+08:00",
                    "username": "借晚风叙旧",
                    "union_openid": ""
                  }]
                }
                """, RestrictChatSetting.class);
        assertEquals("none", s.globalRule().mode());
        assertEquals(1, s.members().size());
        assertEquals("借晚风叙旧", s.members().get(0).username());
        assertEquals("2026-09-10T15:03:01+08:00", s.members().get(0).muteExpireAt());
    }

    @Test
    void botProfile() {
        GatewayApi.BotProfile p = Json.read("""
                {
                  "id": "13696488937455386294",
                  "username": "落落",
                  "avatar": "http://thirdqq.qlogo.cn/g?b=oidb&k=Riax7EV5RBHKiaKkDbVWI1ag&kti=aqJVJgwBHsA&s=0&t=1785540964",
                  "share_url": "https://qun.qq.com/qunpro/robot/qunshare?robot_uin=4019230206&robot_appid=1905134745&biz_type=0",
                  "welcome_msg": ""
                }
                """, GatewayApi.BotProfile.class);
        assertEquals("落落", p.username());
        assertNotNull(p.shareUrl());
        assertTrue(p.shareUrl().contains("qunshare"));
        assertEquals("", p.welcomeMsg());
        assertFalse(Boolean.TRUE.equals(null));
    }
}
