package com.xuanji.qqbot.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.xuanji.qqbot.json.Json;
import com.xuanji.qqbot.model.message.MessageAttachment;
import com.xuanji.qqbot.model.message.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用真实网关 GROUP_MESSAGE_CREATE 报文做回归。
 */
class GroupMessageCreateRealPayloadTest {

    private static GroupMessageCreate parse(String dJson, String envelopeId) {
        JsonNode d = Json.read(dJson, JsonNode.class);
        Event e = Events.parse(EventType.GROUP_MESSAGE_CREATE, d, envelopeId);
        assertNotNull(e, "parse failed");
        return (GroupMessageCreate) e;
    }

    @Test
    void plainText() {
        GroupMessageCreate m = parse("""
                {
                  "author": {"bot":false,"id":"F5C011165910EB4FF2D63A9A582445F7","member_openid":"F5C011165910EB4FF2D63A9A582445F7","member_role":"owner","union_openid":"","username":"Yolo.H"},
                  "content": "你好",
                  "group_id": "6D6C0B27E33AFA31F014E5959BF27F8C",
                  "group_openid": "6D6C0B27E33AFA31F014E5959BF27F8C",
                  "id": "ROBOT1.0_abc",
                  "message_scene": {"ext":["msg_idx=REFIDX_x","auth_token=tok"],"source":"default"},
                  "message_type": 0,
                  "timestamp": "2026-09-10T12:08:50+08:00"
                }
                """, "ENV1");
        assertEquals("你好", m.content());
        assertFalse(m.looksLikeAtBot());
        assertEquals("6D6C0B27E33AFA31F014E5959BF27F8C", m.groupId());
        assertEquals("Yolo.H", m.author().username());
        assertEquals(0, m.messageType());
        assertEquals("ENV1", m.eventId());
    }

    @Test
    void atBotMessage() {
        GroupMessageCreate m = parse("""
                {
                  "author": {"bot":false,"id":"U","member_openid":"U","member_role":"owner","username":"Yolo.H"},
                  "content": "<@FCD53C9F723CD8C1F97ADF7186C5D05A> 你好",
                  "group_id": "G",
                  "group_openid": "G",
                  "id": "M2",
                  "mentions": [{
                    "bot": true,
                    "id": "FCD53C9F723CD8C1F97ADF7186C5D05A",
                    "is_you": true,
                    "member_openid": "FCD53C9F723CD8C1F97ADF7186C5D05A",
                    "member_role": "admin",
                    "scope": "single",
                    "username": "落落"
                  }],
                  "message_scene": {"source":"default","ext":[]},
                  "message_type": 0,
                  "timestamp": "2026-09-10T12:09:21+08:00"
                }
                """, "ENV_AT");
        assertTrue(m.looksLikeAtBot());
        assertEquals("你好", m.contentWithoutAtPrefix().trim());
        assertNotNull(m.mentions());
        User mention = m.mentions().get(0);
        assertTrue(mention.isBot());
        assertTrue(mention.isMentionedYou());
        assertEquals("single", mention.scope());
        assertEquals("admin", mention.memberRole());
    }

    @Test
    void imageAttachment() {
        GroupMessageCreate m = parse("""
                {
                  "attachments": [{
                    "content": "",
                    "content_type": "image/jpeg",
                    "filename": "x.png",
                    "height": 1080,
                    "size": 119364,
                    "url": "https://example.com/a.png",
                    "width": 1080
                  }],
                  "author": {"bot":false,"id":"U","member_openid":"U","username":"Yolo.H"},
                  "content": "",
                  "group_id": "G",
                  "group_openid": "G",
                  "id": "M3",
                  "message_type": 0,
                  "timestamp": "2026-09-10T12:10:41+08:00"
                }
                """, "ENV_IMG");
        MessageAttachment a = m.attachments().get(0);
        assertTrue(a.isImage());
        assertEquals("", a.content());
        assertEquals(1080, a.width());
        assertEquals(1080, a.height());
        assertEquals(119364L, a.size());
    }

