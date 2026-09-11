package com.xuanji.qqbot.model.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 富媒体上传接口返回。
 * file_info 有时效 ttl，过期需重新上传。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FileInfo(
        /** 用于发消息 media.file_info 的透传串 */
        @JsonProperty("file_info") String fileInfo,
        /** 有效期秒；0 表示可长期使用 */
        @JsonProperty("ttl") Long ttl,
        /** srv_send_msg=true 时的消息 id */
        @JsonProperty("id") String id
) {
}
