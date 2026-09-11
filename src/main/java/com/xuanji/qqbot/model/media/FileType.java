package com.xuanji.qqbot.model.media;

/**
 * 上传文件类型（官方 file_type）。
 * 超软限制可能降级为文件；超硬限制报错。
 */
public enum FileType {
    /** 图片 png/jpg 等，软限 20MB */
    IMAGE(1),
    /** 视频 mp4，软限 30MB */
    VIDEO(2),
    /** 语音 silk 等，软限 20MB */
    AUDIO(3),
    /** 通用文件，软限 200MB */
    FILE(4);

    private final int code;

    FileType(int code) {
        this.code = code;
    }

    /**
     * @return 官方 file_type 数值
     */
    public int code() {
        return code;
    }
}
