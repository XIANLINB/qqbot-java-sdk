package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 发送侧消息类型（对齐官方）。
 * 0=文本 2=Markdown 3=Ark 卡片 6=输入中 7=富媒体 8=图文卡片
 */
public enum MsgType {
    /** 文本 content */
    TEXT(0),
    /** Markdown markdown */
    MARKDOWN(2),
    /** Ark 卡片 ark */
    ARK(3),
    /** 输入中 input_notify */
    INPUT_NOTIFY(6),
    /** 富媒体 media */
    MEDIA(7),
    /** 图文卡片 card */
    CARD(8);

    private final int code;

    MsgType(int code) {
        this.code = code;
    }

    /**
     * @return 官方 msg_type 数值
     */
    public int code() {
        return code;
    }

    /**
     * @return Jackson 序列化用整数
     */
    @JsonValue
    public int toValue() {
        return code;
    }

    /**
     * @param code 官方数值
     * @return 枚举
     */
    @JsonCreator
    public static MsgType from(int code) {
        for (MsgType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        throw new IllegalArgumentException("未知 msg_type: " + code);
    }
}
