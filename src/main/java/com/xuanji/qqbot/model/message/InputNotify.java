package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 「对方正在输入」状态（仅单聊，msg_type=6）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InputNotify(
        /** 输入类型，官方示例为 1 */
        @JsonProperty("input_type") Integer inputType,
        /** 状态持续秒数，最长 60 */
        @JsonProperty("input_second") Integer inputSecond
) {
    /**
     * 构造输入中状态。
     *
     * @param seconds 持续秒数，超过 60 会截断
     * @return InputNotify
     */
    public static InputNotify typing(int seconds) {
        return new InputNotify(1, Math.min(seconds, 60));
    }
}
