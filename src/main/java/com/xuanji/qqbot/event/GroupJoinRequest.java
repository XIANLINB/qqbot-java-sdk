package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.model.group.AutoApproved;
import com.xuanji.qqbot.model.group.JoinVerifyInfo;

import java.util.List;

/**
 * GROUP_JOIN_REQUEST：用户申请加群。
 * <p>
 * apply_source：
 * <ul>
 *   <li>self_apply — 用户主动申请</li>
 *   <li>invited — 被他人邀请，invited_by 为邀请人 OpenID</li>
 * </ul>
 * verify_info：验证消息 / 问答审核；
 * auto_approved：命中自动审批策略时出现（strategy_id）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupJoinRequest(
        @JsonProperty("apply_at") String applyAt,
        /** self_apply | invited */
        @JsonProperty("apply_source") String applySource,
        @JsonProperty("group_openid") String groupOpenid,
        @JsonProperty("join_request_id") String joinRequestId,
        @JsonProperty("member_openid") String memberOpenid,
        @JsonProperty("username") String username,
        @JsonProperty("verify_info") JoinVerifyInfo verifyInfo,
        /** apply_source=invited 时为邀请人 OpenID */
        @JsonProperty("invited_by") String invitedBy,
        /** 自动审批通过信息，可能无 */
        @JsonProperty("auto_approved") AutoApproved autoApproved,
        @JsonProperty("bot") Boolean bot,
        @JsonIgnore String eventEnvelopeId
) implements Event {
    @Override
    public String eventId() {
        return eventEnvelopeId;
    }

    @Override
    public String type() {
        return EventType.GROUP_JOIN_REQUEST;
    }

    @Override
    public String groupOpenid() {
        return groupOpenid;
    }

    /**
     * @return 申请人 OpenID
     */
    public String applicantOpenid() {
        return memberOpenid;
    }

    /**
     * @return 申请 ID，审批接口回传
     */
    public String requestId() {
        return joinRequestId;
    }

    /**
     * @return 是否主动申请（非邀请）
     */
    public boolean isSelfApply() {
        return "self_apply".equalsIgnoreCase(applySource);
    }

    /**
     * @return 是否他人邀请入群
     */
    public boolean isInvited() {
        return "invited".equalsIgnoreCase(applySource);
    }

    /**
     * @return 邀请人 OpenID（仅 invited）
     */
    public String inviterOpenid() {
        return invitedBy;
    }

    /**
     * @return 自动通过策略信息，未自动通过为 null
     */
    public AutoApproved autoApproved() {
        return autoApproved;
    }

    /**
     * @return 是否已由自动策略通过
     */
    public boolean isAutoApproved() {
        return autoApproved != null && autoApproved.strategyId() != null
                && !autoApproved.strategyId().isBlank();
    }

    /**
     * @return 自动通过策略 ID
     */
    public String strategyId() {
        return autoApproved == null ? null : autoApproved.strategyId();
    }

    /**
     * @return 验证信息
     */
    public JoinVerifyInfo verify() {
        return verifyInfo;
    }

    /**
     * @return 是否验证消息模式
     */
    public boolean isVerifyMessage() {
        return verifyInfo != null && verifyInfo.isVerifyMessage();
    }

    /**
     * @return 是否问答审核模式
     */
    public boolean isReviewQa() {
        return verifyInfo != null && verifyInfo.isReviewQa();
    }

    /**
     * @return 验证消息文本
     */
    public String verifyMessage() {
        return verifyInfo == null ? null : verifyInfo.message();
    }

    /**
     * @return 问答列表
     */
    public List<JoinVerifyInfo.ReviewQa> reviewQuestions() {
        return verifyInfo == null ? List.of() : verifyInfo.questions();
    }
}
