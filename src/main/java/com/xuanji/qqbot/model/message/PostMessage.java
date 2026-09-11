package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.model.keyboard.Keyboard;

/**
 * 发消息请求体（单聊/群聊共用）。
 * <p>
 * 官方发送类型：
 * 0=文本 content · 2=Markdown markdown · 3=Ark 卡片 ark ·
 * 6=输入中 input_notify · 7=富媒体 media · 8=图文卡片 card
 * <p>
 * 被动：msg_id 与 event_id 二选一；msg_seq 与 msg_id 联用。
 * markdown 与 content 互斥；markdown 模板 ID 已废弃，请用 content 原生语法。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PostMessage(
        /** 0/2/3/6/7/8 */
        @JsonProperty("msg_type") Integer msgType,
        /** 文本；msg_type=0 */
        @JsonProperty("content") String content,
        /** Markdown；msg_type=2 */
        @JsonProperty("markdown") Markdown markdown,
        /** 内嵌键盘；与 markdown 搭配，≤5 行×5 列 */
        @JsonProperty("keyboard") Keyboard keyboard,
        /** 被动回复原消息 id */
        @JsonProperty("msg_id") String msgId,
        /** 被动回复事件信封 id */
        @JsonProperty("event_id") String eventId,
        /** 与 msg_id 联用的序号 */
        @JsonProperty("msg_seq") Integer msgSeq,
        /** 富媒体；msg_type=7 */
        @JsonProperty("media") MediaInfo media,
        /** 引用展示 */
        @JsonProperty("message_reference") MessageReference messageReference,
        /** 互动召回 */
        @JsonProperty("is_wakeup") Boolean isWakeup,
        /** 输入中；msg_type=6 */
        @JsonProperty("input_notify") InputNotify inputNotify,
        /** Ark 卡片；msg_type=3 */
        @JsonProperty("ark") ArkMsg ark,
        /** 图文卡片；msg_type=8 */
        @JsonProperty("card") ImageTextCard card
) {
    /**
     * 文本消息。
     *
     * @param content 文本
     * @return 消息体
     */
    public static PostMessage text(String content) {
        return new PostMessage(MsgType.TEXT.code(), content, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    /**
     * Markdown 消息（原生语法）。
     *
     * @param content Markdown
     * @return 消息体
     */
    public static PostMessage markdown(String content) {
        return new PostMessage(MsgType.MARKDOWN.code(), null, Markdown.of(content), null,
                null, null, null, null, null, null, null, null, null);
    }

    /**
     * Markdown + 内嵌键盘。
     *
     * @param content Markdown
     * @param keyboard 键盘
     * @return 消息体
     */
    public static PostMessage markdownWithKeyboard(String content, Keyboard keyboard) {
        return new PostMessage(MsgType.MARKDOWN.code(), null, Markdown.of(content), keyboard,
                null, null, null, null, null, null, null, null, null);
    }

    /**
     * Ark 卡片消息（msg_type=3）。
     *
     * @param ark ArkMsg
     * @return 消息体
     */
    public static PostMessage ark(ArkMsg ark) {
        return new PostMessage(MsgType.ARK.code(), null, null, null,
                null, null, null, null, null, null, null, ark, null);
    }

    /**
     * 图文卡片消息（msg_type=8）。
     *
     * @param card ImageTextCard
     * @return 消息体
     */
    public static PostMessage card(ImageTextCard card) {
        return new PostMessage(MsgType.CARD.code(), null, null, null,
                null, null, null, null, null, null, null, null, card);
    }

    /**
     * 富媒体（msg_type=7）。
     *
     * @param fileInfo file_info
     * @return 消息体
     */
    public static PostMessage media(String fileInfo) {
        return new PostMessage(MsgType.MEDIA.code(), null, null, null,
                null, null, null, MediaInfo.of(fileInfo), null, null, null, null, null);
    }

    /**
     * @param msgId 被回复消息 id
     * @param msgSeq 序号
     * @return 新消息体
     */
    public PostMessage reply(String msgId, Integer msgSeq) {
        return new PostMessage(msgType, content, markdown, keyboard,
                msgId, null, msgSeq == null ? 1 : msgSeq, media, messageReference,
                isWakeup, inputNotify, ark, card);
    }

    /**
     * @param eventId 事件信封 id
     * @return 新消息体
     */
    public PostMessage replyByEvent(String eventId) {
        return new PostMessage(msgType, content, markdown, keyboard,
                null, eventId, null, media, messageReference,
                isWakeup, inputNotify, ark, card);
    }

    /**
     * @param keyboard 键盘
     * @return 新消息体
     */
    public PostMessage withKeyboard(Keyboard keyboard) {
        return new PostMessage(msgType, content, markdown, keyboard,
                msgId, eventId, msgSeq, media, messageReference,
                isWakeup, inputNotify, ark, card);
    }

    /**
     * @param messageId 引用索引
     * @return 新消息体
     */
    public PostMessage withReference(String messageId) {
        return new PostMessage(msgType, content, markdown, keyboard,
                msgId, eventId, msgSeq, media, MessageReference.of(messageId),
                isWakeup, inputNotify, ark, card);
    }
}
