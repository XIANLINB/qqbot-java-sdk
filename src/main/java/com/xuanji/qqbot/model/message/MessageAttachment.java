package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * 消息附件。对齐实测：图片 / 语音 / 视频 / 文件。
 * 类型判断同时看 {@code content_type} 与 {@code filename} 扩展名
 * （群全量里视频/文件有时 content_type 仅为 file）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageAttachment(
        /** 附件附带文本（图片等常见空串） */
        @JsonProperty("content") String content,
        /** 下载 URL */
        @JsonProperty("url") String url,
        /** 文件名 */
        @JsonProperty("filename") String filename,
        /** 图片/视频宽 */
        @JsonProperty("width") Integer width,
        /** 图片/视频高 */
        @JsonProperty("height") Integer height,
        /** 文件大小字节 */
        @JsonProperty("size") Long size,
        /** MIME 或粗类型：image/jpeg、voice、video/mp4、file */
        @JsonProperty("content_type") String contentType,
        /** 语音转 WAV URL */
        @JsonProperty("voice_wav_url") String voiceWavUrl,
        /** 语音 ASR 参考文本 */
        @JsonProperty("asr_refer_text") String asrReferText
) {
    /** 常见图片扩展名 */
    private static final String[] IMAGE_EXT = {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "jfif", "tif", "tiff"
    };
    /** 常见视频扩展名 */
    private static final String[] VIDEO_EXT = {
            "mp4", "mov", "avi", "mkv", "flv", "wmv", "webm", "m4v", "mpeg", "mpg", "3gp", "ts"
    };
    /** 常见音频/音乐扩展名 */
    private static final String[] AUDIO_EXT = {
            "mp3", "mflac", "flac", "wav", "ogg", "aac", "m4a", "wma", "amr", "ape", "opus"
    };
    /** 语音粗类型 content_type */
    private static final String VOICE_CT = "voice";

    /**
     * @return 是否语音条（content_type 含 voice，或扩展名在音频表中且有 voice_wav_url/ASR）
     */
    public boolean isVoice() {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains(VOICE_CT)) {
            return true;
        }
        // 仅当带语音特征时才当 voice，避免把 mp3 音乐卡片当语音条
        return voiceWavUrl != null && !voiceWavUrl.isBlank()
                && (asrReferText != null || extIn(AUDIO_EXT));
    }

    /**
     * @return 是否图片（jpg/png/webp/gif 等）
     */
    public boolean isImage() {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("image")) {
            return true;
        }
        return extIn(IMAGE_EXT);
    }

    /**
     * @return 是否视频（mp4/mov/mkv/webm 等常见格式）
     */
    public boolean isVideo() {
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.contains("video") || ct.contains("mp4") || ct.contains("quicktime")) {
                return true;
            }
        }
        return extIn(VIDEO_EXT);
    }

    /**
     * @return 是否音频/音乐（mp3、mflac、flac、wav 等；不含 voice 条）
     */
    public boolean isAudio() {
        if (isVoice()) {
            return false;
        }
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.startsWith("audio/") || ct.contains("mpeg") || ct.contains("flac")) {
                return true;
            }
        }
        return extIn(AUDIO_EXT);
    }

    /**
     * @return 是否通用文件节点（content_type 为 file 且不能识别为图/音/视频时）
     */
    public boolean isFile() {
        if (contentType == null) {
            return false;
        }
        if (!contentType.equalsIgnoreCase("file")) {
            return false;
        }
        // content_type=file 但扩展名能识别时，优先按具体类型
        return !isImage() && !isVideo() && !isAudio() && !isVoice();
    }

    private boolean extIn(String[] exts) {
        String name = filename;
        if (name == null || name.isBlank()) {
            return false;
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return false;
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        for (String e : exts) {
            if (e.equals(ext)) {
                return true;
            }
        }
        return false;
    }
}
