package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 群基本信息。GET /v2/groups/{group_openid}/info
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupInfo(
        @JsonProperty("group_openid") String groupOpenid,
        @JsonProperty("group_name") String groupName,
        @JsonProperty("group_finger_memo") String groupFingerMemo,
        @JsonProperty("group_class_text") String groupClassText,
        @JsonProperty("group_tags") List<String> groupTags,
        @JsonProperty("group_member_num") Integer groupMemberNum
) {
}
