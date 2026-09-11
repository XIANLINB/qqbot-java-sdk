package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.model.message.ArkData;
import com.xuanji.qqbot.model.message.MessageAttachment;
import com.xuanji.qqbot.model.message.MessageScene;
import com.xuanji.qqbot.model.message.User;

import java.util.List;

/**
 * GROUP_AT_MESSAGE_CREATE：群内 @ 机器人消息（未开全量时）。
 * <p>
 * 实测：content 已去掉 {@code <@bot>} 前缀，常有前后空格；
 * 通常无 mentions 字段；表情在 content 中为 {@code <faceType=...>}。
 * 若同时 @ 其它用户，其 @ 标记也可能已被剥离，content 仅为剩余正文。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupAtMessageCreate(
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
    @Override
    public String eventId() {
        return eventEnvelopeId != null ? eventEnvelopeId : id;
    }

    @Override
    public String type() {
        return EventType.GROUP_AT_MESSAGE_CREATE;
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
     * @return 正文 trim 后文本（不含表情标记）
     */
    public String contentAsText() {
        return com.xuanji.qqbot.model.message.FaceTags.strip(content);
    }

    /**
     * @return 正文中的表情列表
     */
    public List<com.xuanji.qqbot.model.message.FaceTags.Face> faces() {
        return com.xuanji.qqbot.model.message.FaceTags.parseAll(content);
    }
}
