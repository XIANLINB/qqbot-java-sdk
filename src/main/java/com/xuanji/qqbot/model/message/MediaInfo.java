package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 富媒体引用（发送消息时的 media 字段）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaInfo(
        /** 上传接口返回的 file_info，有时效（ttl） */
        @JsonProperty("file_info") String fileInfo
) {
    /**
     * @param fileInfo 上传返回的 file_info
     * @return MediaInfo
     */
    public static MediaInfo of(String fileInfo) {
        return new MediaInfo(fileInfo);
    }
}
