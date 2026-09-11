package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 消息事件中的用户（含 mentions 项）。字段对齐实测群消息报文。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record User(
        @JsonProperty("id") String id,
        @JsonProperty("username") String username,
        @JsonProperty("bot") Boolean bot,
        @JsonProperty("is_you") Boolean isYou,
        @JsonProperty("scope") String scope,
        @JsonProperty("union_openid") String unionOpenid,
        @JsonProperty("union_user_account") String unionUserAccount,
        @JsonProperty("user_openid") String userOpenid,
        @JsonProperty("member_openid") String memberOpenid,
        @JsonProperty("member_role") String memberRole
) {
    public boolean isBot() {
        return Boolean.TRUE.equals(bot);
    }

    /** mentions 中是否标记为 @ 到本机器人 */
    public boolean isMentionedYou() {
        return Boolean.TRUE.equals(isYou);
    }
}