    @Test
    void voiceAttachment() {
        GroupMessageCreate m = parse("""
                {
                  "attachments": [{
                    "asr_refer_text": "你好你好，喂喂喂。",
                    "content_type": "voice",
                    "filename": "v.amr",
                    "size": 7143,
                    "url": "https://example.com/v.amr",
                    "voice_wav_url": "https://example.com/v.wav"
                  }],
                  "author": {"bot":false,"id":"U","member_openid":"U"},
                  "content": "",
                  "group_id": "G",
                  "group_openid": "G",
                  "id": "M4",
                  "message_type": 0,
                  "timestamp": "2026-09-10T12:11:18+08:00"
                }
                """, "ENV_VOICE");
        MessageAttachment a = m.attachments().get(0);
        assertTrue(a.isVoice());
        assertEquals("你好你好，喂喂喂。", a.asrReferText());
        assertNotNull(a.voiceWavUrl());
    }

    @Test
    void arkCard() {
        GroupMessageCreate m = parse("""
                {
                  "ark_data": {
                    "ark_name": "图文H5",
                    "ark_type": "tuwen",
                    "fields": {
                      "desc": "金钟国",
                      "jump_url": "https://i.y.qq.com/x",
                      "tag": "QQ音乐",
                      "title": "恨幸福来过"
                    },
                    "prompt": "[分享]恨幸福来过"
                  },
                  "author": {"bot":false,"id":"U","member_openid":"U"},
                  "content": "[卡片消息] 图文H5\\n摘要: [分享]恨幸福来过",
                  "group_id": "G",
                  "group_openid": "G",
                  "id": "M5",
                  "message_scene": {"source":"default","ext":[]},
                  "message_type": 3,
                  "timestamp": "2026-09-10T12:15:18+08:00"
                }
                """, "ENV_ARK");
        assertEquals(3, m.messageType());
        assertNotNull(m.arkData());
        assertEquals("tuwen", m.arkData().arkType());
        assertEquals("恨幸福来过", m.arkData().title());
        assertEquals("QQ音乐", String.valueOf(m.arkData().fields().get("tag")));
    }

    @Test
    void atBotAndAtNormalUser() {
        GroupMessageCreate m = parse("""
                {
                  "author": {"bot":false,"id":"U","member_openid":"U","member_role":"owner","username":"Yolo.H"},
                  "content": "<@FCD53C9F723CD8C1F97ADF7186C5D05A> 打劫<@367F363415A19238E4CA84B526923F86> ",
                  "group_id": "G",
                  "group_openid": "G",
                  "id": "M6",
                  "mentions": [
                    {"bot":true,"id":"FCD53C9F723CD8C1F97ADF7186C5D05A","is_you":true,"member_openid":"FCD53C9F723CD8C1F97ADF7186C5D05A","member_role":"admin","scope":"single","username":"落落"},
                    {"bot":false,"id":"367F363415A19238E4CA84B526923F86","is_you":false,"member_openid":"367F363415A19238E4CA84B526923F86","member_role":"member","scope":"single","username":"借晚风叙旧"}
                  ],
                  "message_type": 0,
                  "timestamp": "2026-09-10T12:17:10+08:00"
                }
                """, "ENV_MULTI_USER");
        assertTrue(m.looksLikeAtBot());
        assertEquals(1, m.mentionedUsers().size());
        assertEquals("借晚风叙旧", m.mentionedUsers().get(0).username());
        assertEquals(0, m.mentionedOtherBots().size());
        assertEquals("打劫", m.contentWithoutMentions());
    }

    @Test
    void atBotAndAtOtherBot() {
        GroupMessageCreate m = parse("""
                {
                  "author": {"bot":false,"id":"U","member_openid":"U"},
                  "content": "<@FCD53C9F723CD8C1F97ADF7186C5D05A> 打劫<@15694D1FA10B275D0DDE3A4B7082C020> ",
                  "group_id": "G",
                  "group_openid": "G",
                  "id": "M7",
                  "mentions": [
                    {"bot":true,"id":"FCD53C9F723CD8C1F97ADF7186C5D05A","is_you":true,"member_openid":"FCD53C9F723CD8C1F97ADF7186C5D05A","member_role":"admin","scope":"single","username":"落落"},
                    {"bot":true,"id":"15694D1FA10B275D0DDE3A4B7082C020","is_you":false,"member_openid":"15694D1FA10B275D0DDE3A4B7082C020","member_role":"member","scope":"single","username":"魔幻大陆-测试中"}
                  ],
                  "message_type": 0,
                  "timestamp": "2026-09-10T12:18:06+08:00"
                }
                """, "ENV_MULTI_BOT");
        assertTrue(m.looksLikeAtBot());
        assertEquals(0, m.mentionedUsers().size());
        assertEquals(1, m.mentionedOtherBots().size());
        assertEquals("魔幻大陆-测试中", m.mentionedOtherBots().get(0).username());
        assertEquals("打劫", m.contentWithoutMentions());
    }

