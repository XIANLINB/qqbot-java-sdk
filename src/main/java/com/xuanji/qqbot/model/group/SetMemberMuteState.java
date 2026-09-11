package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 设置群成员禁言（官方 SetMemberMuteState）。
 * <p>
 * op：add / update / del；
 * 增/改仅可操作普通成员（不能操作群主、管理员、机器人）；
 * 最大禁言约 30 天；机器人需群管理员身份。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SetMemberMuteState(
        /** add=增加 update=改到期 del=解除 */
        @JsonProperty("op") String op,
        /** 被禁言成员 OpenID */
        @JsonProperty("member_openid") String memberOpenid,
        /** 禁言到期 RFC3339；op=del 可传空串立即解除 */
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
     * @param muteExpireAt 到期时间 RFC3339，如 2026-08-05T11:23:05+08:00
     * @return 增加禁言
     */
    public static SetMemberMuteState add(String memberOpenid, String muteExpireAt) {
        return new SetMemberMuteState(OP_ADD, memberOpenid, muteExpireAt);
    }

    /**
     * 按时长禁言（从当前时刻起 duration）。
     *
     * @param memberOpenid 成员 OpenID
     * @param duration     时长，最大 30 天
     * @return 增加禁言
     */
    public static SetMemberMuteState add(String memberOpenid, Duration duration) {
        Duration d = duration == null || duration.isNegative() || duration.isZero()
                ? Duration.ofSeconds(1) : duration;
        Duration max = Duration.ofDays(30);
        if (d.compareTo(max) > 0) {
            d = max;
        }
        String expire = OffsetDateTime.now(ZoneOffset.ofHours(8))
                .plus(d)
                .withNano(0)
                .toString();
        return add(memberOpenid, expire);
    }

    /**
     * @param memberOpenid 成员 OpenID
     * @param muteExpireAt 新的到期时间
     * @return 更新到期时间
     */
    public static SetMemberMuteState update(String memberOpenid, String muteExpireAt) {
        return new SetMemberMuteState(OP_UPDATE, memberOpenid, muteExpireAt);
    }

    /**
     * @param memberOpenid 成员 OpenID
     * @return 解除禁言（mute_expire_at 空串）
     */
    public static SetMemberMuteState unmute(String memberOpenid) {
        return new SetMemberMuteState(OP_DEL, memberOpenid, "");
    }

    /** @deprecated 用 {@link #unmute(String)}，官方名 SetMemberMuteState */
    @Deprecated
    public static SetMemberMuteState del(String memberOpenid) {
        return unmute(memberOpenid);
    }
}
