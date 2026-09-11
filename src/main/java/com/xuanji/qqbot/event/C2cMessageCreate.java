package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.model.message.ArkData;
import com.xuanji.qqbot.model.message.FaceTags;
import com.xuanji.qqbot.model.message.MessageAttachment;
import com.xuanji.qqbot.model.message.MessageScene;
import com.xuanji.qqbot.model.message.User;

import java.util.List;

/**
 * C2C_MESSAGE_CREATE：用户单聊消息。
 * <p>
 * 实测：author 含 user_openid；username 可为空串；
 * content 为正文；message_scene.ext 含 msg_idx。
 * content 通常不含 &lt;@&gt; 标记。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record C2cMessageCreate(
        /** 消息 ID（d.id），用于被动回复 msg_id */
        @JsonProperty("id") String id,
        /** 发送者：user_openid / id / username 等 */
        @JsonProperty("author") User author,
        /** 消息文本 */
        @JsonProperty("content") String content,
        /** 发送时间 RFC3339 */
        @JsonProperty("timestamp") String timestamp,
        /** 0=纯文本 3=卡片 等 */
        @JsonProperty("message_type") Integer messageType,
        /** 场景上下文 ext/source */
        @JsonProperty("message_scene") MessageScene messageScene,
        /** 附件（可缺省） */
        @JsonProperty("attachments") List<MessageAttachment> attachments,
        /** ARK（可缺省） */
        @JsonProperty("ark_data") ArkData arkData,
        /** 消息元素（可缺省） */
        @JsonProperty("msg_elements") List<MsgElement> msgElements,
        /** 信封 id */
        @JsonIgnore String eventEnvelopeId
) implements Event {
    @Override
    public String eventId() {
        return eventEnvelopeId != null ? eventEnvelopeId : id;
    }

    @Override
    public String type() {
        return EventType.C2C_MESSAGE_CREATE;
    }

    @Override
    public String messageId() {
        return id;
    }

    /**
     * @return 单聊用户 OpenID
     */
    @Override
    public String userOpenid() {
        return author == null ? null : author.userOpenid();
    }

    /**
     * @return 去掉表情标记后的正文
     */
    public String contentAsText() {
        return FaceTags.strip(content);
    }

    /**
     * @return 正文中的表情列表
     */
    public List<FaceTags.Face> faces() {
        return FaceTags.parseAll(content);
    }
}