    @Test
    void atOtherBotOnly_isNotSelf() {
        GroupMessageCreate m = parse("""
                {
                  "author": {"bot":false,"id":"U","member_openid":"U"},
                  "content": "<@15694D1FA10B275D0DDE3A4B7082C020> 你好",
                  "group_id": "G",
                  "group_openid": "G",
                  "id": "M8",
                  "mentions": [
                    {"bot":true,"id":"15694D1FA10B275D0DDE3A4B7082C020","is_you":false,"member_openid":"15694D1FA10B275D0DDE3A4B7082C020","username":"魔幻大陆"}
                  ],
                  "message_type": 0,
                  "timestamp": "2026-09-10T12:19:00+08:00"
                }
                """, "ENV_OTHER_BOT_ONLY");
        assertFalse(m.looksLikeAtBot());
        assertEquals(1, m.mentionedOtherBots().size());
    }

    @Test
    void attachmentFormats() {
        assertTrue(att("image/webp", "a.webp").isImage());
        assertTrue(att("image/jpeg", "a.jpg").isImage());
        assertTrue(att("image/png", "a.png").isImage());
        assertTrue(att("video/mp4", "a.mp4").isVideo());
        assertTrue(att("file", "2026.mp4").isVideo()); // content_type=file 但扩展名 mp4
        assertTrue(att("file", "clip.mov").isVideo());
        assertTrue(att("file", "clip.mkv").isVideo());
        assertTrue(att("voice", "v.amr").isVoice());
        assertTrue(att("audio/mpeg", "song.mp3").isAudio());
        assertTrue(att("file", "song.mflac").isAudio());
        assertTrue(att("file", "song.flac").isAudio());
        assertTrue(att("file", "readme.txt").isFile());
        assertFalse(att("voice", "v.amr").isAudio());
        assertFalse(att("file", "song.mp3").isVoice());
    }

    private static MessageAttachment att(String contentType, String filename) {
        return new MessageAttachment("", "https://x", filename, 1, 1, 1L,
                contentType, null, null);
    }

    @Test
    void atChannelPlainHello() {
        GroupAtMessageCreate hello = atParse("""
                {
                  "author": {"bot":false,"id":"U","member_openid":"U","member_role":"owner","username":"Yolo.H"},
                  "content": " 你好",
                  "group_id": "6D6C0B27E33AFA31F014E5959BF27F8C",
                  "group_openid": "6D6C0B27E33AFA31F014E5959BF27F8C",
                  "id": "A1",
                  "message_type": 0,
                  "timestamp": "2026-09-10T12:26:45+08:00"
                }
                """);
        assertEquals("你好", hello.contentAsText());
        assertTrue(hello.faces().isEmpty());
    }

    @Test
    void atChannelFaceOnly() {
        GroupAtMessageCreate face = atParse("""
                {
                  "author": {"bot":false,"id":"U","member_openid":"U"},
                  "content": " <faceType=1,faceId=\\"277\\",ext=\\"eyJ0ZXh0Ijoi5rGq5rGqIn0=\\">",
                  "group_id": "G",
                  "group_openid": "G",
                  "id": "A2",
                  "message_type": 0,
                  "timestamp": "2026-09-10T12:28:30+08:00"
                }
                """);
        assertEquals(1, face.faces().size());
        assertEquals("277", face.faces().get(0).faceId());
        assertEquals("", face.contentAsText());
    }

    @Test
    void fullModeFace() {
        GroupMessageCreate m = parse("""
                {
                  "author": {"bot":false,"id":"U","member_openid":"U"},
                  "content": "<faceType=1,faceId=\\"277\\",ext=\\"eyJ0ZXh0Ijoi5rGq5rGqIn0=\\">",
                  "group_id": "G",
                  "group_openid": "G",
                  "id": "F1",
                  "message_type": 0,
                  "timestamp": "2026-09-10T12:29:11+08:00"
                }
                """, "ENV_FACE");
        assertEquals(1, m.faces().size());
        assertEquals("", m.contentWithoutMentions());
    }

    private static GroupAtMessageCreate atParse(String dJson) {
        Event e = Events.parse(EventType.GROUP_AT_MESSAGE_CREATE,
                Json.read(dJson, JsonNode.class), "AT");
        assertNotNull(e);
        return (GroupAtMessageCreate) e;
    }
}
