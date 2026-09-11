package com.xuanji.qqbot.api;

import com.xuanji.qqbot.http.ApiCall;
import com.xuanji.qqbot.model.message.MessageIdResult;
import com.xuanji.qqbot.model.message.PostMessage;
import com.xuanji.qqbot.model.message.Reply;
import com.xuanji.qqbot.model.message.StreamMessage;
import com.xuanji.qqbot.model.message.StreamMessageResult;
import com.xuanji.qqbot.model.media.FileInfo;
import com.xuanji.qqbot.model.media.FileType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 单聊 / 群聊消息 OpenAPI。
 * <p>
 * 路径与字段对齐官方：
 * <ul>
 *   <li>POST /v2/users/{user_openid}/messages</li>
 *   <li>POST /v2/groups/{group_openid}/messages</li>
 *   <li>POST /v2/users/{user_openid}/stream_messages</li>
 *   <li>DELETE .../messages/{message_id}</li>
 *   <li>POST .../files （URL 上传；本地/分片见 {@link MediaApi}）</li>
 * </ul>
 * 被动回复请携带事件中的 msg_id；单聊被动有效约 60 分钟/4 次，群聊约 5 分钟/5 次。
 */
public final class MessageApi {
    private final ApiCall api;

    /**
     * @param api 已注入 token 的调用器
     */
    public MessageApi(ApiCall api) {
        this.api = api;
    }

    // ---- 单聊（C2C）----

    /**
     * 发送单聊消息。
     *
     * @param userOpenid 用户 OpenID（单聊场景，来自事件 author.user_openid）
     * @param message    消息体（文本/Markdown/富媒体等）
     * @return 发送结果（id 可用于撤回）
     */
    public MessageIdResult postC2c(String userOpenid, PostMessage message) {
        requireId(userOpenid, "userOpenid");
        Objects.requireNonNull(message, "message");
        return api.post("/v2/users/" + encode(userOpenid) + "/messages", message, MessageIdResult.class);
    }

    /**
     * 发送单聊纯文本（msg_type=0）。
     *
     * @param userOpenid 用户 OpenID
     * @param content    文本内容
     * @param reply      被动回复信息；可为 null 表示主动消息
     * @return 发送结果
     */
    public MessageIdResult postC2cText(String userOpenid, String content, Reply reply) {
        PostMessage msg = PostMessage.text(content);
        if (reply != null) {
            msg = reply.apply(msg);
        }
        return postC2c(userOpenid, msg);
    }

    /**
     * 发送单聊 Markdown（msg_type=2）。
     *
     * @param userOpenid 用户 OpenID
     * @param markdown   自定义 Markdown 文本
     * @param reply      被动回复信息；可为 null
     * @return 发送结果
     */
    public MessageIdResult postC2cMarkdown(String userOpenid, String markdown, Reply reply) {
        PostMessage msg = PostMessage.markdown(markdown);
        if (reply != null) {
            msg = reply.apply(msg);
        }
        return postC2c(userOpenid, msg);
    }

    /**
     * 撤回单聊消息（仅机器人自己发送的；超过约 2 分钟不可撤回）。
     *
     * @param userOpenid 用户 OpenID
     * @param messageId  发送时返回的 id
     */
    public void recallC2c(String userOpenid, String messageId) {
        requireId(userOpenid, "userOpenid");
        requireId(messageId, "messageId");
        api.delete("/v2/users/" + encode(userOpenid) + "/messages/" + encode(messageId), Void.class);
    }

    /**
     * URL 方式上传单聊富媒体（平台下载转存）。
     *
     * @param userOpenid 用户 OpenID
     * @param fileType   1=图片 2=视频 3=语音 4=文件
     * @param url        公网可访问的 http(s) URL
     * @return file_info 等结果
     */
    public FileInfo uploadC2cUrl(String userOpenid, FileType fileType, String url) {
        requireId(userOpenid, "userOpenid");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("file_type", fileType.code());
        body.put("url", url);
        return api.post("/v2/users/" + encode(userOpenid) + "/files", body, FileInfo.class);
    }

    /**
     * 流式发送单聊消息。
     * <p>
     * 首片（无 stream_msg_id）返回的 id 为后续分片的 stream_msg_id。
     * input_state：1=生成中，10=结束；index 从 0 递增。
     *
     * @param userOpenid 用户 OpenID
     * @param message    流式消息请求体
     * @return 含 stream_msg_id 的结果
     */
    public StreamMessageResult postC2cStream(String userOpenid, StreamMessage message) {
        requireId(userOpenid, "userOpenid");
        Objects.requireNonNull(message, "message");
        return api.post("/v2/users/" + encode(userOpenid) + "/stream_messages",
                message, StreamMessageResult.class);
    }

