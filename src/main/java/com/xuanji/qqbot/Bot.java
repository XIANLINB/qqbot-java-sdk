package com.xuanji.qqbot;

import com.xuanji.qqbot.api.GatewayApi;
import com.xuanji.qqbot.api.GroupAdminApi;
import com.xuanji.qqbot.api.MediaApi;
import com.xuanji.qqbot.api.JoinApprovalStrategyApi;
import com.xuanji.qqbot.api.MenuPanelApi;
import com.xuanji.qqbot.api.MessageApi;
import com.xuanji.qqbot.model.message.*;
import com.xuanji.qqbot.event.Events;
import com.xuanji.qqbot.event.Intents;
import com.xuanji.qqbot.http.ApiCall;
import com.xuanji.qqbot.http.TokenSource;
import com.xuanji.qqbot.http.Transport;
import com.xuanji.qqbot.event.Event;
import com.xuanji.qqbot.model.media.FileInfo;
import com.xuanji.qqbot.model.media.FileType;
import com.xuanji.qqbot.model.media.Scope;
import com.xuanji.qqbot.ws.GatewaySupervisor;
import com.xuanji.qqbot.ws.ConnectListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * QQ 机器人「动作」门面：只封装对官方 OpenAPI 的行为调用。
 * <p>
 * <b>业务能力（动作）</b>：
 * <ul>
 *   <li>机器人信息：{@link #getBotInfo()}</li>
 *   <li>主动消息：{@code proactiveMsgGroup / proactiveMsgC2c ...}</li>
 *   <li>被动回复：{@code reply / replyMarkdown / replyMedia ...}</li>
 *   <li>富媒体：{@code upload* / sendMedia}</li>
 *   <li>群管理：{@link #group()} — 成员/禁言/黑名单/入群审批/群信息等</li>
 * </ul>
 * <p>
 * <b>以下不是业务能力，而是基础设施（由框架/启动器使用，业务一般不直接碰）</b>：
 * <ul>
 *   <li>构建与生命周期：{@code builder()} / {@link #close()} — 创建与释放客户端</li>
 *   <li>{@link #events()} — 事件总线，供监听注册</li>
 *   <li>{@link #connectGateway(long)} — 建立 WebSocket 收事件；业务在监听里再调动作方法</li>
 * </ul>
 * 实现 {@link AutoCloseable}。
 */
public class Bot implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    private final QqBotOptions options;
    private final Transport transport;
    private final boolean ownsTransport;
    private final TokenSource tokenSource;
    private final ApiCall apiCall;
    private final MessageApi messageApi;
    private final MediaApi mediaApi;
    private final GroupAdminApi groupAdminApi;
    private final MenuPanelApi menuPanelApi;
    private final JoinApprovalStrategyApi joinApprovalApi;
    private final com.xuanji.qqbot.api.InteractionApi interactionApi;
    private final GatewayApi gatewayApi;
    private final Events events;
    private final Executor ownedExecutor;
    private final MsgSeqAllocator seqAllocator = new MsgSeqAllocator();
    private GatewaySupervisor supervisor;

    protected Bot(QqBotOptions options, Transport transport, boolean ownsTransport, Executor ownedExecutor) {
        this.options = options;
        this.transport = transport;
        this.ownsTransport = ownsTransport;
        this.ownedExecutor = ownedExecutor;
        this.tokenSource = new TokenSource(transport, options);
        this.events = new Events(options.eventExecutor());
        this.apiCall = new ApiCall(transport, options.baseUri(), tokenSource::accessToken,
                events::botTag, events::logRawPayload);
        // 富媒体上传走独立 transport：超时可单独配置（默认 60s/60s）
        MediaSpec mediaSpec = options.media() == null ? MediaSpec.defaults() : options.media();
        Transport mediaTransport = new Transport.JdkTransport(mediaSpec.connectTimeout(), mediaSpec.requestTimeout());
        ApiCall mediaCall = new ApiCall(mediaTransport, options.baseUri(), tokenSource::accessToken,
                events::botTag, events::logRawPayload);
        this.messageApi = new MessageApi(apiCall);
        this.mediaApi = new MediaApi(mediaCall, mediaSpec);
        this.groupAdminApi = new GroupAdminApi(apiCall);
        this.menuPanelApi = new MenuPanelApi(apiCall);
        this.joinApprovalApi = new JoinApprovalStrategyApi(apiCall);
        this.interactionApi = new com.xuanji.qqbot.api.InteractionApi(apiCall);
        this.gatewayApi = new GatewayApi(apiCall);
    }

    /**
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param options 配置
     * @return Bot 实例
     */
    public static Bot create(QqBotOptions options) {
        return builder().options(options).build();
    }

    // ==================== 动作 API ====================
    //
    // 官方发送群/单聊消息关键字段：
    //   msg_id    被动：事件 d.id（群约 5 分钟/5 次）
    //   event_id  被动：事件最外层 id；与 msg_id 二选一
    //             支持：INTERACTION_CREATE / GROUP_ADD_ROBOT / GROUP_MSG_RECEIVE 等
    //   msg_seq   与 msg_id 联用，默认 1；同 msg_id+msg_seq 重复会失败
    //   message_reference  引用回复（展示为引用气泡，≠ msg_id 被动）
    //   media     富媒体 file_info
    // 主动 = 不带 msg_id/event_id；被动 = 必须带其一（msg 消息建议再带 msg_seq）

    /**
     * 获取机器人自身信息（GET /users/@me）。
     *
     * @return 机器人资料
     */
    public GatewayApi.BotProfile getBotInfo() {
        return gatewayApi.me();
    }

    /**
     * 生成机器人分享链接（POST /v2/generate_url_link）。
     *
     * @return 分享链接
     */
    public MenuPanelApi.ShareUrlLink generateShareUrl() {
        return menuPanelApi.generateShareUrl();
    }

    /**
     * 生成带 callback_data 的分享链接（最长 32 字符）。
     *
     * @param callbackData 添加机器人时透传的自定义数据
     * @return 分享链接
     */
    public MenuPanelApi.ShareUrlLink generateShareUrl(String callbackData) {
        return menuPanelApi.generateShareUrl(callbackData);
    }

    /**
     * 自定义菜单 / 指令面板 API。
     *
     * @return 菜单与面板
     */
    public MenuPanelApi menuPanel() {
        return menuPanelApi;
    }

    /**
     * 被动回复事件（推荐）：自动填 msg_id 或 event_id，并自动分配 msg_seq。
     * <ul>
     *   <li>消息事件（群@ / 全量 / 单聊）：自动写 msg_id + 自增 msg_seq</li>
     *   <li>进群等：自动写 event_id</li>
     * </ul>
     *
     * @param event   触发本次业务的事件
     * @param content 回复文本
     * @return 发送结果
     */
    public MessageIdResult reply(Event event, String content) {
        return reply(event, PostMessage.text(content));
    }

    /**
     * 被动回复任意消息体，自动补齐被动字段。
     *
     * @param event 事件
     * @param body  消息体
     * @return 发送结果
     */
    public MessageIdResult reply(Event event, PostMessage body) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(body, "body");
        String msgId = event.messageId();
        String eventId = event.eventId();
        String groupOpenid = event.groupOpenid();
        String userOpenid = event.userOpenid();

        PostMessage out = body;
        if (msgId != null && !msgId.isBlank()) {
            int seq = seqAllocator.next(msgId);
            out = out.reply(msgId, seq);
        } else if (eventId != null && !eventId.isBlank()) {
            out = out.replyByEvent(eventId);
        }

        if (groupOpenid != null && !groupOpenid.isBlank()) {
            return messageApi.postGroup(groupOpenid, out);
        }
        if (userOpenid != null && !userOpenid.isBlank()) {
            return messageApi.postC2c(userOpenid, out);
        }
        throw new IllegalArgumentException(
                "事件无法确定回复目标：groupOpenid/userOpenid 均为空, type=" + event.type());
    }

    /**
     * 被动回复富媒体，自动 msg_id/event_id + msg_seq。
     *
     * @param event    事件
     * @param fileInfo 上传返回的 file_info
     * @return 发送结果
     */
    public MessageIdResult replyMedia(Event event, String fileInfo) {
        return reply(event, PostMessage.media(fileInfo));
    }

    /**
     * 被动回复 Markdown（原生语法）。
     *
     * @param event 事件
     * @param markdown Markdown 全文
     * @return 发送结果
     */
    public MessageIdResult replyMarkdown(Event event, String markdown) {
        return reply(event, PostMessage.markdown(markdown));
    }

    /**
     * 被动回复 Markdown + 内嵌键盘（≤5×5）。
     *
     * @param event 事件
     * @param markdown Markdown
     * @param keyboard 键盘
     * @return 发送结果
     */
    public MessageIdResult replyMarkdown(Event event, String markdown, com.xuanji.qqbot.model.keyboard.Keyboard keyboard) {
        return reply(event, PostMessage.markdownWithKeyboard(markdown, keyboard));
    }

    /**
     * 被动回复 Ark 卡片（msg_type=3）。
     *
     * @param event 事件
     * @param ark ArkMsg
     * @return 发送结果
     */
    public MessageIdResult replyArk(Event event, com.xuanji.qqbot.model.message.ArkMsg ark) {
        return reply(event, PostMessage.ark(ark));
    }

    /**
     * 被动回复图文卡片（msg_type=8）。
     *
     * @param event 事件
     * @param card ImageTextCard
     * @return 发送结果
     */
    public MessageIdResult replyCard(Event event, com.xuanji.qqbot.model.message.ImageTextCard card) {
        return reply(event, PostMessage.card(card));
    }

    // ---- 主动消息（无 msg_id / event_id，内容类型与被动一致）----

    /**
     * 主动发群文本。
     *
     * @param groupOpenid 群 OpenID
     * @param content 文本
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgGroup(String groupOpenid, String content) {
        return messageApi.postGroup(groupOpenid, PostMessage.text(content));
    }

    /**
     * 主动发群 Markdown（可选键盘）。
     *
     * @param groupOpenid 群
     * @param markdown Markdown
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgGroupMarkdown(String groupOpenid, String markdown) {
        return messageApi.postGroup(groupOpenid, PostMessage.markdown(markdown));
    }

    /**
     * 主动发群 Markdown + 内嵌键盘（≤5×5）。
     *
     * @param groupOpenid 群
     * @param markdown Markdown
     * @param keyboard 键盘
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgGroupMarkdown(String groupOpenid, String markdown,
                                                     com.xuanji.qqbot.model.keyboard.Keyboard keyboard) {
        return messageApi.postGroup(groupOpenid, PostMessage.markdownWithKeyboard(markdown, keyboard));
    }

    /**
     * 主动发群 Ark 卡片（msg_type=3）。
     *
     * @param groupOpenid 群
     * @param ark ArkMsg
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgGroupArk(String groupOpenid, com.xuanji.qqbot.model.message.ArkMsg ark) {
        return messageApi.postGroup(groupOpenid, PostMessage.ark(ark));
    }

    /**
     * 主动发群图文卡片（msg_type=8）。
     *
     * @param groupOpenid 群
     * @param card ImageTextCard
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgGroupCard(String groupOpenid, com.xuanji.qqbot.model.message.ImageTextCard card) {
        return messageApi.postGroup(groupOpenid, PostMessage.card(card));
    }

    /**
     * 主动发群富媒体。
     *
     * @param groupOpenid 群
     * @param fileInfo file_info
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgGroupMedia(String groupOpenid, String fileInfo) {
        return messageApi.postGroup(groupOpenid, PostMessage.media(fileInfo));
    }

    /**
     * 主动发单聊文本。
     *
     * @param userOpenid 用户
     * @param content 文本
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgC2c(String userOpenid, String content) {
        return messageApi.postC2c(userOpenid, PostMessage.text(content));
    }

    /**
     * 主动发单聊 Markdown。
     *
     * @param userOpenid 用户
     * @param markdown Markdown
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgC2cMarkdown(String userOpenid, String markdown) {
        return messageApi.postC2c(userOpenid, PostMessage.markdown(markdown));
    }

    /**
     * 主动发单聊 Markdown + 键盘。
     *
     * @param userOpenid 用户
     * @param markdown Markdown
     * @param keyboard 键盘
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgC2cMarkdown(String userOpenid, String markdown,
                                                   com.xuanji.qqbot.model.keyboard.Keyboard keyboard) {
        return messageApi.postC2c(userOpenid, PostMessage.markdownWithKeyboard(markdown, keyboard));
    }

    /**
     * 主动发单聊 Ark。
     *
     * @param userOpenid 用户
     * @param ark ArkMsg
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgC2cArk(String userOpenid, com.xuanji.qqbot.model.message.ArkMsg ark) {
        return messageApi.postC2c(userOpenid, PostMessage.ark(ark));
    }

    /**
     * 主动发单聊图文卡片。
     *
     * @param userOpenid 用户
     * @param card ImageTextCard
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgC2cCard(String userOpenid, com.xuanji.qqbot.model.message.ImageTextCard card) {
        return messageApi.postC2c(userOpenid, PostMessage.card(card));
    }

    /**
     * 主动发单聊富媒体。
     *
     * @param userOpenid 用户
     * @param fileInfo file_info
     * @return 发送结果
     */
    public MessageIdResult proactiveMsgC2cMedia(String userOpenid, String fileInfo) {
        return messageApi.postC2c(userOpenid, PostMessage.media(fileInfo));
    }

    /**
     * 主动发送任意消息体到群/单聊。
     *
     * @param scope GROUP 或 C2C
     * @param openid 对方 OpenID
     * @param body 消息体（不带被动字段）
     * @return 发送结果
     */
    public MessageIdResult proactiveMsg(Scope scope, String openid, PostMessage body) {
        return scope == Scope.GROUP
                ? messageApi.postGroup(openid, body)
                : messageApi.postC2c(openid, body);
    }

    /**
     * 主动发送单聊互动召回文本（is_wakeup=true）。
     *
     * @param userOpenid 用户
     * @param content 文本
     * @return 发送结果
     */
    public MessageIdResult wakeupMsgC2c(String userOpenid, String content) {
        return messageApi.postC2c(userOpenid, PostMessage.wakeupText(content));
    }

    /**
     * 主动发单聊 Markdown 召回（is_wakeup=true）。
     *
     * @param userOpenid 用户
     * @param markdown Markdown
     * @return 发送结果
     */
    public MessageIdResult wakeupMsgC2cMarkdown(String userOpenid, String markdown) {
        return messageApi.postC2c(userOpenid, new PostMessage(
                MsgType.MARKDOWN.code(), null, com.xuanji.qqbot.model.message.Markdown.of(markdown),
                null, null, null, null, null, null, Boolean.TRUE, null, null, null));
    }

    /**
     * 主动发单聊「正在输入」（msg_type=6，最长 60s）。
     *
     * @param userOpenid 用户
     * @param seconds 1-60
     * @return 发送结果
     */
    public MessageIdResult typingC2c(String userOpenid, int seconds) {
        int sec = Math.max(1, Math.min(seconds, 60));
        PostMessage body = new PostMessage(
                MsgType.INPUT_NOTIFY.code(),
                null, null, null, null, null, null, null, null, null,
                InputNotify.typing(sec), null, null);
        return messageApi.postC2c(userOpenid, body);
    }

    // ---- 流式单聊 ----

    /**
     * 流式单聊：首片（index=0，生成中）。
     *
     * @param userOpenid 用户 OpenID
     * @param msgId      被回复消息 id
     * @param content    Markdown 正文
     * @return 含 stream_msg_id 的结果
     */
    public StreamMessageResult streamC2cFirst(String userOpenid, String msgId, String content) {
        return messageApi.postC2cStream(userOpenid,
                StreamMessage.first(content, StreamMessage.CONTENT_MARKDOWN, msgId, 1));
    }

    /**
     * 流式单聊：续片（追加正文）。
     *
     * @param userOpenid  用户 OpenID
     * @param streamMsgId 首片返回的 id
     * @param index       分片序号，从 1 起递增
     * @param content     本片内容
     * @param msgId       被回复消息 id
     * @return 结果
     */
    public StreamMessageResult streamC2cAppend(String userOpenid, String streamMsgId,
                                               int index, String content, String msgId) {
        return messageApi.postC2cStream(userOpenid,
                StreamMessage.append(streamMsgId, index, content, StreamMessage.CONTENT_MARKDOWN, msgId, 1));
    }

    /**
     * 流式单聊：结束片（input_state=10）。
     *
     * @param userOpenid  用户 OpenID
     * @param streamMsgId 流式消息 id
     * @param index       分片序号
     * @param content     最终正文（replace 模式通常为全量）
     * @param msgId       被回复消息 id
     * @return 结果
     */
    public StreamMessageResult streamC2cFinish(String userOpenid, String streamMsgId,
                                               int index, String content, String msgId) {
        return messageApi.postC2cStream(userOpenid,
                StreamMessage.finish(streamMsgId, index, content, StreamMessage.CONTENT_MARKDOWN, msgId, 1));
    }

    /**
     * 流式发送整段文本：自动拆首片+续片+结束（每片约 limit 字符）。
     *
     * @param userOpenid 用户 OpenID
     * @param msgId      被回复消息 id
     * @param markdown   完整 Markdown
     * @return 结束片结果
     */
    public StreamMessageResult streamC2cText(String userOpenid, String msgId, String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return streamC2cFinish(userOpenid, null, 0, "", msgId);
        }
        final int chunk = 800;
        StreamMessageResult first = streamC2cFirst(userOpenid, msgId, markdown.substring(0, Math.min(chunk, markdown.length())));
        String streamId = first == null ? null : first.id();
        int idx = 1;
        for (int i = chunk; i < markdown.length(); i += chunk) {
            int to = Math.min(markdown.length(), i + chunk);
            boolean last = to >= markdown.length();
            if (last) {
                return streamC2cFinish(userOpenid, streamId, idx, markdown.substring(i, to), msgId);
            }
            streamC2cAppend(userOpenid, streamId, idx, markdown.substring(i, to), msgId);
            idx++;
        }
        return streamC2cFinish(userOpenid, streamId, idx, "", msgId);
    }

    /**
     * 入群自动审批策略 API。
     *
     * @return 策略接口
     */
    public JoinApprovalStrategyApi joinApproval() {
        return joinApprovalApi;
    }

    /**
     * 互动事件响应（type=11/12 必须调用，否则客户端一直 loading）。
     * <p>
     * <b>响应超时：指令回调类场景为 3 秒</b>，请在收到事件后立即响应；
     * 同一 interaction_id 只能回应一次。
     *
     * @param interactionId 互动 id（事件 d.id，不带事件名前缀）
     * @param code 0=成功 1=失败 2=频繁 3=重复 4=无权限 5=仅管理员
     */
    public void respondInteraction(String interactionId, int code) {
        interactionApi.respond(interactionId, code);
    }

    /**
     * 以成功码响应互动事件（3 秒超时，尽快调用）。
     *
     * @param interactionId 互动 id
     */
    public void respondInteractionSuccess(String interactionId) {
        interactionApi.respondSuccess(interactionId);
    }

    /**
     * 以事件对象响应（取 d.id，3 秒超时，尽快调用）。
     *
     * @param event INTERACTION_CREATE 事件
     */
    public void respondInteractionSuccess(com.xuanji.qqbot.event.InteractionCreate event) {
        interactionApi.respondSuccess(event.id());
    }

    // ---- 被动：按 msg_id（用户消息回复）----
    // msg_id 取事件 d.id；msg_seq 同一原消息多次回复时递增

    /**
     * 被动回群文本（msg_id + msg_seq）。
     *
     * @param groupOpenid 群 OpenID
     * @param msgId       事件中的消息 id（d.id）
     * @param content     文本
     * @param msgSeq      序号；不填业务可传 1；同 id 再回需 2、3…
     * @return 发送结果
     */
    public MessageIdResult passiveMsgGroup(String groupOpenid, String msgId, String content, int msgSeq) {
        return messageApi.postGroup(groupOpenid, PostMessage.text(content).reply(msgId, msgSeq));
    }

    /**
     * 被动回群 Markdown（msg_id + msg_seq）。
     *
     * @param groupOpenid 群
     * @param msgId 消息 id
     * @param markdown Markdown
     * @param msgSeq 序号
     * @return 发送结果
     */
    public MessageIdResult passiveMsgGroupMarkdown(String groupOpenid, String msgId, String markdown, int msgSeq) {
        return messageApi.postGroup(groupOpenid, PostMessage.markdown(markdown).reply(msgId, msgSeq));
    }

    /**
     * 被动回群 Markdown + 键盘。
     *
     * @param groupOpenid 群
     * @param msgId 消息 id
     * @param markdown Markdown
     * @param keyboard 键盘
     * @param msgSeq 序号
     * @return 发送结果
     */
    public MessageIdResult passiveMsgGroupMarkdown(String groupOpenid, String msgId, String markdown,
                                                   com.xuanji.qqbot.model.keyboard.Keyboard keyboard, int msgSeq) {
        return messageApi.postGroup(groupOpenid,
                PostMessage.markdownWithKeyboard(markdown, keyboard).reply(msgId, msgSeq));
    }

    /**
     * 被动回群 Ark。
     *
     * @param groupOpenid 群
     * @param msgId 消息 id
     * @param ark ArkMsg
     * @param msgSeq 序号
     * @return 发送结果
     */
    public MessageIdResult passiveMsgGroupArk(String groupOpenid, String msgId,
                                              com.xuanji.qqbot.model.message.ArkMsg ark, int msgSeq) {
        return messageApi.postGroup(groupOpenid, PostMessage.ark(ark).reply(msgId, msgSeq));
    }

    /**
     * 被动回群图文卡片。
     *
     * @param groupOpenid 群
     * @param msgId 消息 id
     * @param card ImageTextCard
     * @param msgSeq 序号
     * @return 发送结果
     */
    public MessageIdResult passiveMsgGroupCard(String groupOpenid, String msgId,
                                               com.xuanji.qqbot.model.message.ImageTextCard card, int msgSeq) {
        return messageApi.postGroup(groupOpenid, PostMessage.card(card).reply(msgId, msgSeq));
    }

    /**
     * 被动回单聊文本（msg_id + msg_seq）。
     *
     * @param userOpenid 用户
     * @param msgId 消息 id
     * @param content 文本
     * @param msgSeq 序号
     * @return 发送结果
     */
    public MessageIdResult passiveMsgC2c(String userOpenid, String msgId, String content, int msgSeq) {
        return messageApi.postC2c(userOpenid, PostMessage.text(content).reply(msgId, msgSeq));
    }

    /**
     * 被动回单聊 Markdown（msg_id + msg_seq）。
     *
     * @param userOpenid 用户
     * @param msgId 消息 id
     * @param markdown Markdown
     * @param msgSeq 序号
     * @return 发送结果
     */
    public MessageIdResult passiveMsgC2cMarkdown(String userOpenid, String msgId, String markdown, int msgSeq) {
        return messageApi.postC2c(userOpenid, PostMessage.markdown(markdown).reply(msgId, msgSeq));
    }

    /**
     * 被动回单聊 Markdown + 键盘。
     *
     * @param userOpenid 用户
     * @param msgId 消息 id
     * @param markdown Markdown
     * @param keyboard 键盘
     * @param msgSeq 序号
     * @return 发送结果
     */
    public MessageIdResult passiveMsgC2cMarkdown(String userOpenid, String msgId, String markdown,
                                                 com.xuanji.qqbot.model.keyboard.Keyboard keyboard, int msgSeq) {
        return messageApi.postC2c(userOpenid,
                PostMessage.markdownWithKeyboard(markdown, keyboard).reply(msgId, msgSeq));
    }

    /**
     * 被动回单聊 Ark。
     *
     * @param userOpenid 用户
     * @param msgId 消息 id
     * @param ark ArkMsg
     * @param msgSeq 序号
     * @return 发送结果
     */
    public MessageIdResult passiveMsgC2cArk(String userOpenid, String msgId,
                                            com.xuanji.qqbot.model.message.ArkMsg ark, int msgSeq) {
        return messageApi.postC2c(userOpenid, PostMessage.ark(ark).reply(msgId, msgSeq));
    }

    /**
     * 被动回单聊图文卡片。
     *
     * @param userOpenid 用户
     * @param msgId 消息 id
     * @param card ImageTextCard
     * @param msgSeq 序号
     * @return 发送结果
     */
    public MessageIdResult passiveMsgC2cCard(String userOpenid, String msgId,
                                             com.xuanji.qqbot.model.message.ImageTextCard card, int msgSeq) {
        return messageApi.postC2c(userOpenid, PostMessage.card(card).reply(msgId, msgSeq));
    }

    /**
     * 被动发送任意消息体到群（需已带 msg_id/event_id）。
     *
     * @param groupOpenid 群
     * @param body 消息体
     * @return 发送结果
     */
    public MessageIdResult passiveMsgGroup(String groupOpenid, PostMessage body) {
        return messageApi.postGroup(groupOpenid, body);
    }

    /**
     * 被动发送任意消息体到单聊。
     *
     * @param userOpenid 用户
     * @param body 消息体
     * @return 发送结果
     */
    public MessageIdResult passiveMsgC2c(String userOpenid, PostMessage body) {
        return messageApi.postC2c(userOpenid, body);
    }

    // ---- 被动：按 event_id（系统事件回复；与 msg_id 二选一）----
    // 官方支持 event_id 的事件含：INTERACTION_CREATE、GROUP_ADD_ROBOT、GROUP_MSG_RECEIVE 等
    // event_id 取自事件信封最外层 id（Events 解析出的 eventId()）

    /**
     * 被动回群文本（event_id）。用于 GROUP_ADD_ROBOT 等仅有信封 id 的事件。
     *
     * @param groupOpenid 群 OpenID
     * @param eventId     事件信封 id（event.eventId()）
     * @param content     文本
     * @return 发送结果
     */
    public MessageIdResult passiveMsgGroupByEvent(String groupOpenid, String eventId, String content) {
        return messageApi.postGroup(groupOpenid, PostMessage.text(content).replyByEvent(eventId));
    }

    /**
     * 被动回单聊文本（event_id）。
     *
     * @param userOpenid 用户 OpenID
     * @param eventId    事件信封 id
     * @param content    文本
     * @return 发送结果
     */
    public MessageIdResult passiveMsgC2cByEvent(String userOpenid, String eventId, String content) {
        return messageApi.postC2c(userOpenid, PostMessage.text(content).replyByEvent(eventId));
    }

    /**
     * 按事件对象被动回群：有 id 用 msg_id+seq，否则用 event_id。
     *
     * @param groupOpenid 群 OpenID
     * @param msgId       消息 id，可 null
     * @param eventId     事件信封 id，可 null
     * @param content     文本
     * @param msgSeq      msg_id 模式下的序号
     * @return 发送结果
     */
    public MessageIdResult passiveMsgGroupAuto(String groupOpenid, String msgId, String eventId,
                                               String content, int msgSeq) {
        PostMessage body = PostMessage.text(content);
        if (msgId != null && !msgId.isBlank()) {
            body = body.reply(msgId, msgSeq);
        } else if (eventId != null && !eventId.isBlank()) {
            body = body.replyByEvent(eventId);
        }
        return messageApi.postGroup(groupOpenid, body);
    }

    // ---- 组合：引用 / 富媒体 ----

    /**
     * 被动回群并「引用」某条消息（message_reference，展示为引用气泡）。
     * <p>
     * reference 的 message_id：他人消息用 message_scene.ext 中 msg_idx；
     * 机器人自己的消息用响应 ext_info.ref_idx。
     * 注意：引用 ≠ msg_id 被动回复；两者可同时使用。
     *
     * @param groupOpenid 群 OpenID
     * @param replyMsgId  被回复消息 id（写入 msg_id）
     * @param msgSeq      序号
     * @param content     文本
     * @param referenceIdx 引用索引 REFIDX…（写入 message_reference）
     * @return 发送结果
     */
    public MessageIdResult passiveMsgGroupWithReference(String groupOpenid, String replyMsgId, int msgSeq,
                                                        String content, String referenceIdx) {
        PostMessage body = PostMessage.text(content)
                .reply(replyMsgId, msgSeq)
                .withReference(referenceIdx);
        return messageApi.postGroup(groupOpenid, body);
    }

    /**
     * 被动回群富媒体（file_info + msg_id/seq）。
     *
     * @param groupOpenid 群 OpenID
     * @param msgId       被回复消息 id
     * @param msgSeq      序号
     * @param fileInfo    上传返回的 file_info
     * @return 发送结果
     */
    public MessageIdResult passiveMsgGroupMedia(String groupOpenid, String msgId, int msgSeq, String fileInfo) {
        return messageApi.postGroup(groupOpenid, PostMessage.media(fileInfo).reply(msgId, msgSeq));
    }

    /**
     * 按回复目标发送任意消息体（含 Markdown/Keyboard）。
     *
     * @param scope   C2C 或 GROUP
     * @param openid  对方 OpenID
     * @param body    消息体（不含被动字段亦可）
     * @param target  回复目标（msgId / eventId）
     * @return 发送结果
     */
    public MessageIdResult send(Scope scope, String openid, PostMessage body, ReplyTarget target) {
        PostMessage msg = target == null ? body : target.apply(body);
        return scope == Scope.GROUP
                ? messageApi.postGroup(openid, msg)
                : messageApi.postC2c(openid, msg);
    }

    /**
     * 撤回群消息。
     *
     * @param groupOpenid 群 OpenID
     * @param messageId   消息 id
     */
    public void recallGroupMsg(String groupOpenid, String messageId) {
        messageApi.recallGroup(groupOpenid, messageId);
    }

    /**
     * 撤回单聊消息。
     *
     * @param userOpenid 用户 OpenID
     * @param messageId  消息 id
     */
    public void recallC2cMsg(String userOpenid, String messageId) {
        messageApi.recallC2c(userOpenid, messageId);
    }

    /**
     * URL 上传群富媒体。
     *
     * @param groupOpenid 群 OpenID
     * @param fileType    类型
     * @param url         公网 URL
     * @return file_info
     */
    public FileInfo uploadGroupMediaUrl(String groupOpenid, FileType fileType, String url) {
        return mediaApi.uploadUrl(Scope.GROUP, groupOpenid, fileType, url);
    }

    /**
     * URL 上传单聊富媒体。
     *
     * @param userOpenid 用户 OpenID
     * @param fileType   类型
     * @param url        公网 URL
     * @return file_info
     */
    public FileInfo uploadC2cMediaUrl(String userOpenid, FileType fileType, String url) {
        return mediaApi.uploadUrl(Scope.C2C, userOpenid, fileType, url);
    }

    /**
     * 上传本地文件（小文件 Base64 / 大文件分片）。
     *
     * @param scope    场景
     * @param openid   OpenID
     * @param fileType 类型
     * @param file     本地路径
     * @return file_info
     */
    public FileInfo uploadMedia(Scope scope, String openid, FileType fileType, Path file) {
        return mediaApi.uploadFile(scope, openid, fileType, file);
    }

    /**
     * 发送富媒体消息。
     *
     * @param scope    场景
     * @param openid   OpenID
     * @param fileInfo 上传结果 file_info
     * @param target   被动回复目标，可 null 表示主动
     * @return 发送结果
     */
    public MessageIdResult sendMedia(Scope scope, String openid, String fileInfo, ReplyTarget target) {
        PostMessage body = PostMessage.media(fileInfo);
        if (target != null) {
            body = target.apply(body);
        }
        return scope == Scope.GROUP
                ? messageApi.postGroup(openid, body)
                : messageApi.postC2c(openid, body);
    }

    // ==================== 事件与连接 ====================

    /**
     * @return 事件总线
     */
    public Events events() {
        return events;
    }

    /**
     * 连接官方网关（自动重连），阻塞至就绪；失败抛异常。
     *
     * @param intents 事件位，见 {@link Intents}
     * @return 网关守护
     */
    public GatewaySupervisor connectGateway(long intents) {
        return connectGateway(intents, true);
    }

    /**
     * 连接官方网关（自动重连）。
     *
     * @param intents  事件位，见 {@link Intents}
     * @param failFast true=就绪前阻塞，失败抛异常；false=失败转后台退避重试
     * @return 网关守护
     */
    public GatewaySupervisor connectGateway(long intents, boolean failFast) {
        return connectGateway(intents, failFast, options.shard());
    }

    /**
     * 连接官方网关（自动重连），使用指定分片。
     *
     * @param intents 事件位，见 {@link Intents}
     * @param shard  WebSocket 分片 [index, num]，如 [0,1]（单分片）或 [n, N]（多分片）
     * @return 网关守护
     */
    public GatewaySupervisor connectGateway(long intents, int[] shard) {
        return connectGateway(intents, true, shard);
    }

    /**
     * 连接官方网关（自动重连）。
     *
     * @param intents   事件位，见 {@link Intents}
     * @param failFast  true=就绪前阻塞，失败抛异常；false=失败转后台退避重试
     * @param shard     WebSocket 分片 [index, num]，如 [0,1]（单分片）或 [n, N]（多分片）
     * @return 网关守护
     */
    public GatewaySupervisor connectGateway(long intents, boolean failFast, int[] shard) {
        if (supervisor != null) {
            throw new IllegalStateException("网关已连接");
        }
        int[] effectiveShard = shard == null || shard.length != 2 ? new int[]{0, 1} : shard;
        GatewaySupervisor sup = new GatewaySupervisor(gatewayApi, tokenSource::accessToken, intents, events,
                effectiveShard, "websocket/" + options.credentials().appId(), options.connectListener());
        if (failFast) {
            sup.startAndAwait();
        } else {
            sup.startBackground();
        }
        this.supervisor = sup;
        return sup;
    }

    /**
     * @return 当前 WebSocket 连接状态；未连接（含 webhook 接入）为 NOT_CONNECTED
     */
    public com.xuanji.qqbot.ws.ConnectState connectState() {
        return supervisor == null
                ? com.xuanji.qqbot.ws.ConnectState.NOT_CONNECTED
                : supervisor.state();
    }

    // ==================== 进阶：分组 API ====================

    /**
     * @return 消息 API
     */
    public MessageApi message() {
        return messageApi;
    }

    /**
     * @return 富媒体 API
     */
    public MediaApi media() {
        return mediaApi;
    }

    /**
     * @return 群管理 API
     */
    public GroupAdminApi group() {
        return groupAdminApi;
    }

    /**
     * @return 网关 API
     */
    public GatewayApi gateway() {
        return gatewayApi;
    }

    /**
     * @return 带鉴权的底层调用
     */
    public ApiCall apiCall() {
        return apiCall;
    }

    /**
     * @return 当前 access_token
     */
    public String accessToken() {
        return tokenSource.accessToken();
    }

    /**
     * @return 配置
     */
    public QqBotOptions options() {
        return options;
    }

    /**
     * @return HTTP 传输
     */
    public Transport transport() {
        return transport;
    }

    @Override
    public void close() {
        if (supervisor != null) {
            try {
                supervisor.close();
            } catch (Exception ignored) {
                // 忽略
            }
            supervisor = null;
        }
        if (ownsTransport) {
            try {
                transport.close();
            } catch (Exception ignored) {
                // 忽略
            }
        }
        if (ownedExecutor instanceof AutoCloseable ac) {
            try {
                ac.close();
            } catch (Exception ignored) {
                // 忽略
            }
        }
        log.debug("Bot closed");
    }

    /**
     * {@link Bot} 构建器。
     */
    public static final class Builder {
        private String appId;
        private String appSecret;
        private QqBotOptions options;
        private Transport transport;
        private Executor eventExecutor;
        private MediaSpec media;
        private ConnectListener connectListener;

        /**
         * @param appId AppID
         * @return this
         */
        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        /**
         * @param appSecret AppSecret
         * @return this
         */
        public Builder appSecret(String appSecret) {
            this.appSecret = appSecret;
            return this;
        }

        /**
         * @param media 富媒体上传配置（超时/重试），null 用默认
         * @return this
         */
        public Builder media(MediaSpec media) {
            this.media = media;
            return this;
        }

        /**
         * @param options 完整配置
         * @return this
         */
        public Builder options(QqBotOptions options) {
            this.options = options;
            return this;
        }

        /**
         * @param transport 自定义传输
         * @return this
         */
        public Builder transport(Transport transport) {
            this.transport = transport;
            return this;
        }

        /**
         * @param eventExecutor 事件线程池
         * @return this
         */
        public Builder eventExecutor(Executor eventExecutor) {
            this.eventExecutor = eventExecutor;
            return this;
        }

        /**
         * @param connectListener WebSocket 连接生命周期监听（见 {@link ConnectListener}）
         * @return this
         */
        public Builder connectListener(ConnectListener connectListener) {
            this.connectListener = connectListener;
            return this;
        }

        /**
         * @return Bot
         */
        public Bot build() {
            QqBotOptions opts = this.options;
            if (opts == null) {
                if (appId == null || appSecret == null) {
                    throw new IllegalArgumentException("需要提供 appId/appSecret");
                }
                opts = QqBotOptions.defaults(new com.xuanji.qqbot.auth.Credentials(appId, appSecret));
            }
        if (eventExecutor != null || this.media != null || this.connectListener != null) {
            opts = new QqBotOptions(
                    opts.credentials(),
                    opts.baseUri(),
                    opts.connectTimeout(),
                    opts.requestTimeout(),
                    opts.tokenRefreshMargin(),
                    eventExecutor,
                    opts.shard(),
                    this.media != null ? this.media : opts.media(),
                    this.connectListener != null ? this.connectListener : opts.connectListener()
            );
        }
            Transport t = transport;
            boolean own = false;
            if (t == null) {
                t = new Transport.JdkTransport(opts.connectTimeout(), opts.requestTimeout());
                own = true;
            }
            return new Bot(opts, t, own, null);
        }
    }
}
