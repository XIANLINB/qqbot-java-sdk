package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 引用回复。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageReference(
        /** 被引用消息索引，如 REFIDX_xxx；非机器人消息取事件 ext 中 msg_idx，机器人消息取响应 ext_info.ref_idx */
        @JsonProperty("message_id") String messageId
) {
    /**
     * @param messageId 消息索引
     * @return 引用对象
     */
    public static MessageReference of(String messageId) {
        return new MessageReference(messageId);
    }
}
