package com.xuanji.qqbot.model.message;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 被动回复 msg_seq 自动分配。
 * 同一 msg_id 每次 +1，避免官方「同 msg_id+msg_seq 重复发送失败」。
 */
public final class MsgSeqAllocator {
    private final ConcurrentHashMap<String, AtomicInteger> map = new ConcurrentHashMap<>();

    /**
     * 取该 msg_id 的下一个序号（首次为 1）。
     *
     * @param msgId 原消息 id
     * @return 下一 seq
     */
    public int next(String msgId) {
        if (msgId == null || msgId.isBlank()) {
            return 1;
        }
        return map.computeIfAbsent(msgId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 重置某 msg_id 的序号（可选，一般不用）。
     *
     * @param msgId 原消息 id
     */
    public void reset(String msgId) {
        if (msgId != null) {
            map.remove(msgId);
        }
    }
}
