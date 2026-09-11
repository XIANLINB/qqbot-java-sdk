package com.xuanji.qqbot.spring;

import com.xuanji.qqbot.Bot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多机器人注册表：名称/AppID → Bot 实例。
 * 每个机器人凭证、token 缓存、事件总线互相独立；回复自动路由到收到事件的那个 Bot。
 */
public final class BotRegistry implements AutoCloseable {
    private final Map<String, Entry> byName = new LinkedHashMap<>();
    private final Map<String, Entry> byAppId = new LinkedHashMap<>();

    /**
     * 注册条目。
     *
     * @param entry name/appId/mode/bot/intents
     */
    public synchronized void register(Entry entry) {
        byName.put(entry.name(), entry);
        byAppId.put(entry.appId(), entry);
    }

    /**
     * @param name 机器人名称
     * @return 条目，不存在为 null
     */
    public Entry get(String name) {
        return byName.get(name);
    }

    /**
     * @param appId 机器人 AppID
     * @return 条目，不存在为 null
     */
    public Entry byAppId(String appId) {
        return appId == null ? null : byAppId.get(appId);
    }

    /**
     * @return 全部条目（按配置顺序）
     */
    public List<Entry> all() {
        return new ArrayList<>(byName.values());
    }

    /**
     * @param mode websocket / webhook
     * @return 该接入方式的全部条目
     */
    public List<Entry> byMode(String mode) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : byName.values()) {
            if (e.mode().equalsIgnoreCase(mode)) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * @return 默认机器人：仅配置一个时返回它；多个返回第一个
     */
    public Entry defaultBot() {
        return byName.isEmpty() ? null : byName.values().iterator().next();
    }

    /**
     * @return 机器人名称集合
     */
    public Collection<String> names() {
        return byName.keySet();
    }

    @Override
    public void close() {
        for (Entry e : byName.values()) {
            try {
                e.bot().close();
            } catch (Exception ignored) {
                // 忽略
            }
        }
        byName.clear();
    }

    /**
     * 注册表条目。
     *
     * @param name      机器人名称
     * @param appId     AppID
     * @param mode      websocket / webhook
     * @param bot       Bot 实例
     * @param intents   WebSocket intents（webhook 模式为 0）
     * @param appSecret AppSecret（webhook 验签用）
     * @param path      Webhook 回调路径（仅 webhook 模式有意义）
     */
    public record Entry(
            String name,
            String appId,
            String mode,
            Bot bot,
            long intents,
            String appSecret,
            String path
    ) {
        /**
         * @return 日志用标识：mode/appId
         */
        public String tag() {
            return mode + "/" + appId;
        }

        /**
         * @return 是否 WebSocket 接入
         */
        public boolean isWebsocket() {
            return "websocket".equalsIgnoreCase(mode);
        }

        /**
         * @return 是否 Webhook 接入
         */
        public boolean isWebhook() {
            return "webhook".equalsIgnoreCase(mode);
        }
    }
}
