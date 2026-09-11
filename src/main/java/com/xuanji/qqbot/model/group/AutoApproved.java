package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 入群申请自动通过信息（官方 auto_approved）。
 * 仅当命中自动审批策略时出现。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutoApproved(
        /** 命中的自动审批策略 ID */
        @JsonProperty("strategy_id") String strategyId
) {
}
