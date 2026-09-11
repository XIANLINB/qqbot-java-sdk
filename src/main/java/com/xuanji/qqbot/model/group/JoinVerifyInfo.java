package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 入群验证方式。
 * <p>
 * method：
 * <ul>
 *   <li>verify_message — 验证消息，见 verify_message</li>
 *   <li>admin_review_qa — 问答审核，见 review_qa_list</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JoinVerifyInfo(
        @JsonProperty("method") String method,
        @JsonProperty("verify_message") String verifyMessage,
        @JsonProperty("review_qa_list") List<ReviewQa> reviewQaList
) {
    /**
     * @return 是否验证消息模式
     */
    public boolean isVerifyMessage() {
        return "verify_message".equalsIgnoreCase(method);
    }

    /**
     * @return 是否问答审核模式
     */
    public boolean isReviewQa() {
        return "admin_review_qa".equalsIgnoreCase(method);
    }

    /**
     * @return 验证消息内容（method=verify_message 时）
     */
    public String message() {
        return verifyMessage;
    }

    /**
     * @return 问答列表（method=admin_review_qa 时）
     */
    public List<ReviewQa> questions() {
        return reviewQaList == null ? List.of() : reviewQaList;
    }

    /**
     * 问答项。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewQa(
            @JsonProperty("question") String question,
            @JsonProperty("answer") String answer
    ) {
    }
}
