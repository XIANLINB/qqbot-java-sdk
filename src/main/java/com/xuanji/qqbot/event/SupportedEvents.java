package com.xuanji.qqbot.event;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 本期 SDK 范围内的事件（单聊全部 + 群聊全部 + 互动）。
 */
public final class SupportedEvents {
    /** 单聊事件 */
    public static final Set<String> C2C = Set.of(
            EventType.C2C_MESSAGE_CREATE,
            EventType.FRIEND_ADD,
            EventType.FRIEND_DEL,
            EventType.C2C_MSG_REJECT,
            EventType.C2C_MSG_RECEIVE
    );

    /** 群聊事件 */
    public static final Set<String> GROUP = Set.of(
            EventType.GROUP_AT_MESSAGE_CREATE,
            EventType.GROUP_MESSAGE_CREATE,
            EventType.GROUP_ADD_ROBOT,
            EventType.GROUP_DEL_ROBOT,
            EventType.GROUP_MEMBER_ADD,
            EventType.GROUP_MEMBER_REMOVE,
            EventType.GROUP_MSG_RECEIVE,
            EventType.GROUP_MSG_REJECT,
            EventType.SUBSCRIBE_MESSAGE_STATUS,
            EventType.GROUP_JOIN_REQUEST
    );

    /** 互动事件 */
    public static final Set<String> INTERACTION = Set.of(
            EventType.INTERACTION_CREATE
    );

    private SupportedEvents() {
    }

    /**
     * @return 全部已支持事件名
     */
    public static Set<String> all() {
        LinkedHashSet<String> s = new LinkedHashSet<>();
        s.addAll(C2C);
        s.addAll(GROUP);
        s.addAll(INTERACTION);
        return s;
    }

    /**
     * @return WebSocket 默认 Intent 说明
     */
    public static String defaultIntentHint() {
        return "GROUP_AND_C2C_EVENT(1<<25) | INTERACTION(1<<26)";
    }
}
