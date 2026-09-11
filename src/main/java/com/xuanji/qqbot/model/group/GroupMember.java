package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 群成员（官方 Member）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupMember(
        /** 成员 OpenID */
        @JsonProperty("member_openid") String memberOpenid,
        /** 昵称 */
        @JsonProperty("username") String username,
        /** member / admin / owner */
        @JsonProperty("member_role") String memberRole,
        /** 是否机器人 */
        @JsonProperty("bot") Boolean bot,
        /** 入群时间 RFC3339 */
        @JsonProperty("joined_at") String joinedAt,
        /** 统一 OpenID（如有） */
        @JsonProperty("union_openid") String unionOpenid
) {
}
