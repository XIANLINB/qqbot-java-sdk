package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * SUBSCRIBE_MESSAGE_STATUS：订阅消息授权状态变更。
 * 字段以官方推送为准，宽松映射。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubscribeMessageStatus(
        @JsonProperty("id") String id,
        @JsonProperty("status") String status,
        @JsonProperty("group_id") String groupId,
        @JsonProperty("group_openid") String groupOpenid,
        @JsonProperty("author") com.xuanji.qqbot.model.message.User author,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("data") Map<String, Object> data,
        @JsonIgnore String eventEnvelopeId
) implements Event {
    @Override
    public String eventId() {
        return eventEnvelopeId != null ? eventEnvelopeId : id;
    }

    @Override
    public String type() {
        return EventType.SUBSCRIBE_MESSAGE_STATUS;
    }

    @Override
    public String groupOpenid() {
        if (groupOpenid != null && !groupOpenid.isBlank()) {
            return groupOpenid;
        }
        return groupId;
    }
}
