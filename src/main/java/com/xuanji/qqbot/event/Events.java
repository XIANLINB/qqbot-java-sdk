package com.xuanji.qqbot.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.xuanji.qqbot.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * 轻量事件总线：不扫描注解，只显式注册。
 * 支持按事件类型字符串或具体 Class 订阅；Webhook/WS 均通过 {@link #dispatchEnvelope} 注入。
 */
public final class Events {
    private static final Logger log = LoggerFactory.getLogger(Events.class);

    private final Executor executor;
    private final List<Consumer<? super Event>> anyListeners = new CopyOnWriteArrayList<>();
    private final Map<String, List<Consumer<? super Event>>> byType = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Consumer<? super Event>>> byClass = new ConcurrentHashMap<>();
    private final EventDeduplicator deduplicator = new EventDeduplicator();

    /**
     * @param executor 事件回调线程池；null 表示调用线程同步执行
     */
    public Events(Executor executor) {
        this.executor = executor;
    }

    /**
     * 按官方事件名订阅。
     *
     * @param eventType 如 EventType.GROUP_AT_MESSAGE_CREATE
     * @param handler   回调
     * @return this
     */
    public Events on(String eventType, Consumer<? super Event> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");
        byType.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
        return this;
    }

    /**
     * 按具体事件 Class 订阅（类型安全）。
     *
     * @param type    事件类，如 GroupAtMessageCreate.class
     * @param handler 回调
     * @param <T>     事件类型
     * @return this
     */
    public <T extends Event> Events on(Class<T> type, Consumer<? super T> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        @SuppressWarnings("unchecked")
        Consumer<? super Event> erased = (Consumer<? super Event>) handler;
        byClass.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(erased);
        return this;
    }

    /**
     * 订阅所有已解析事件。
     *
     * @param handler 回调
     * @return this
     */
    public Events onAny(Consumer<? super Event> handler) {
        anyListeners.add(Objects.requireNonNull(handler));
        return this;
    }

    /**
     * 分发已解析事件。
     *
     * @param event 事件对象
     */
    public void dispatch(Event event) {
        if (event == null) {
            return;
        }
        Runnable task = () -> {
            for (Consumer<? super Event> c : anyListeners) {
                runSafely(c, event);
            }
            for (Consumer<? super Event> c : byType.getOrDefault(event.type(), List.of())) {
                runSafely(c, event);
            }
            for (Map.Entry<Class<?>, List<Consumer<? super Event>>> e : byClass.entrySet()) {
                if (e.getKey().isInstance(event)) {
                    for (Consumer<? super Event> c : e.getValue()) {
                        runSafely(c, event);
                    }
                }
            }
        };
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
    }

    /**
     * 解析网关/Webhook 信封并分发。
     *
     * @param type       事件名 t
     * @param d          事件体 d
     * @param envelopeId 信封 id（作为 event_id 被动回复）
     */
    public void dispatchEnvelope(String type, JsonNode d, String envelopeId) {
        Event event = parse(type, d, envelopeId);
        if (event == null) {
            return;
        }
        if (deduplicator.isDuplicate(event)) {
            log.info("[xuanji] 丢弃重复事件 type={} msgId={} eventId={}",
                    event.type(), event.messageId(), event.eventId());
            return;
        }
        dispatch(event);
    }

    /**
     * 将官方事件体反序列化为 SDK 事件对象。
     *
     * @param type       事件名
     * @param d          事件 JSON
     * @param envelopeId 信封 id
     * @return 事件或 null（未支持类型）
     */
    public static Event parse(String type, JsonNode d, String envelopeId) {
        if (type == null || d == null || d.isNull()) {
            return null;
        }
        try {
            Event event = switch (type) {
                case EventType.C2C_MESSAGE_CREATE -> withEnvelope(
                        Json.mapper().treeToValue(d, C2cMessageCreate.class), envelopeId, type);
                case EventType.GROUP_AT_MESSAGE_CREATE -> withEnvelope(
                        Json.mapper().treeToValue(d, GroupAtMessageCreate.class), envelopeId, type);
                case EventType.GROUP_MESSAGE_CREATE -> withEnvelope(
                        Json.mapper().treeToValue(d, GroupMessageCreate.class), envelopeId, type);
                case EventType.GROUP_ADD_ROBOT -> withEnvelope(
                        Json.mapper().treeToValue(d, GroupAddRobot.class), envelopeId, type);
                case EventType.GROUP_DEL_ROBOT -> withEnvelope(
                        Json.mapper().treeToValue(d, GroupDelRobot.class), envelopeId, type);
                case EventType.FRIEND_ADD -> withEnvelope(
                        Json.mapper().treeToValue(d, FriendAdd.class), envelopeId, type);
                case EventType.FRIEND_DEL -> withEnvelope(
                        Json.mapper().treeToValue(d, FriendDel.class), envelopeId, type);
                case EventType.C2C_MSG_REJECT -> withEnvelope(
                        Json.mapper().treeToValue(d, C2cMsgReject.class), envelopeId, type);
                case EventType.C2C_MSG_RECEIVE -> withEnvelope(
                        Json.mapper().treeToValue(d, C2cMsgReceive.class), envelopeId, type);
                case EventType.GROUP_MSG_REJECT -> withEnvelope(
                        Json.mapper().treeToValue(d, GroupMsgReject.class), envelopeId, type);
                case EventType.GROUP_MSG_RECEIVE -> withEnvelope(
                        Json.mapper().treeToValue(d, GroupMsgReceive.class), envelopeId, type);
                case EventType.GROUP_MEMBER_ADD -> withEnvelope(
                        Json.mapper().treeToValue(d, GroupMemberAdd.class), envelopeId, type);
                case EventType.GROUP_MEMBER_REMOVE -> withEnvelope(
                        Json.mapper().treeToValue(d, GroupMemberRemove.class), envelopeId, type);
                case EventType.GROUP_JOIN_REQUEST -> withEnvelope(
                        Json.mapper().treeToValue(d, GroupJoinRequest.class), envelopeId, type);
                case EventType.SUBSCRIBE_MESSAGE_STATUS -> withEnvelope(
                        Json.mapper().treeToValue(d, SubscribeMessageStatus.class), envelopeId, type);
                case EventType.INTERACTION_CREATE -> withEnvelope(
                        Json.mapper().treeToValue(d, InteractionCreate.class), envelopeId, type);
                default -> {
                if (isChannelEventType(type)) {
                    log.info("[xuanji] 频道相关事件（本期不实现）: {}", type);
                } else {
                    log.info("[xuanji] 未知事件类型: {}", type);
                }
                yield null;
            }
            };
            // default 分支已打日志
            return event;
        } catch (Exception e) {
            log.warn("解析事件 {} 失败: {}", type, e.getMessage());
            return null;
        }
    }

    private static boolean isChannelEventType(String type) {
        if (type == null) {
            return false;
        }
        return type.startsWith("GUILD")
                || type.startsWith("CHANNEL")
                || type.startsWith("AT_MESSAGE")
                || type.startsWith("PUBLIC_MESSAGE")
                || type.contains("GUILD_MEMBER")
                || type.contains("FORUM")
                || type.contains("REACTION");
    }

    private static Event withEnvelope(Event e, String envelopeId, String type) {
        if (e instanceof C2cMessageCreate c2c) {
            return new C2cMessageCreate(c2c.id(), c2c.author(), c2c.content(), c2c.timestamp(),
                    c2c.messageType(), c2c.messageScene(), c2c.attachments(), c2c.arkData(),
                    c2c.msgElements(), envelopeId);
        }
        if (e instanceof GroupAtMessageCreate g) {
            return new GroupAtMessageCreate(g.id(), g.author(), g.content(), g.groupId(),
                    g.groupOpenid(), g.timestamp(), g.messageType(), g.messageScene(), g.attachments(),
                    g.mentions(), g.arkData(), g.msgElements(), envelopeId);
        }
        if (e instanceof GroupMessageCreate g) {
            return new GroupMessageCreate(g.id(), g.author(), g.content(), g.groupId(),
                    g.groupOpenid(), g.timestamp(), g.messageType(), g.messageScene(), g.attachments(),
                    g.mentions(), g.arkData(), g.msgElements(), envelopeId);
        }
        if (e instanceof InteractionCreate i) {
            return new InteractionCreate(i.id(), i.interactionType(), i.scene(), i.chatType(),
                    i.timestamp(), i.guildId(), i.channelId(), i.userOpenid(),
                    i.groupOpenid(), i.groupMemberOpenid(), i.data(), i.version(),
                    i.applicationId(), envelopeId);
        }
        if (e instanceof GroupAddRobot g) {
            return new GroupAddRobot(g.groupOpenid(), g.groupId(), g.opMemberOpenid(), g.timestamp(), envelopeId);
        }
        if (e instanceof GroupDelRobot g) {
            return new GroupDelRobot(g.groupOpenid(), g.groupId(), g.opMemberOpenid(), g.timestamp(), envelopeId);
        }
        if (e instanceof FriendAdd f) {
            return new FriendAdd(f.openid(), f.timestamp(), f.author(), envelopeId);
        }
        if (e instanceof FriendDel f) {
            return new FriendDel(f.openid(), f.timestamp(), f.author(), envelopeId);
        }
        if (e instanceof C2cMsgReject c) {
            return new C2cMsgReject(c.id(), c.author(), c.timestamp(), envelopeId);
        }
        if (e instanceof C2cMsgReceive c) {
            return new C2cMsgReceive(c.id(), c.author(), c.timestamp(), envelopeId);
        }
        if (e instanceof GroupMsgReject g) {
            return new GroupMsgReject(g.id(), g.groupId(), g.groupOpenid(), g.author(), g.timestamp(), envelopeId);
        }
        if (e instanceof GroupMsgReceive g) {
            return new GroupMsgReceive(g.id(), g.groupId(), g.groupOpenid(), g.author(), g.timestamp(), envelopeId);
        }
        if (e instanceof GroupMemberAdd g) {
            return new GroupMemberAdd(g.groupOpenid(), g.memberOpenid(), g.timestamp(), envelopeId);
        }
        if (e instanceof GroupMemberRemove g) {
            return new GroupMemberRemove(g.groupOpenid(), g.memberOpenid(), g.timestamp(), envelopeId);
        }
        if (e instanceof GroupJoinRequest g) {
            return new GroupJoinRequest(g.applyAt(), g.applySource(), g.groupOpenid(),
                    g.joinRequestId(), g.memberOpenid(), g.username(), g.verifyInfo(),
                    g.invitedBy(), g.autoApproved(), g.bot(), envelopeId);
        }
        if (e instanceof SubscribeMessageStatus s) {
            return new SubscribeMessageStatus(s.id(), s.status(), s.groupId(), s.groupOpenid(),
                    s.author(), s.timestamp(), s.data(), envelopeId);
        }
        return e;
    }

    private static void runSafely(Consumer<? super Event> c, Event event) {
        try {
            c.accept(event);
        } catch (Exception ex) {
            log.error("事件处理失败 {}: {}", event.type(), ex.getMessage(), ex);
        }
    }
}
