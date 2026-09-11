package com.xuanji.qqbot.model.message;

import com.xuanji.qqbot.model.keyboard.Keyboard;

/**
 * 被动回复辅助：统一写入 msg_id/event_id 与 msg_seq。
 */
public final class Reply {
    private final String msgId;
    private final String eventId;
    private final int seq;

    private Reply(String msgId, String eventId, int seq) {
        this.msgId = msgId;
        this.eventId = eventId;
        this.seq = seq;
    }

    /**
     * 按原消息 id 被动回复。
     *
     * @param msgId 事件中的消息 id
     * @return Reply
     */
    public static Reply to(String msgId) {
        return new Reply(msgId, null, 1);
    }

    /**
     * 按事件信封 id 被动回复。
     *
     * @param eventId 事件最外层 id
     * @return Reply
     */
    public static Reply toEvent(String eventId) {
        return new Reply(null, eventId, 1);
    }

    /**
     * 设置回复序号。
     *
     * @param seq 序号，默认 1；同 id 再发需递增
     * @return Reply
     */
    public Reply seq(int seq) {
        return new Reply(msgId, eventId, seq);
    }

    /**
     * @return 被动回复的原消息 id，可能为 null
     */
    public String msgId() {
        return msgId;
    }

    /**
     * @return 被动回复的事件 id，可能为 null
     */
    public String eventId() {
        return eventId;
    }

    /**
     * @return msg_seq
     */
    public int seq() {
        return seq;
    }

    /**
     * 应用到出站消息体。
     *
     * @param base 原消息体
     * @return 附带被动字段的新消息体
     */
    public PostMessage apply(PostMessage base) {
        if (eventId != null) {
            return base.replyByEvent(eventId);
        }
        return base.reply(msgId, seq);
    }
}
