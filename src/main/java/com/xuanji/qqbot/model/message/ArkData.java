package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * ARK 结构化卡片（实测 message_type=3，如 QQ 音乐分享）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArkData(
        /** 操作提示，如 [分享]恨幸福来过 */
        @JsonProperty("prompt") String prompt,
        /** 卡片类型：tuwen/feed/miniapp/map/contact_card/video_share/music_together */
        @JsonProperty("ark_type") String arkType,
        /** 类型中文名，如 图文H5 */
        @JsonProperty("ark_name") String arkName,
        /** 字段：title/desc/tag/jump_url/preview/source 等 */
        @JsonProperty("fields") Map<String, Object> fields
) {
    /**
     * @return fields 中 title
     */
    public String title() {
        Object v = fields == null ? null : fields.get("title");
        return v == null ? null : String.valueOf(v);
    }

    /**
     * @return fields 中 desc
     */
    public String desc() {
        Object v = fields == null ? null : fields.get("desc");
        return v == null ? null : String.valueOf(v);
    }

    /**
     * @return fields 中 jump_url
     */
    public String jumpUrl() {
        Object v = fields == null ? null : fields.get("jump_url");
        return v == null ? null : String.valueOf(v);
    }
}
