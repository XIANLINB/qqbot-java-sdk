package com.xuanji.qqbot.event;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件去重。
 * <p>
 * 官方说明：为确保消息可达，同一消息可能被重复推送。
 * 去重键优先用 {@code messageId}（d.id），其次用信封 {@code eventId}。
 * 采用带过期时间的内存缓存，避免长跑内存无限增长。
 */
public final class EventDeduplicator {
    private final ConcurrentHashMap<String, Long> seen = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final int maxEntries;

    /**
     * @param ttlMillis  记录保留时长（毫秒），默认 10 分钟
     * @param maxEntries 最大缓存条数，超出按插入序淘汰约 1/4
     */
    public EventDeduplicator(long ttlMillis, int maxEntries) {
        this.ttlMillis = Math.max(ttlMillis, 1000L);
        this.maxEntries = Math.max(maxEntries, 64);
    }

    public EventDeduplicator() {
        this(10 * 60 * 1000L, 4096);
    }

    /**
     * 若已处理过则返回 true（应丢弃）；否则记录并返回 false。
     *
     * @param event 事件
     * @return true=重复
     */
    public boolean isDuplicate(Event event) {
        if (event == null) {
            return false;
        }
        String key = keyOf(event);
        if (key == null || key.isBlank()) {
            return false;
        }
        purgeIfFull();
        long now = System.currentTimeMillis();
        Long prev = seen.putIfAbsent(key, now);
        if (prev == null) {
            return false;
        }
        if (now - prev > ttlMillis) {
            // 过期则视为新消息，覆盖
            seen.put(key, now);
            return false;
        }
        return true;
    }

    private String keyOf(Event event) {
        String msgId = event.messageId();
        if (msgId != null && !msgId.isBlank()) {
            return "m:" + msgId;
        }
        String eventId = event.eventId();
        if (eventId != null && !eventId.isBlank()) {
            return "e:" + eventId;
        }
        return null;
    }

    private void purgeIfFull() {
        if (seen.size() < maxEntries) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = seen.entrySet().iterator();
        while (it.hasNext() && seen.size() > maxEntries * 3 / 4) {
            Map.Entry<String, Long> e = it.next();
            if (now - e.getValue() > ttlMillis) {
                it.remove();
            }
        }
        // 仍满则再删最旧一批
        if (seen.size() >= maxEntries) {
            seen.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(maxEntries / 4)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(seen::remove);
        }
    }
}
