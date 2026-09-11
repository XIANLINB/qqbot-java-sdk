package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 消息场景上下文（官方 message_scene）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageScene(
        /** 场景来源，如 default */
        @JsonProperty("source") String source,
        /** 扩展 kv 列表，如 msg_idx=、auth_token= */
        @JsonProperty("ext") List<String> ext
) {
    /**
     * 从 ext 中取 key=value 的 value。
     *
     * @param key 键名，如 msg_idx
     * @return 值，不存在为 null
     */
    public String extValue(String key) {
        if (ext == null || key == null) {
            return null;
        }
        String prefix = key + "=";
        for (String item : ext) {
            if (item != null && item.startsWith(prefix)) {
                return item.substring(prefix.length());
            }
        }
        return null;
    }

    /**
     * @return 消息索引 msg_idx
     */
    public String msgIdx() {
        return extValue("msg_idx");
    }

    /**
     * @return 被引用消息索引 ref_msg_idx
     */
    public String refMsgIdx() {
        return extValue("ref_msg_idx");
    }
}
