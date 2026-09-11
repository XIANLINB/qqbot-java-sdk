package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 机器人群内状态。GET /v2/groups/{group_openid}/bot_state
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupBotState(
        @JsonProperty("member_openid") String memberOpenid,
        @JsonProperty("joined_at") String joinedAt,
        @JsonProperty("allow_proactive_msg") Boolean allowProactiveMsg,
        /** all / only_mention / mention_and_context */
        @JsonProperty("recv_msg_setting") String recvMsgSetting,
        @JsonProperty("member_role") String memberRole
) {
}
