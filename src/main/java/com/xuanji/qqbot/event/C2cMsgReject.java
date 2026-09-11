package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * C2C_MSG_REJECT：用户关闭单聊主动消息推送。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record C2cMsgReject(
        @JsonProperty("id") String id,
        @JsonProperty("author") com.xuanji.qqbot.model.message.User author,
        @JsonProperty("timestamp") String timestamp,
        @JsonIgnore String eventEnvelopeId
) implements Event {
    @Override
    public String eventId() {
        return eventEnvelopeId != null ? eventEnvelopeId : id;
    }

    @Override
    public String type() {
        return EventType.C2C_MSG_REJECT;
    }

    @Override
    public String userOpenid() {
        return author == null ? null : author.userOpenid();
    }
}
