package com.xuanji.qqbot.model.media;

/**
 * 富媒体上传场景。
 * 单聊与群聊 files / upload_prepare / upload_part_finish 端点隔离，不可混用。
 */
public enum Scope {
    /** 单聊 /v2/users */
    C2C,
    /** 群聊 /v2/groups */
    GROUP
}
