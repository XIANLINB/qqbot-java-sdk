<div align="center">

# qqbot-java-sdk

轻量级 QQ 官方机器人 Java SDK（OpenAPI api-v2）

**QQ官方机器人开发交流群：[1019955788](https://qm.qq.com/q/1019955788)**

</div>

> **QQ 机器人官方文档**：https://bot.q.qq.com/wiki/develop/api-v2/
>
> 使用本 Java-SDK 默认您了解并熟悉 **Java 基本语法**以及 **SpringBoot 框架**开发体系。

## 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 17+ |
| Spring Boot | 3.x / 4.x（已在 4.0.8 验证） |
| Maven | 3.8+ |

```xml
<dependency>
    <groupId>io.github.xianlinb</groupId>
    <artifactId>qqbot-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 快速开始

### 第一步：配置 yml

支持**多机器人混排**（websocket / webhook 数量不限，也可只配一台）。所有节点均可不配置，不配即用默认值：

```yaml
xuanji:
  # ---- WebSocket 机器人（推荐，无需公网）----
  bots:
    - name: 机器人A
      mode: websocket
      app-id: 你的AppID
      app-secret: 你的AppSecret
      intents:                       # 可不配，默认订阅 SDK 全部支持事件
        - GROUP_AND_C2C_EVENT
        - GROUP_MEMBER_EVENT
        - INTERACTION

  # ---- 追加 Webhook 机器人（需公网 HTTPS，端口限 80/443/8080/8443）----
  #   - name: 机器人B
  #     mode: webhook
  #     app-id: xxx
  #     app-secret: yyy
  #     path: /xuanji/webhook          # webhook 方式必须配置回调路径

  # 事件回调线程池（全部机器人共享，默认 8；0=收包线程同步执行，不建议）
  # event:
  #   pool-size: 8

  # 富媒体上传（可不配，默认 60s 连接 / 60s 请求 / 网络失败重试 1 次）
  # media:
  #   connect-timeout: 60s
  #   request-timeout: 60s
  #   retry: 1

  # 调试：打印 WS/Webhook 收发原始报文（默认 false）
  # debug:
  #   raw-payload: false
```

单机器人也可以用更短的旧写法（`xuanji.websocket.enable: true` + app-id/app-secret），两种写法等价。
WebSocket 首连失败默认终止应用启动，可加 `xuanji.websocket.fail-fast: false` 改为后台退避重试。

### 第二步：写第一个插件

创建一个类，加上 `@Xuanji` 和 `@Component`，用事件注解标记方法即可。
单聊收到「你好」回复「你也好」，群聊被 @「你好」也回复「你也好」：

```java
@Xuanji
@Component
public class HelloPlugin {

    /** 单聊：收到「你好」回复「你也好」 */
    @C2cMessageCreateEvent
    @Command(type = CommandMatchType.EXACT, value = "你好")
    public void onC2cHello(C2cMessageCreate msg, Bot bot) {
        bot.reply(msg, "你也好");
    }

    /** 群聊：@机器人 说「你好」回复「你也好」（返回值即回复） */
    @GroupMessageCreateEvent
    @Command(type = CommandMatchType.EXACT, value = "你好", at = At.REQUIRED)
    public String onGroupHello(GroupMessageCreate msg) {
        return "你也好";
    }
}
```

启动 Spring Boot 应用，日志出现 `[xuanji] WebSocket 网关已连接` 即接入成功。

> 群聊里记得 **@机器人** 再发「你好」；未 @ 的普通群消息不会触发带 `at = At.REQUIRED` 的指令。

### 语法糖速览

```java
/** 内容参数注入：String 参数直接拿到去掉 @ 后的正文 */
@GroupMessageCreateEvent
@Command(type = CommandMatchType.REGEX, value = "签到\\d+", at = At.REQUIRED)
public String onSign(GroupMessageCreate msg, String content) {
    return "签到成功：" + content;
}

/** at = At.NOT：仅未 @ 时触发（免打扰记录等场景） */
@GroupMessageCreateEvent
@Command(type = CommandMatchType.REGEX, value = ".*", at = At.NOT)
public void onNoAt(GroupMessageCreate msg) {
    log.info("[未@我] {}", msg.contentWithoutMentions());
}
```

- **返回值即回复**：方法返回 `String`（或 `PostMessage`）自动作为被动回复发送
- **content 注入**：方法参数声明 `String` 直接触发匹配正文；声明 `Bot` 自动绑定为收到事件的那台
- **`@Order`**：插件类与方法都可标注，值小的先触发（Spring 语义）

### Webhook 方式

1. yml 切换：机器人 `mode: webhook` 并配置 `path`（每台各自配置，必须配置）
2. 开放平台配置回调地址：`https://你的域名:端口/xuanji/webhook`
3. 验签（Ed25519）、回调地址验证（op=13）、事件分发全部自动处理

### 多机器人说明

- 全部机器人共用同一套插件代码；`Bot` 参数注入收到事件的那台，`bot.reply` 天然路由正确
- **openid 按 appid 隔离**：同一个群对两台机器人的 `group_openid` 不同，切勿把 A 的 openid 给 B 用
- `bot.connectState()` 可随时查询连接状态（NOT_CONNECTED / CONNECTING / CONNECTED / RECONNECTING / CLOSED）

## 消息类型一览

| 类型 | msg_type | 主动 | 被动 |
|------|----------|------|------|
| 文本 | 0 | `proactiveMsgGroup/C2c` | `reply` |
| Markdown（可挂内嵌键盘） | 2 | `proactiveMsg*Markdown(+Keyboard)` | `replyMarkdown(+Keyboard)` |
| Ark 卡片（23/24/37 模板） | 3 | `proactiveMsg*Ark` | `replyArk` |
| 输入中状态（仅单聊） | 6 | `typingC2c` | — |
| 富媒体（图片/语音/视频/文件） | 7 | `proactiveMsg*Media` | `replyMedia` |
| 图文卡片 | 8 | `proactiveMsg*Card` | `replyCard` |

Markdown 内还可嵌入：`<@openid>` @成员、`<qqbot-cmd-enter/>` 回车指令、`<qqbot-cmd-input/>` 参数指令。

## Bot 能力表（动作方法）

| 方法 | 说明 |
|------|------|
| `getBotInfo()` | 获取机器人详情 |
| `generateShareUrl(callbackData?)` | 生成机器人分享/添加链接 |
| `proactiveMsgGroup(groupOpenid, content)` | 主动发群文本 |
| `proactiveMsgC2c(userOpenid, content)` | 主动发单聊文本 |
| `proactiveMsgGroupMarkdown / C2cMarkdown(...)` | 主动发 Markdown（可带键盘） |
| `proactiveMsgGroupArk / C2cArk(...)` | 主动发 Ark 卡片 |
| `proactiveMsgGroupCard / C2cCard(...)` | 主动发图文卡片 |
| `proactiveMsgGroupMedia / C2cMedia(...)` | 主动发富媒体 |
| `reply(event, content/body)` | 被动回复（自动带 msg_id/event_id，msg_seq 自动递增） |
| `replyMarkdown(event, md, keyboard?)` | 被动回 Markdown（可带键盘） |
| `replyArk(event, ark)` / `replyCard(event, card)` | 被动回卡片 |
| `replyMedia(event, fileInfo)` | 被动回富媒体 |
| `wakeupMsgC2c(userOpenid, content)` | 互动召回（is_wakeup，30 天 4 周期） |
| `typingC2c(userOpenid, seconds)` | 「正在输入」状态 |
| `streamC2cFirst/Append/Finish/Text(...)` | 单聊流式消息（AI 逐段输出） |
| `recallGroupMsg / recallC2cMsg(...)` | 撤回（2 分钟内） |
| `uploadMedia / uploadGroupMediaUrl / sendMedia(...)` | 富媒体上传与发送 |
| `passiveMsgGroupWithReference(...)` | 引用回复 |
| `respondInteraction(id/event, code)` | 响应按钮互动（3 秒内） |
| `group()` | 群管理（见下） |
| `joinApproval()` | 入群自动审批策略（CRUD/执行/白名单） |
| `menuPanel()` | 自定义菜单、指令面板（增删改查/关联对象） |
| `connectState()` | WebSocket 连接状态查询 |
| `events()` | 事件总线（一般用注解即可） |

`group()` 群管理能力：

| 方法 | 说明 |
|------|------|
| `getGroupInfo(groupOpenid)` | 群基本信息（名称/人数/标签） |
| `getBotState(groupOpenid)` | 机器人群内状态（角色/权限） |
| `listMembers / getMember(...)` | 群成员列表/详情 |
| `batchRemoveMembers(...)` | 批量移除成员 |
| `listBlacklist / blacklistAdd / blacklistDel` | 群黑名单 |
| `getRestrictChatSetting / muteMember / unmuteMember` | 禁言查询与设置 |
| `listJoinRequests(...)` | 入群申请列表 |
| `approveJoinRequest / declineJoinRequest(...)` | 入群审批 |

## 注解表

| 注解 | 作用 |
|------|------|
| `@Xuanji` | 标记插件类（需同时为 Spring Bean） |
| `@C2cMessageCreateEvent` | 单聊消息 |
| `@GroupAtMessageCreateEvent` | 群 @ 机器人消息（未开全量时） |
| `@GroupMessageCreateEvent` | 群消息（全量模式，含 @） |
| `@GroupAddRobotEvent` / `@GroupDelRobotEvent` | 机器人进群/退群 |
| `@GroupMemberAddEvent` / `@GroupMemberRemoveEvent` | 群成员加入/退出 |
| `@GroupMsgReceiveEvent` / `@GroupMsgRejectEvent` | 群推送开启/关闭 |
| `@C2cMsgReceiveEvent` / `@C2cMsgRejectEvent` | 单聊推送开启/关闭 |
| `@FriendAddEvent` / `@FriendDelEvent` | 添加/删除好友 |
| `@GroupJoinRequestEvent` | 用户申请加群 |
| `@SubscribeMessageStatusEvent` | 订阅消息授权变更 |
| `@InteractionCreateEvent` | 按钮等互动回调 |
| `@AnyEvent` | 接收全部事件，自行按 `event.type()` 细分 |
| `@Command` | 内容过滤与 @ 语义：`type` 匹配方式（PREFIX/SUFFIX/EXACT/REGEX，可多个命中即触发）；`at` 三态——`At.REQUIRED` 必须 @、`At.ANY` 不关心（默认）、`At.NOT` 必须未 @ |
| `@Order` | 插件类/监听方法执行顺序，值小的先触发 |

## 可靠性说明

- WebSocket 断线自动重连：优先 Resume 补发漏掉的事件；按官方错误码表决策（连接过期续传 / 会话失效重鉴权 / 下架封禁停止重连并告警）
- 心跳超时自动判死重连；重连退避 1s→60s 带抖动
- 事件重复推送自动去重（同一消息只处理一次）
- Webhook：Ed25519 验签失败返回 401，地址验证（op=13）自动应答；验证请求与事件推送按 `X-Bot-Appid` 头路由
- 事件回调默认运行在 SDK 线程池（`xuanji.event.pool-size`，默认 8），慢 handler 不阻塞收包
- 富媒体上传：独立超时（默认 60s/60s），网络类失败自动重试 1 次；分片直传每片 60s
- access_token 自动刷新；刷新瞬时失败且旧 token 未真过期时继续复用
- 全链路中文日志：`[IN]`/`[OUT]` 带机器人标识、动作中文名、来源（主动/被动）、内容预览；原始报文受 `debug.raw-payload` 开关

## License

[MIT](https://opensource.org/licenses/MIT)
