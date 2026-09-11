package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GROUP_MEMBER_ADD：群成员加入。
 * 实测 d：group_openid、member_openid、timestamp（数字）。
 * Intent：GROUP_MEMBER_EVENT (1&lt;&lt;24)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupMemberAdd(
        @JsonProperty("group_openid") String groupOpenid,
        @JsonProperty("member_openid") String memberOpenid,
        @JsonProperty("timestamp") Object timestamp,
        @JsonIgnore String eventEnvelopeId
) implements Event {
    @Override
    public String eventId() {
        return eventEnvelopeId;
    }

    @Override
    public String type() {
        return EventType.GROUP_MEMBER_ADD;
    }

    @Override
    public String groupOpenid() {
        return groupOpenid;
    }

    /**
     * @return 加入的成员 OpenID
     */
    public String openid() {
        return memberOpenid;
    }

    /**
     * @return 时间戳字符串
     */
    public String timestampAsString() {
        return timestamp == null ? null : String.valueOf(timestamp);
    }
}
