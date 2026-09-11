package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.model.message.ArkData;
import com.xuanji.qqbot.model.message.MessageAttachment;
import com.xuanji.qqbot.model.message.User;

import java.util.List;

/**
 * 消息事件中的嵌套元素（官方 MsgElement），支持递归。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MsgElement(
        /** 元素引用索引 */
        @JsonProperty("msg_idx") String msgIdx,
        @JsonProperty("author") User author,
        @JsonProperty("message_type") Integer messageType,
        @JsonProperty("content") String content,
        @JsonProperty("attachments") List<MessageAttachment> attachments,
        @JsonProperty("ark_data") ArkData arkData,
        @JsonProperty("msg_elements") List<MsgElement> msgElements
) {
}
