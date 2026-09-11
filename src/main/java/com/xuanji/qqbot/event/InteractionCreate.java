package com.xuanji.qqbot.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * INTERACTION_CREATE：按钮/快捷菜单/反馈/授权等互动。
 * 对齐官方字段；仅 type=11/12 需调用 PUT /interactions/{id} 回应。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InteractionCreate(
        /** 互动 id，用于互动响应与被动回复 event_id */
        @JsonProperty("id") String id,
        /** 11=按钮 12=快捷菜单 13=反馈 14=清空会话 15=故事集 16=切模型 18=用户授权 19=群授权 20=群授权状态 */
        @JsonProperty("type") Integer interactionType,
        /** c2c / group / guild */
        @JsonProperty("scene") String scene,
        /** 0=频道 1=群聊 2=单聊 */
        @JsonProperty("chat_type") Integer chatType,
        @JsonProperty("timestamp") String timestamp,
        /** 仅频道 */
        @JsonProperty("guild_id") String guildId,
        @JsonProperty("channel_id") String channelId,
        /** 仅单聊 */
        @JsonProperty("user_openid") String userOpenid,
        /** 仅群聊 */
        @JsonProperty("group_openid") String groupOpenid,
        /** 仅群聊 */
        @JsonProperty("group_member_openid") String groupMemberOpenid,
        @JsonProperty("data") InteractionData data,
        @JsonProperty("version") Integer version,
        @JsonProperty("application_id") String applicationId,
        @JsonIgnore String eventEnvelopeId
) implements Event {
    /** 消息按钮回调 */
    public static final int TYPE_INLINE_KEYBOARD = 11;
    /** 单聊快捷菜单 */
    public static final int TYPE_CALLBACK_COMMAND = 12;

    @Override
    public String eventId() {
        return eventEnvelopeId != null ? eventEnvelopeId : id;
    }

    @Override
    public String type() {
        return EventType.INTERACTION_CREATE;
    }

    @Override
    public String userOpenid() {
        return userOpenid;
    }

    @Override
    public String groupOpenid() {
        return groupOpenid;
    }

    /**
     * @return 是否需要响应（type=11/12）
     */
    public boolean needRespond() {
        return interactionType != null
                && (interactionType == TYPE_INLINE_KEYBOARD || interactionType == TYPE_CALLBACK_COMMAND);
    }

    /**
     * @return 是否群聊场景
     */
    public boolean isGroup() {
        return "group".equalsIgnoreCase(scene) || Integer.valueOf(1).equals(chatType);
    }

    /**
     * @return 是否单聊场景
     */
    public boolean isC2c() {
        return "c2c".equalsIgnoreCase(scene) || Integer.valueOf(2).equals(chatType);
    }

    /**
     * @return 按钮 data（type=11）
     */
    public String buttonData() {
        return data == null || data.resolved() == null ? null : data.resolved().buttonData();
    }

    /**
     * @return 按钮 id（type=11）
     */
    public String buttonId() {
        return data == null || data.resolved() == null ? null : data.resolved().buttonId();
    }

    /**
     * 互动数据。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InteractionData(
            @JsonProperty("type") Integer type,
            @JsonProperty("resolved") Resolved resolved
    ) {
    }

    /**
     * 解析后数据。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resolved(
            @JsonProperty("button_data") String buttonData,
            @JsonProperty("button_id") String buttonId,
            @JsonProperty("user_id") String userId,
            @JsonProperty("feature_id") String featureId,
            @JsonProperty("message_id") String messageId,
            @JsonProperty("feedback_opt") String feedbackOpt,
            @JsonProperty("checked") Integer checked,
            @JsonProperty("action") String action
    ) {
    }
}
