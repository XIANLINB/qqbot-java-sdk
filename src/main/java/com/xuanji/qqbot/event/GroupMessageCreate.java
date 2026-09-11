package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.model.message.ArkData;
import com.xuanji.qqbot.model.message.MessageAttachment;
import com.xuanji.qqbot.model.message.MessageScene;
import com.xuanji.qqbot.model.message.User;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * GROUP_MESSAGE_CREATE：群消息（全量模式）。
 * <p>
 * 开启「接收所有消息」后，所有群聊消息（含 @ 本机器人）均走本事件。
 * content 中 @ 表现为 {@code <@openid>}；mentions 可含多人/多机器人。
 * 仅 {@code is_you=true} 表示 @ 到了<b>本</b>机器人；@ 别人机器人时 is_you=false。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupMessageCreate(
        @JsonProperty("id") String id,
        @JsonProperty("author") User author,
        @JsonProperty("content") String content,
        @JsonProperty("group_id") String groupId,
        @JsonProperty("group_openid") String groupOpenid,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("message_type") Integer messageType,
        @JsonProperty("message_scene") MessageScene messageScene,
        @JsonProperty("attachments") List<MessageAttachment> attachments,
        @JsonProperty("mentions") List<User> mentions,
        @JsonProperty("ark_data") ArkData arkData,
        @JsonProperty("msg_elements") List<MsgElement> msgElements,
        @JsonIgnore String eventEnvelopeId
) implements Event {
    private static final Pattern AT_TOKEN = Pattern.compile("<@[^>]+>");

    @Override
    public String eventId() {
        return eventEnvelopeId != null ? eventEnvelopeId : id;
    }

    @Override
    public String type() {
        return EventType.GROUP_MESSAGE_CREATE;
    }

    @Override
    public String messageId() {
        return id;
    }

    @Override
    public String groupOpenid() {
        return groupOpenidOrId();
    }

    public String memberOpenid() {
        return author == null ? null : author.memberOpenid();
    }

    public String groupOpenidOrId() {
        if (groupOpenid != null && !groupOpenid.isBlank()) {
            return groupOpenid;
        }
        return groupId;
    }

    /**
     * 是否 @ 了本机器人。只认 mentions 中 {@code is_you=true}，
     * 不把「@ 别的机器人 / 别的用户」算进来。
     *
     * @return true 表示本机器人被 @
     */
    public boolean looksLikeAtBot() {
        if (mentions != null) {
            for (User u : mentions) {
                if (u != null && u.isMentionedYou()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return 被 @ 的普通用户（bot=false）
     */
    public List<User> mentionedUsers() {
        List<User> out = new ArrayList<>();
        if (mentions == null) {
            return out;
        }
        for (User u : mentions) {
            if (u != null && !u.isBot()) {
                out.add(u);
            }
        }
        return out;
    }

    /**
     * @return 被 @ 的其它机器人（bot=true 且 is_you=false）
     */
    public List<User> mentionedOtherBots() {
        List<User> out = new ArrayList<>();
        if (mentions == null) {
            return out;
        }
        for (User u : mentions) {
            if (u != null && u.isBot() && !u.isMentionedYou()) {
                out.add(u);
            }
        }
        return out;
    }

    /**
     * 去掉 content 中全部 {@code <@openid>} 与表情标记后的正文。
     * 例：{@code <@A> 打劫<@B> } → {@code 打劫}
     *
     * @return 纯文本
     */
    public String contentWithoutMentions() {
        if (content == null) {
            return null;
        }
        String s = AT_TOKEN.matcher(content).replaceAll("");
        return com.xuanji.qqbot.model.message.FaceTags.strip(s);
    }

    /**
     * @return 正文中的表情列表
     */
    public List<com.xuanji.qqbot.model.message.FaceTags.Face> faces() {
        return com.xuanji.qqbot.model.message.FaceTags.parseAll(content);
    }

    /**
     * 仅去掉开头的 {@code <@openid>}（兼容旧命名）。
     *
     * @return 纯文本
     */
    public String contentWithoutAtPrefix() {
        return contentWithoutMentions();
    }
}
