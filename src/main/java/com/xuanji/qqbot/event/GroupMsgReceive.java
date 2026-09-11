package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GROUP_MSG_RECEIVE：群管理员打开机器人推送。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupMsgReceive(
        @JsonProperty("id") String id,
        @JsonProperty("group_id") String groupId,
        @JsonProperty("group_openid") String groupOpenid,
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
        return EventType.GROUP_MSG_RECEIVE;
    }

    @Override
    public String groupOpenid() {
        if (groupOpenid != null && !groupOpenid.isBlank()) {
            return groupOpenid;
        }
        return groupId;
    }
}
