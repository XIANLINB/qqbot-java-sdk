package com.xuanji.qqbot.event;

/**
 * 已知事件类型名（payload 字段 t）。
 * 本期范围：单聊全部 + 群聊全部 + 互动；不含频道。
 */
public final class EventType {
    /** 网关就绪 */
    public static final String READY = "READY";
    /** 会话恢复完成 */
    public static final String RESUMED = "RESUMED";

    // ---- 单聊 C2C ----
    /** 单聊消息 */
    public static final String C2C_MESSAGE_CREATE = "C2C_MESSAGE_CREATE";
    /** 用户添加机器人 */
    public static final String FRIEND_ADD = "FRIEND_ADD";
    /** 用户删除机器人 */
    public static final String FRIEND_DEL = "FRIEND_DEL";
    /** 用户关闭单聊主动消息推送 */
    public static final String C2C_MSG_REJECT = "C2C_MSG_REJECT";
    /** 用户打开单聊主动消息推送 */
    public static final String C2C_MSG_RECEIVE = "C2C_MSG_RECEIVE";

    // ---- 群聊 ----
    /** 群内 @ 机器人消息 */
    public static final String GROUP_AT_MESSAGE_CREATE = "GROUP_AT_MESSAGE_CREATE";
    /** 群全量消息（需权限） */
    public static final String GROUP_MESSAGE_CREATE = "GROUP_MESSAGE_CREATE";
    /** 机器人被添加到群 */
    public static final String GROUP_ADD_ROBOT = "GROUP_ADD_ROBOT";
    /** 机器人被移出群 */
    public static final String GROUP_DEL_ROBOT = "GROUP_DEL_ROBOT";
    /** 群成员加入 */
    public static final String GROUP_MEMBER_ADD = "GROUP_MEMBER_ADD";
    /** 群成员移除 */
    public static final String GROUP_MEMBER_REMOVE = "GROUP_MEMBER_REMOVE";
    /** 群管理员打开推送 */
    public static final String GROUP_MSG_RECEIVE = "GROUP_MSG_RECEIVE";
    /** 群管理员关闭推送 */
    public static final String GROUP_MSG_REJECT = "GROUP_MSG_REJECT";
    /** 订阅消息授权状态变更 */
    public static final String SUBSCRIBE_MESSAGE_STATUS = "SUBSCRIBE_MESSAGE_STATUS";
    /** 用户申请加群 */
    public static final String GROUP_JOIN_REQUEST = "GROUP_JOIN_REQUEST";
    /** 按钮等互动 */
    public static final String INTERACTION_CREATE = "INTERACTION_CREATE";

    // ---- 频道（本期不实现，仅保留名）----
    /** 频道公域 @ */
    public static final String AT_MESSAGE_CREATE = "AT_MESSAGE_CREATE";
    /** 频道全量消息 */
    public static final String MESSAGE_CREATE = "MESSAGE_CREATE";

    private EventType() {
    }
}
