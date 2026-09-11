package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * C2C_MSG_RECEIVE：用户打开单聊主动消息推送。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record C2cMsgReceive(
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
        return EventType.C2C_MSG_RECEIVE;
    }

    @Override
    public String userOpenid() {
        return author == null ? null : author.userOpenid();
    }
}
