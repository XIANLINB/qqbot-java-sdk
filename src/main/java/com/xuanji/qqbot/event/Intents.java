package com.xuanji.qqbot.event;

/**
 * 事件订阅 Intent 位标记。
 * 取值对齐官方「事件订阅 Intents」；WebSocket Identify 时写入 d.intents。
 * <p>
 * 本期（单聊+群聊+互动）默认使用：
 * {@link #groupC2cAndInteraction()} = GROUP_AND_C2C_EVENT | INTERACTION。
 * Webhook 无需 intents，在开放平台勾选事件即可。
 */
public final class Intents {
    /** 频道/子频道变更（本期不用） */
    public static final long GUILDS = 1L << 0;
    /** 频道成员变更（本期不用） */
    public static final long GUILD_MEMBERS = 1L << 1;
    /** 群成员事件 GROUP_MEMBER_ADD / GROUP_MEMBER_REMOVE（官方 1&lt;&lt;24） */
    public static final long GROUP_MEMBER_EVENT = 1L << 24;
    /** 频道全量消息（本期不用） */
    public static final long GUILD_MESSAGES = 1L << 9;
    /** 表情表态（本期不用） */
    public static final long GUILD_MESSAGE_REACTIONS = 1L << 10;
    /** 频道私信（本期不用） */
    public static final long DIRECT_MESSAGE = 1L << 12;
    /**
     * 群聊与单聊事件（官方 1&lt;&lt;25）。
     * 含：C2C_MESSAGE_CREATE、FRIEND_*、C2C_MSG_*、GROUP_AT_MESSAGE_CREATE、
     * GROUP_MESSAGE_CREATE（需权限）、GROUP_ADD/DEL_ROBOT、GROUP_MSG_*、
     * GROUP_MEMBER_*、GROUP_JOIN_REQUEST、SUBSCRIBE_MESSAGE_STATUS 等。
     */
    public static final long GROUP_AND_C2C_EVENT = 1L << 25;
    /** 互动事件 INTERACTION_CREATE（官方 1&lt;&lt;26） */
    public static final long INTERACTION = 1L << 26;
    /** 消息审核（本期不用） */
    public static final long MESSAGE_AUDIT = 1L << 27;
    /** 论坛（本期不用） */
    public static final long FORUMS_EVENT = 1L << 28;
    /** 音频（本期不用） */
    public static final long AUDIO_ACTION = 1L << 29;
    /** 频道公域消息（本期不用） */
    public static final long PUBLIC_GUILD_MESSAGES = 1L << 30;

    private Intents() {
    }

    /**
     * 按位或拼接。
     *
     * @param bits 若干 intent
     * @return 组合值
     */
    public static long of(long... bits) {
        long v = 0;
        for (long b : bits) {
            v |= b;
        }
        return v;
    }

    /**
     * 仅 GROUP_AND_C2C_EVENT。
     *
     * @return intent
     */
    public static long groupAndC2c() {
        return GROUP_AND_C2C_EVENT;
    }

    /**
     * 本期默认：群/单聊 + 互动。对应官方文档中 GROUP_AND_C2C_EVENT 与 INTERACTION 两位。
     *
     * @return intents
     */
    public static long groupC2cAndInteraction() {
        return of(GROUP_AND_C2C_EVENT, GROUP_MEMBER_EVENT, INTERACTION);
    }
}
