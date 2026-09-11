package com.xuanji.qqbot.model.message;

/**
 * 回复目标：msgId（被动消息）或 eventId（事件回复）。
 */
public final class ReplyTarget {
    private final String msgId;
    private final String eventId;
    private final int seq;

    private ReplyTarget(String msgId, String eventId, int seq) {
        this.msgId = msgId;
        this.eventId = eventId;
        this.seq = seq;
    }

    /**
     * 按原消息 id 被动回复。
     *
     * @param msgId 事件中的消息 id
     * @return 回复目标
     */
    public static ReplyTarget to(String msgId) {
        return new ReplyTarget(msgId, null, 1);
    }

    /**
     * 按事件信封 id 回复。
     *
     * @param eventId 事件 id
     * @return 回复目标
     */
    public static ReplyTarget event(String eventId) {
        return new ReplyTarget(null, eventId, 1);
    }

    /**
     * @param seq 序号，默认 1
     * @return 新目标
     */
    public ReplyTarget seq(int seq) {
        return new ReplyTarget(msgId, eventId, seq);
    }

    public String msgId() {
        return msgId;
    }

    public String eventId() {
        return eventId;
    }

    public int seq() {
        return seq;
    }

    /**
     * 应用到消息体。
     *
     * @param base 基础消息
     * @return 带被动字段的消息
     */
    public PostMessage apply(PostMessage base) {
        if (eventId != null) {
            return base.replyByEvent(eventId);
        }
        return base.reply(msgId, seq);
    }
}
