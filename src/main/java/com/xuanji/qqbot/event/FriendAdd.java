package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FRIEND_ADD：用户添加机器人。
 * 实测 d：openid、timestamp（数字）、author.union_openid；d 内无 id，用信封 id。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FriendAdd(
        @JsonProperty("openid") String openid,
        @JsonProperty("timestamp") Object timestamp,
        @JsonProperty("author") com.xuanji.qqbot.model.message.User author,
        @JsonIgnore String eventEnvelopeId
) implements Event {
    @Override
    public String eventId() {
        return eventEnvelopeId;
    }

    @Override
    public String type() {
        return EventType.FRIEND_ADD;
    }

    @Override
    public String userOpenid() {
        return openid;
    }

    /**
     * @return 时间戳字符串（官方数字秒）
     */
    public String timestampAsString() {
        return timestamp == null ? null : String.valueOf(timestamp);
    }
}
