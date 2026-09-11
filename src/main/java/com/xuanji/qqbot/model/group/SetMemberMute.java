package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @deprecated 请使用官方命名 {@link SetMemberMuteState}。
 */
@Deprecated
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SetMemberMute(
        /** add / update / del */
        @JsonProperty("op") String op,
        /** 成员 OpenID */
        @JsonProperty("member_openid") String memberOpenid,
        /** 禁言到期 RFC3339；del 时可空串立即解除 */
        @JsonProperty("mute_expire_at") String muteExpireAt
) {
    /** op=add */
    public static final String OP_ADD = "add";
    /** op=update */
    public static final String OP_UPDATE = "update";
    /** op=del */
    public static final String OP_DEL = "del";

    /**
     * @param memberOpenid 成员 OpenID
     * @param muteExpireAt 到期时间 RFC3339
     * @return 增加禁言操作
     */
    public static SetMemberMute add(String memberOpenid, String muteExpireAt) {
        return new SetMemberMute(OP_ADD, memberOpenid, muteExpireAt);
    }

    /**
     * @param memberOpenid 成员 OpenID
     * @return 解除禁言操作
     */
    public static SetMemberMute unmute(String memberOpenid) {
        return new SetMemberMute(OP_DEL, memberOpenid, "");
    }
}
