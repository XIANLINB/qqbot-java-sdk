package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 流式消息响应。首片返回的 id 即后续分片所需 stream_msg_id。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StreamMessageResult(
        /** 消息/流式 id */
        @JsonProperty("id") String id,
        /** 时间 RFC3339 */
        @JsonProperty("timestamp") String timestamp,
        /** 流式剩余长度（字符） */
        @JsonProperty("remain_msg_len") Integer remainMsgLen,
        /** 扩展信息 */
        @JsonProperty("ext_info") MessageIdResult.ExtInfo extInfo
) {
}
