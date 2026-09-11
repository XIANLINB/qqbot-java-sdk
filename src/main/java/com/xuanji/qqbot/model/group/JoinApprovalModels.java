package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 入群自动审批策略相关模型。对齐官方 /v2/groups/join_approval_strategy。
 */
public final class JoinApprovalModels {
    private JoinApprovalModels() {
    }

    /** 启用 */
    public static final String ENABLE_ON = "on";
    /** 关闭 */
    public static final String ENABLE_OFF = "off";

    /**
     * 策略列表页。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StrategyPage(
            @JsonProperty("strategies") List<JoinApprovalStrategy> strategies,
            @JsonProperty("next_cursor") String nextCursor
    ) {
    }

    /**
     * 单条策略。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JoinApprovalStrategy(
            @JsonProperty("strategy_id") String strategyId,
            @JsonProperty("group_openids") List<String> groupOpenids,
            @JsonProperty("group_ids") List<Object> groupIds,
            @JsonProperty("whitelist_user_count") Integer whitelistUserCount,
            @JsonProperty("is_enable") String isEnable,
            @JsonProperty("expire_at") String expireAt,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("updated_at") String updatedAt,
            @JsonProperty("remark") String remark
    ) {
        /**
         * @return 是否启用
         */
        public boolean isEnabled() {
            return ENABLE_ON.equalsIgnoreCase(isEnable);
        }
    }

    /**
     * 创建策略请求。
     * group_openids 与 group_ids 二选一必填。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateStrategyRequest(
            @JsonProperty("group_openids") List<String> groupOpenids,
            @JsonProperty("group_ids") List<Object> groupIds,
            @JsonProperty("is_enable") String isEnable,
            @JsonProperty("expire_at") String expireAt,
            @JsonProperty("remark") String remark
    ) {
        /**
         * @param groupOpenids 群 openid 列表，≤100
         * @return 创建请求（默认 on）
         */
        public static CreateStrategyRequest byGroupOpenids(List<String> groupOpenids) {
            return new CreateStrategyRequest(groupOpenids, null, ENABLE_ON, null, null);
        }

        /**
         * @param groupIds QQ 群号列表，≤100
         * @return 创建请求（默认 on）
         */
        public static CreateStrategyRequest byGroupIds(List<Object> groupIds) {
            return new CreateStrategyRequest(null, groupIds, ENABLE_ON, null, null);
        }

        /**
         * @param expireAt RFC3339
         * @return 带过期时间
         */
        public CreateStrategyRequest expireAt(String expireAt) {
            return new CreateStrategyRequest(groupOpenids, groupIds, isEnable, expireAt, remark);
        }

        /**
         * @param remark 备注
         * @return 带备注
         */
        public CreateStrategyRequest remark(String remark) {
            return new CreateStrategyRequest(groupOpenids, groupIds, isEnable, expireAt, remark);
        }

        /**
         * @param enable on/off
         * @return 带启用状态
         */
        public CreateStrategyRequest enable(String enable) {
            return new CreateStrategyRequest(groupOpenids, groupIds, enable, expireAt, remark);
        }
    }

    /**
     * 修改策略请求（PATCH）。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpdateStrategyRequest(
            @JsonProperty("is_enable") String isEnable,
            @JsonProperty("expire_at") String expireAt,
            @JsonProperty("group_action") GroupAction groupAction,
            @JsonProperty("remark") String remark
    ) {
        /**
         * @return 停用
         */
        public static UpdateStrategyRequest disable() {
            return new UpdateStrategyRequest(ENABLE_OFF, null, null, null);
        }

        /**
         * @return 启用
         */
        public static UpdateStrategyRequest enable() {
            return new UpdateStrategyRequest(ENABLE_ON, null, null, null);
        }

        /**
         * @param expireAt RFC3339
         * @return 改过期时间
         */
        public static UpdateStrategyRequest expireAt(String expireAt) {
            return new UpdateStrategyRequest(null, expireAt, null, null);
        }

        /**
         * @param action 群增删
         * @return 改关联群
         */
        public static UpdateStrategyRequest groupAction(GroupAction action) {
            return new UpdateStrategyRequest(null, null, action, null);
        }

        /**
         * @param remark 备注
         * @return 改备注
         */
        public static UpdateStrategyRequest remark(String remark) {
            return new UpdateStrategyRequest(null, null, null, remark);
        }
    }

    /**
     * 关联群增删。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroupAction(
            @JsonProperty("op") String op,
            @JsonProperty("group_openids") List<String> groupOpenids,
            @JsonProperty("group_ids") List<Object> groupIds
    ) {
        /** add */
        public static final String OP_ADD = "add";
        /** del */
        public static final String OP_DEL = "del";

        /**
         * @param openids 群 openid
         * @return 添加
         */
        public static GroupAction addOpenids(List<String> openids) {
            return new GroupAction(OP_ADD, openids, null);
        }

        /**
         * @param openids 群 openid
         * @return 移除
         */
        public static GroupAction delOpenids(List<String> openids) {
            return new GroupAction(OP_DEL, openids, null);
        }

        /**
         * @param ids QQ 群号
         * @return 添加
         */
        public static GroupAction addIds(List<Object> ids) {
            return new GroupAction(OP_ADD, null, ids);
        }

        /**
         * @param ids QQ 群号
         * @return 移除
         */
        public static GroupAction delIds(List<Object> ids) {
            return new GroupAction(OP_DEL, null, ids);
        }
    }

    /**
     * 创建策略响应。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateStrategyResult(
            @JsonProperty("strategy_id") String strategyId,
            @JsonProperty("is_enable") String isEnable,
            @JsonProperty("expire_at") String expireAt
    ) {
    }

    /**
     * 修改策略响应。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpdateStrategyResult(
            @JsonProperty("is_enable") String isEnable,
            @JsonProperty("expire_at") String expireAt
    ) {
    }

    /**
     * 白名单操作请求。字段名为 whitelist_users，值为 QQ 号码字符串列表（非 openid）。
     * op 仅 add/del；单次最多 10000 个。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WhitelistUsersRequest(
            /** add / del */
            @JsonProperty("op") String op,
            /** QQ 号码列表（字符串，避免精度问题） */
            @JsonProperty("whitelist_users") List<String> whitelistUsers
    ) {
        /** add */
        public static final String OP_ADD = "add";
        /** del */
        public static final String OP_DEL = "del";

        /**
         * @param qqNumbers QQ 号码
         * @return 追加
         */
        public static WhitelistUsersRequest add(List<String> qqNumbers) {
            return new WhitelistUsersRequest(OP_ADD, qqNumbers);
        }

        /**
         * @param qqNumbers QQ 号码
         * @return 删除
         */
        public static WhitelistUsersRequest del(List<String> qqNumbers) {
            return new WhitelistUsersRequest(OP_DEL, qqNumbers);
        }
    }
}
