package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 群禁言设置查询响应。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestrictChatSetting(
        /** 全员禁言规则 */
        @JsonProperty("global_rule") GlobalMuteRule globalRule,
        /** 当前禁言成员 */
        @JsonProperty("members") List<MemberMuteState> members
) {
    /**
     * 全员禁言规则。
     *
     * @param mode none/always/schedule
     * @param scheduleRules 定时禁言
     * @param recurringRules 周期禁言
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GlobalMuteRule(
            @JsonProperty("mode") String mode,
            @JsonProperty("schedule_rules") List<MuteScheduleRule> scheduleRules,
            @JsonProperty("recurring_rules") List<MuteRecurringRule> recurringRules
    ) {
    }

    /**
     * 定时禁言规则。
     *
     * @param taskId 任务 ID
     * @param startAt 开始
     * @param endAt 结束
     * @param enabled 是否启用
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MuteScheduleRule(
            @JsonProperty("task_id") String taskId,
            @JsonProperty("start_at") String startAt,
            @JsonProperty("end_at") String endAt,
            @JsonProperty("enabled") Boolean enabled
    ) {
    }

    /**
     * 周期禁言规则。
     *
     * @param taskId 任务 ID
     * @param weekdays 1=周一 … 7=周日
     * @param startTime HH:mm
     * @param endTime HH:mm
     * @param enabled 是否启用
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MuteRecurringRule(
            @JsonProperty("task_id") String taskId,
            @JsonProperty("weekdays") List<Integer> weekdays,
            @JsonProperty("start_time") String startTime,
            @JsonProperty("end_time") String endTime,
            @JsonProperty("enabled") Boolean enabled
    ) {
    }

    /**
     * 成员禁言状态。
     *
     * @param memberOpenid 成员 OpenID
     * @param muteExpireAt 到期时间 RFC3339
     * @param username 昵称
     * @param unionOpenid 统一 OpenID
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MemberMuteState(
            @JsonProperty("member_openid") String memberOpenid,
            @JsonProperty("mute_expire_at") String muteExpireAt,
            @JsonProperty("username") String username,
            @JsonProperty("union_openid") String unionOpenid
    ) {
    }
}
