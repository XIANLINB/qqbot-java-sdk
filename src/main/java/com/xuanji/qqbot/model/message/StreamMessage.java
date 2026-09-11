package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 流式单聊消息请求体。
 * <p>
 * 对应官方 POST /v2/users/{user_openid}/stream_messages。
 * 每个分片使用相同 stream_msg_id，index 从 0 递增。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record StreamMessage(
        /** 模式：append=拼接；replace=全量替换（须以已下发前缀开头） */
        @JsonProperty("input_mode") String inputMode,
        /** 状态：1=生成中，10=生成结束 */
        @JsonProperty("input_state") Integer inputState,
        /** 分片序号，从 0 递增 */
        @JsonProperty("index") Integer index,
        /** 内容类型：text / markdown */
        @JsonProperty("content_type") String contentType,
        /** 本片正文 */
        @JsonProperty("content_raw") String contentRaw,
        /** 被动回复事件 id，与 msg_id 二选一 */
        @JsonProperty("event_id") String eventId,
        /** 被动回复原消息 id */
        @JsonProperty("msg_id") String msgId,
        /** 流式消息 id；首片由服务端返回，续片回传 */
        @JsonProperty("stream_msg_id") String streamMsgId,
        /** 消息序号，用于去重 */
        @JsonProperty("msg_seq") Integer msgSeq,
        /** 是否召回消息；true 时不校验 msg_id/event_id 有效期 */
        @JsonProperty("is_wakeup") Boolean isWakeup
) {
    /** input_state=1 生成中 */
    public static final int STATE_GENERATING = 1;
    /** input_state=10 生成结束 */
    public static final int STATE_FINISHED = 10;

    /** input_mode=append */
    public static final String MODE_APPEND = "append";
    /** input_mode=replace */
    public static final String MODE_REPLACE = "replace";
    /** content_type=text */
    public static final String CONTENT_TEXT = "text";
    /** content_type=markdown */
    public static final String CONTENT_MARKDOWN = "markdown";

    /**
     * 首片：state=1，index=0，replace 模式。
     *
     * @param contentRaw   正文
     * @param contentType  text 或 markdown
     * @param msgId        被动回复原消息 id
     * @param msgSeq       序号，null 视为 1
     * @return 流式消息
     */
    public static StreamMessage first(String contentRaw, String contentType, String msgId, Integer msgSeq) {
        return new StreamMessage(MODE_REPLACE, STATE_GENERATING, 0, contentType, contentRaw,
                null, msgId, null, msgSeq == null ? 1 : msgSeq, null);
    }

    /**
     * 续片：追加正文。
     *
     * @param streamMsgId 流式消息 id
     * @param index       分片序号
     * @param contentRaw  本片正文
     * @param contentType text 或 markdown
     * @param msgId       被动回复原消息 id
     * @param msgSeq      序号
     * @return 流式消息
     */
    public static StreamMessage append(String streamMsgId, int index, String contentRaw,
                                       String contentType, String msgId, Integer msgSeq) {
        return new StreamMessage(MODE_APPEND, STATE_GENERATING, index, contentType, contentRaw,
                null, msgId, streamMsgId, msgSeq == null ? 1 : msgSeq, null);
    }

    /**
     * 结束片：state=10。
     *
     * @param streamMsgId 流式消息 id
     * @param index       分片序号
     * @param contentRaw  最终正文（replace 模式下通常为全量）
     * @param contentType text 或 markdown
     * @param msgId       被动回复原消息 id
     * @param msgSeq      序号
     * @return 流式消息
     */
    public static StreamMessage finish(String streamMsgId, int index, String contentRaw,
                                       String contentType, String msgId, Integer msgSeq) {
        return new StreamMessage(MODE_REPLACE, STATE_FINISHED, index, contentType, contentRaw,
                null, msgId, streamMsgId, msgSeq == null ? 1 : msgSeq, null);
    }

    /**
     * 改用 event_id 被动回复。
     *
     * @param eventId 事件信封 id
     * @return 新消息
     */
    public StreamMessage withEventId(String eventId) {
        return new StreamMessage(inputMode, inputState, index, contentType, contentRaw,
                eventId, null, streamMsgId, msgSeq, isWakeup);
    }
}
