package com.xuanji.qqbot.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.http.ApiCall;
import com.xuanji.qqbot.model.group.JoinApprovalModels;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 入群自动审批策略。GET/POST/PATCH/DELETE /v2/groups/join_approval_strategy。
 */
public final class JoinApprovalStrategyApi {
    private static final String BASE = "/v2/groups/join_approval_strategy";

    private final ApiCall api;

    /**
     * @param api 已注入 token 的调用器
     */
    public JoinApprovalStrategyApi(ApiCall api) {
        this.api = api;
    }

    /**
     * 查询生效中的策略列表。
     *
     * @param cursor 分页游标，首次 null
     * @param limit  每页条数，默认 20，最大 50
     * @return 策略页
     */
    public JoinApprovalModels.StrategyPage listStrategies(String cursor, Integer limit) {
        StringBuilder path = new StringBuilder(BASE);
        boolean first = true;
        if (cursor != null && !cursor.isBlank()) {
            path.append(first ? '?' : '&').append("cursor=").append(ApiCall.pathEncode(cursor));
            first = false;
        }
        if (limit != null) {
            path.append(first ? '?' : '&').append("limit=").append(limit);
        }
        return api.get(path.toString(), JoinApprovalModels.StrategyPage.class);
    }

    /**
     * 创建入群自动审批策略（一个机器人最多 20 个）。
     *
     * @param request 创建请求
     * @return strategy_id 等
     */
    public JoinApprovalModels.CreateStrategyResult createStrategy(JoinApprovalModels.CreateStrategyRequest request) {
        Objects.requireNonNull(request, "request");
        return api.post(BASE, request, JoinApprovalModels.CreateStrategyResult.class);
    }

    /**
     * 修改策略（启用/过期/关联群/备注）。
     *
     * @param strategyId 策略 ID
     * @param request    修改请求
     * @return 修改结果
     */
    public JoinApprovalModels.UpdateStrategyResult updateStrategy(String strategyId,
                                                                   JoinApprovalModels.UpdateStrategyRequest request) {
        requireId(strategyId, "strategyId");
        Objects.requireNonNull(request, "request");
        return api.patch(BASE + "/" + ApiCall.pathEncode(strategyId), request,
                JoinApprovalModels.UpdateStrategyResult.class);
    }

    /**
     * 删除策略。
     *
     * @param strategyId 策略 ID
     */
    public void deleteStrategy(String strategyId) {
        requireId(strategyId, "strategyId");
        api.delete(BASE + "/" + ApiCall.pathEncode(strategyId), Object.class);
    }

    /**
     * 立即执行策略：对关联全部群全量扫描，命中白名单的入群申请自动通过。
     * 异步执行，约 10 分钟完成；官方响应无 body。
     *
     * @param strategyId 策略 ID
     */
    public void executeStrategy(String strategyId) {
        requireId(strategyId, "strategyId");
        api.post(BASE + "/" + ApiCall.pathEncode(strategyId) + "/execute", Map.of(), Object.class);
    }

    /**
     * 修改策略白名单 QQ 号码（注意：是 QQ 号码字符串，不是 openid）。
     * 单次最多 10000 个，总量上限 10 万。
     *
     * @param strategyId 策略 ID
     * @param op         add 新增 / del 删除
     * @param qqNumbers  QQ 号码列表（字符串）
     * @return 操作后白名单数量等
     */
    public WhitelistResult updateWhitelist(String strategyId, String op, List<String> qqNumbers) {
        requireId(strategyId, "strategyId");
        Objects.requireNonNull(op, "op");
        if (!JoinApprovalModels.WhitelistUsersRequest.OP_ADD.equals(op)
                && !JoinApprovalModels.WhitelistUsersRequest.OP_DEL.equals(op)) {
            throw new IllegalArgumentException("op 仅支持 add/del");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("op", op);
        body.put("whitelist_users", qqNumbers);
        return api.post(BASE + "/" + ApiCall.pathEncode(strategyId) + "/whitelist_users",
                body, WhitelistResult.class);
    }

    /**
     * 白名单追加 QQ 号码。
     *
     * @param strategyId 策略 ID
     * @param qqNumbers  QQ 号码
     * @return 结果
     */
    public WhitelistResult whitelistAdd(String strategyId, List<String> qqNumbers) {
        return updateWhitelist(strategyId, JoinApprovalModels.WhitelistUsersRequest.OP_ADD, qqNumbers);
    }

    /**
     * 白名单删除 QQ 号码。
     *
     * @param strategyId 策略 ID
     * @param qqNumbers  QQ 号码
     * @return 结果
     */
    public WhitelistResult whitelistDel(String strategyId, List<String> qqNumbers) {
        return updateWhitelist(strategyId, JoinApprovalModels.WhitelistUsersRequest.OP_DEL, qqNumbers);
    }

    private static void requireId(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    /**
     * 白名单修改响应。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WhitelistResult(
            @JsonProperty("strategy_id") String strategyId,
            @JsonProperty("whitelist_user_count") Integer whitelistUserCount,
            @JsonProperty("updated_at") String updatedAt
    ) {
    }
}
