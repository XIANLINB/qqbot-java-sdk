package com.xuanji.qqbot.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.xuanji.qqbot.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
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
    /** 日志与事件来源标识，如 websocket/1905134745/落落 */
    private volatile String botTag = "";
    /** 是否打印原始报文（yml: xuanji.debug.raw-payload） */
    private volatile boolean logRawPayload = false;

    /**
     * @param executor 事件回调线程池；null 表示调用线程同步执行
     */
    public Events(Executor executor) {
        this.executor = executor;
    }

    /**
     * 设置机器人标识（方式/appId/名称），用于事件日志前缀。
     *
     * @param tag 如 websocket/1905134745/落落
     */
    public void setBotTag(String tag) {
        this.botTag = tag == null ? "" : tag;
    }

    /**
     * @return 当前机器人标识
     */
    public String botTag() {
        return botTag;
    }

    /**
     * 是否打印原始报文（WS 与 Webhook 统一在此输出）。
     *
     * @param enabled true 打印
     */
    public void setLogRawPayload(boolean enabled) {
        this.logRawPayload = enabled;
    }

    /**
     * @return 当前是否打印原始报文（OUT 侧 ApiCall 同步读取此开关）
     */
    public boolean logRawPayload() {
        return logRawPayload;
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
        dispatchEnvelope(type, d, envelopeId, null);
    }

    /**
     * 解析网关/Webhook 信封并分发（统一入口，WS 与 Webhook 都走这里）。
     * <p>
     * 日志格式：[IN][方式/appId/名称][事件中文名][群ID=..][成员ID=..][用户ID=..][消息ID=..][事件ID=..][类型=..][内容=..][原始报文=..]
     * 原始报文受 {@code logRawPayload} 开关控制；READY/RESUMED 不附原始报文。
     *
     * @param type        事件名 t
     * @param d           事件体 d
     * @param envelopeId  信封 id（作为 event_id 被动回复）
     * @param rawEnvelope 完整信封 JSON（可为 null）
     */
    public void dispatchEnvelope(String type, JsonNode d, String envelopeId, JsonNode rawEnvelope) {
        boolean isSession = EventType.READY.equals(type) || EventType.RESUMED.equals(type);
        String rawSuffix = "";
        if (logRawPayload && rawEnvelope != null && !isSession) {
            rawSuffix = " [原始报文=" + compact(rawEnvelope) + "]";
        }
        Event event = parse(type, d, envelopeId);

        String name = nameZh(type);
        StringBuilder kv = new StringBuilder();
        if (event != null) {
            appendKv(kv, "群ID", event.groupOpenid());
            appendKv(kv, "成员ID", event.memberOpenid());
            appendKv(kv, "用户ID", event.userOpenid());
            appendKv(kv, "消息ID", event.messageId());
            if (envelopeId != null) {
                appendKv(kv, "事件ID", envelopeId);
            }
        }

        if (event == null) {
            if (name != null) {
                // READY / RESUMED 等会话级
                log.info("[IN][{}][{}]{}", botTag, name, rawSuffix);
            } else if (isChannelEventType(type)) {
                log.info("[IN][{}][频道事件-未实现:{}]{}", botTag, type, rawSuffix);
            } else if (type != null) {
                log.info("[IN][{}][未知事件:{}]{}", botTag, type, rawSuffix);
            }
            return;
        }
        if (deduplicator.isDuplicate(event)) {
            log.info("[IN][{}][{}][重复丢弃]{}{}",
                    botTag, name, kv, rawSuffix);
            return;
        }
        log.info("[IN][{}][{}]{}{}{}", botTag, name, kv, typePreview(event), rawSuffix);
        dispatch(event);
    }

    /** 事件名 → 中文名。 */
    private static String nameZh(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case EventType.READY -> "会话就绪";
            case EventType.RESUMED -> "会话恢复";
            case EventType.C2C_MESSAGE_CREATE -> "单聊消息";
            case EventType.GROUP_AT_MESSAGE_CREATE -> "群聊艾特消息";
            case EventType.GROUP_MESSAGE_CREATE -> "群聊全量消息";
            case EventType.GROUP_ADD_ROBOT -> "机器人进群";
            case EventType.GROUP_DEL_ROBOT -> "机器人退群";
            case EventType.GROUP_MEMBER_ADD -> "群成员加入";
            case EventType.GROUP_MEMBER_REMOVE -> "群成员移除";
            case EventType.GROUP_MSG_RECEIVE -> "群推送开启";
            case EventType.GROUP_MSG_REJECT -> "群推送关闭";
            case EventType.FRIEND_ADD -> "添加好友";
            case EventType.FRIEND_DEL -> "删除好友";
            case EventType.C2C_MSG_RECEIVE -> "单聊推送开启";
            case EventType.C2C_MSG_REJECT -> "单聊推送关闭";
            case EventType.GROUP_JOIN_REQUEST -> "入群申请";
            case EventType.SUBSCRIBE_MESSAGE_STATUS -> "订阅授权变更";
            case EventType.INTERACTION_CREATE -> "互动事件";
            default -> null;
        };
    }

    /** 消息内容类型与预览，如 [文本][内容=你好]。 */
    private static String typePreview(Event e) {
        if (e instanceof C2cMessageCreate c) {
            return messagePreview(c.messageType(), c.attachments(), c.content());
        }
        if (e instanceof GroupMessageCreate g) {
            return messagePreview(g.messageType(), g.attachments(), g.content());
        }
        if (e instanceof GroupAtMessageCreate g) {
            return messagePreview(g.messageType(), g.attachments(), g.content());
        }
        if (e instanceof InteractionCreate) {
            return "[按钮/菜单]";
        }
        return "";
    }

    private static String messagePreview(Integer messageType,
                                          java.util.List<com.xuanji.qqbot.model.message.MessageAttachment> attachments,
                                          String content) {
        String type = "文本";
        if (attachments != null && !attachments.isEmpty()) {
            var a = attachments.get(0);
            type = a.isImage() ? "图片" : a.isVoice() ? "语音" : a.isVideo() ? "视频" : "文件";
        } else if (messageType != null && messageType == 3) {
            type = "卡片";
        }
        if (content == null || content.isBlank()) {
            return "[" + type + "]";
        }
        String prev = content.length() > 50 ? content.substring(0, 50) + "…" : content;
        return "[" + type + "][内容=" + prev.replace("\n", "\\n") + "]";
    }

    private static void appendKv(StringBuilder sb, String key, String value) {
        if (value != null && !value.isBlank()) {
            sb.append('[').append(key).append('=').append(value).append(']');
        }
    }

    private static String compact(JsonNode node) {
        try {
            return Json.mapper().writeValueAsString(node);
        } catch (Exception e) {
            return String.valueOf(node);
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
                || type.contains("FORUM")
                || type.contains("REACTION");
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
                default -> null;
            };
            return event;
        } catch (Exception e) {
            log.warn("解析事件 {} 失败: {}", type, e.getMessage());
            return null;
        }
    }

    /**
     * 将「为每种事件注入信封 id」的重复 copy 构造收敛到一处。
     * <p>
     * 每个事件 record 都以 {@code @JsonIgnore String eventEnvelopeId} 作为信封字段，
     * 这里用 record 反射复制出一份、仅替换该字段，新增事件类型时无需再补 copy 分支；
     * 传入 null 信封、或非 record、或不带信封字段的事件，均原样返回。
     * 返回对象仍是原具体类型，订阅侧 {@code on(Class<T>)} 的公开 API 不受影响。
     *
     * @param e          已反序列化的事件
     * @param envelopeId 信封 id（作为 event_id 被动回复）
     * @param type       事件名（仅用于日志）
     * @return 注入信封后的事件
     */
    private static Event withEnvelope(Event e, String envelopeId, String type) {
        if (e == null || envelopeId == null) {
            return e;
        }
        Class<?> clazz = e.getClass();
        if (!clazz.isRecord()) {
            return e;
        }
        RecordComponent[] comps = clazz.getRecordComponents();
        int idx = -1;
        for (int i = 0; i < comps.length; i++) {
            if ("eventEnvelopeId".equals(comps[i].getName())) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return e;
        }
        Object[] args = new Object[comps.length];
        try {
            for (int i = 0; i < comps.length; i++) {
                args[i] = comps[i].getAccessor().invoke(e);
            }
            args[idx] = envelopeId;
            Class<?>[] types = new Class[comps.length];
            for (int i = 0; i < comps.length; i++) {
                types[i] = comps[i].getType();
            }
            Constructor<?> ctor = clazz.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            return (Event) ctor.newInstance(args);
        } catch (ReflectiveOperationException ex) {
            log.warn("事件 {} 注入信封失败，按原样分发: {}", type, ex.getMessage());
            return e;
        }
    }

    private static void runSafely(Consumer<? super Event> c, Event event) {
        try {
            c.accept(event);
        } catch (Exception ex) {
            log.error("事件处理失败 {}: {}", event.type(), ex.getMessage(), ex);
        }
    }
}
