package com.xuanji.qqbot.event;

/**
 * 事件负载标记接口。
 */
public interface Event {
    /**
     * 事件信封 id（最外层 id）。用于官方 event_id 被动回复。
     *
     * @return 信封 id
     */
    String eventId();

    /**
     * @return 事件类型名，如 GROUP_AT_MESSAGE_CREATE
     */
    String type();

    /**
     * 用户消息 id（事件体 d.id）。用于官方 msg_id 被动回复。
     * 非消息事件返回 null。
     *
     * @return 消息 id
     */
    default String messageId() {
        return null;
    }

    /**
     * 群场景群 OpenID，无则 null。
     *
     * @return group_openid
     */
    default String groupOpenid() {
        return null;
    }

    /**
     * 单聊场景用户 OpenID，无则 null。
     *
     * @return user_openid
     */
    default String userOpenid() {
        return null;
    }

    /**
     * 触发本事件的群成员 OpenID（如进群操作者、按钮点击者），无则 null。
     *
     * @return member_openid
     */
    default String memberOpenid() {
        return null;
    }
}
