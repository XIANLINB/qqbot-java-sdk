package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 入群申请分页列表。
 * 实测：verify_info 可为 null（例如 invited 无验证）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JoinRequestPage(
        @JsonProperty("list") List<JoinRequest> list,
        @JsonProperty("next_cursor") String nextCursor
) {
    /**
     * 入群申请条目（对齐实测字段）。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JoinRequest(
            @JsonProperty("join_request_id") String joinRequestId,
            @JsonProperty("risk_tips") String riskTips,
            @JsonProperty("union_openid") String unionOpenid,
            @JsonProperty("member_openid") String memberOpenid,
            @JsonProperty("username") String username,
            @JsonProperty("apply_at") String applyAt,
            /** self_apply / invited */
            @JsonProperty("apply_source") String applySource,
            @JsonProperty("invited_by") String invitedBy,
            @JsonProperty("bot") Boolean bot,
            /** 可能为 null */
            @JsonProperty("verify_info") VerifyInfo verifyInfo
    ) {
        /**
         * @return 是否主动申请
         */
        public boolean isSelfApply() {
            return "self_apply".equalsIgnoreCase(applySource);
        }

        /**
         * @return 是否邀请
         */
        public boolean isInvited() {
            return "invited".equalsIgnoreCase(applySource);
        }
    }

    /**
     * 入群验证信息。
     * method=verify_message 时看 verify_message；
     * method=admin_review_qa 时看 review_qa_list。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VerifyInfo(
            @JsonProperty("method") String method,
            @JsonProperty("verify_message") String verifyMessage,
            @JsonProperty("review_qa_list") List<ReviewQA> reviewQaList
    ) {
        /**
         * @return 是否验证消息
         */
        public boolean isVerifyMessage() {
            return "verify_message".equalsIgnoreCase(method);
        }

        /**
         * @return 是否问答审核
         */
        public boolean isReviewQa() {
            return "admin_review_qa".equalsIgnoreCase(method);
        }
    }

    /**
     * 问答项。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewQA(
            @JsonProperty("question") String question,
            @JsonProperty("answer") String answer
    ) {
    }
}
