package com.xuanji.qqbot.model.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 富媒体预上传响应（upload_prepare）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UploadPrepare(
        /** 上传任务 ID，合并与 part_finish 时回传 */
        @JsonProperty("upload_id") String uploadId,
        /** 分块大小（字节字符串，默认约 5MB） */
        @JsonProperty("block_size") String blockSize,
        /** 分片列表（含预签名 URL） */
        @JsonProperty("parts") List<UploadPart> parts,
        /** 上传行为配置 */
        @JsonProperty("upload_config") UploadConfig uploadConfig
) {
    /**
     * @return block_size 解析为 long，失败用 5MB 默认
     */
    public long blockSizeBytes() {
        return parseLong(blockSize, 5L * 1024 * 1024);
    }

    private static long parseLong(String v, long def) {
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * 单个分片。
     *
     * @param index 分片序号从 0
     * @param presignedUrl PUT 目标
     * @param blockSize 本片大小
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UploadPart(
            @JsonProperty("index") Integer index,
            @JsonProperty("presigned_url") String presignedUrl,
            @JsonProperty("block_size") String blockSize
    ) {
    }

    /**
     * 上传配置。
     *
     * @param concurrency 并发数
     * @param retryTimeout 重试超时秒
     * @param retryDelay 重试延迟秒
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UploadConfig(
            @JsonProperty("concurrency") Integer concurrency,
            @JsonProperty("retry_timeout") Integer retryTimeout,
            @JsonProperty("retry_delay") Integer retryDelay
    ) {
        /**
         * @return 并发数，默认 1，上限 8
         */
        public int concurrencyOrDefault() {
            return concurrency == null || concurrency < 1 ? 1 : Math.min(concurrency, 8);
        }
    }
}
