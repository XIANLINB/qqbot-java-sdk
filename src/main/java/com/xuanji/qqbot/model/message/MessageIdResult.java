package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 发消息成功响应。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageIdResult(
        /** 消息 ID，可用于撤回等 */
        @JsonProperty("id") String id,
        /** 发送时间 RFC3339 */
        @JsonProperty("timestamp") String timestamp,
        /** 扩展信息 */
        @JsonProperty("ext_info") ExtInfo extInfo
) {
    /**
     * 扩展信息。
     *
     * @param refIdx 本消息的引用索引，供后续 message_reference 使用
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtInfo(@JsonProperty("ref_idx") String refIdx) {
    }

    /**
     * @return 引用索引，可能为 null
     */
    public String refIdx() {
        return extInfo == null ? null : extInfo.refIdx();
    }
}
