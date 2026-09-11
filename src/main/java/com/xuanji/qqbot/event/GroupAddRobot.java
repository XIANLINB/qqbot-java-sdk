package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GROUP_ADD_ROBOT：机器人被添加到群聊。
 * 实测 d：group_openid、op_member_openid、timestamp（数字）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupAddRobot(
        @JsonProperty("group_openid") String groupOpenid,
        @JsonProperty("group_id") String groupId,
        @JsonProperty("op_member_openid") String opMemberOpenid,
        @JsonProperty("timestamp") Object timestamp,
        @JsonIgnore String eventEnvelopeId
) implements Event {
    @Override
    public String eventId() {
        return eventEnvelopeId;
    }

    @Override
    public String type() {
        return EventType.GROUP_ADD_ROBOT;
    }

    @Override
    public String groupOpenid() {
        if (groupOpenid != null && !groupOpenid.isBlank()) {
            return groupOpenid;
        }
        return groupId;
    }

    /**
     * @return 操作者（拉机器人进群的人）
     */
    public String operatorOpenid() {
        return opMemberOpenid;
    }

    /**
     * @return 时间戳字符串
     */
    public String timestampAsString() {
        return timestamp == null ? null : String.valueOf(timestamp);
    }
}
