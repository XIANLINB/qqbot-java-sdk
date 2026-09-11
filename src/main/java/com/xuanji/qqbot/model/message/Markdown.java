package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Markdown 消息字段（msg_type=2 的 markdown）。
 * 模板 ID 已废弃，使用 content 原生语法。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Markdown(
        /** 原生 Markdown 全文 */
        @JsonProperty("content") String content,
        /** 图片转存失败是否中断发送 */
        @JsonProperty("force_verify_image_resource") Boolean forceVerifyImageResource
) {
    /**
     * @param content 原生 Markdown
     * @return Markdown 消息
     */
    public static Markdown of(String content) {
        return new Markdown(content, null);
    }
}
