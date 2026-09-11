package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 群黑名单分页列表。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BlacklistPage(
        /** 黑名单用户 */
        @JsonProperty("users") List<BlacklistUser> users,
        /** 下一页游标 */
        @JsonProperty("next_cursor") String nextCursor
) {
    /**
     * 黑名单条目。
     *
     * @param unionOpenid 统一 OpenID
     * @param memberOpenid 成员 OpenID
     * @param username 昵称
     * @param bannedAt 拉黑时间
     * @param bot 是否机器人
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BlacklistUser(
            @JsonProperty("union_openid") String unionOpenid,
            @JsonProperty("member_openid") String memberOpenid,
            @JsonProperty("username") String username,
            @JsonProperty("banned_at") String bannedAt,
            @JsonProperty("bot") Boolean bot
    ) {
    }
}