    /**
     * 流式发单段 Markdown 的便捷方法。
     *
     * @param userOpenid  用户 OpenID
     * @param streamMsgId 流式消息 ID；首片传 null
     * @param index       分片序号，从 0 递增
     * @param markdown    本片 Markdown 内容
     * @param msgId       被动回复的原消息 id
     * @param finished    true 表示结束片（input_state=10）
     * @return 流式发送结果
     */
    public StreamMessageResult streamC2cChunk(String userOpenid, String streamMsgId, int index,
                                              String markdown, String msgId, boolean finished) {
        StreamMessage msg = finished
                ? StreamMessage.finish(streamMsgId, index, markdown, StreamMessage.CONTENT_MARKDOWN, msgId, 1)
                : streamMsgId == null
                ? StreamMessage.first(markdown, StreamMessage.CONTENT_MARKDOWN, msgId, 1)
                : StreamMessage.append(streamMsgId, index, markdown, StreamMessage.CONTENT_MARKDOWN, msgId, 1);
        return postC2cStream(userOpenid, msg);
    }

    // ---- 群聊 ----

    /**
     * 发送群聊消息。
     *
     * @param groupOpenid 群 OpenID（事件 group_openid）
     * @param message     消息体
     * @return 发送结果
     */
    public MessageIdResult postGroup(String groupOpenid, PostMessage message) {
        requireId(groupOpenid, "groupOpenid");
        Objects.requireNonNull(message, "message");
        return api.post("/v2/groups/" + encode(groupOpenid) + "/messages", message, MessageIdResult.class);
    }

    /**
     * 发送群聊纯文本（msg_type=0）。
     *
     * @param groupOpenid 群 OpenID
     * @param content     文本内容
     * @param reply       被动回复信息；可为 null
     * @return 发送结果
     */
    public MessageIdResult postGroupText(String groupOpenid, String content, Reply reply) {
        PostMessage msg = PostMessage.text(content);
        if (reply != null) {
            msg = reply.apply(msg);
        }
        return postGroup(groupOpenid, msg);
    }

    /**
     * 发送群聊 Markdown（msg_type=2）。
     *
     * @param groupOpenid 群 OpenID
     * @param markdown    Markdown 文本
     * @param reply       被动回复信息；可为 null
     * @return 发送结果
     */
    public MessageIdResult postGroupMarkdown(String groupOpenid, String markdown, Reply reply) {
        PostMessage msg = PostMessage.markdown(markdown);
        if (reply != null) {
            msg = reply.apply(msg);
        }
        return postGroup(groupOpenid, msg);
    }

    /**
     * 发送群聊富媒体（msg_type=7）。
     *
     * @param groupOpenid 群 OpenID
     * @param fileInfo    上传接口返回的 file_info
     * @param reply       被动回复信息；可为 null
     * @return 发送结果
     */
    public MessageIdResult postGroupMedia(String groupOpenid, String fileInfo, Reply reply) {
        PostMessage msg = PostMessage.media(fileInfo);
        if (reply != null) {
            msg = reply.apply(msg);
        }
        return postGroup(groupOpenid, msg);
    }

    /**
     * 撤回群聊消息。
     *
     * @param groupOpenid 群 OpenID
     * @param messageId   发送时返回的 id
     */
    public void recallGroup(String groupOpenid, String messageId) {
        requireId(groupOpenid, "groupOpenid");
        requireId(messageId, "messageId");
        api.delete("/v2/groups/" + encode(groupOpenid) + "/messages/" + encode(messageId), Void.class);
    }

    /**
     * URL 方式上传群聊富媒体。
     *
     * @param groupOpenid 群 OpenID
     * @param fileType    1=图片 2=视频 3=语音 4=文件
     * @param url         公网 URL
     * @return file_info 等结果
     */
    public FileInfo uploadGroupUrl(String groupOpenid, FileType fileType, String url) {
        requireId(groupOpenid, "groupOpenid");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("file_type", fileType.code());
        body.put("url", url);
        return api.post("/v2/groups/" + encode(groupOpenid) + "/files", body, FileInfo.class);
    }

    private static String encode(String s) {
        return ApiCall.pathEncode(s);
    }

    private static void requireId(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }
}
