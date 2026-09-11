# qqbot-sdk

轻量 **QQ 机器人 Java SDK**（OpenAPI api-v2）。单模块、最小依赖，只提供客户端能力，不是 Bot 框架。

## 依赖

- Java 17+
- Jackson、SLF4J、BouncyCastle（Webhook Ed25519）

## 快速使用

```java
try (QqBot bot = QqBot.builder()
        .appId(System.getenv("QQBOT_APP_ID"))
        .appSecret(System.getenv("QQBOT_APP_SECRET"))
        .build()) {

    bot.events().on(GroupAtMessageCreate.class, msg ->
            bot.api().message().postGroupText(
                    msg.groupOpenid(),
                    "echo: " + msg.content(),
                    Reply.to(msg.id()).seq(1)));

    bot.connectGateway(Intents.groupAndC2c());
    Thread.currentThread().join();
}
```

## Webhook（不绑 Spring）

```java
WebhookCodec codec = WebhookCodec.of(appSecret);
if (codec.isValidation(rawBody)) {
    return codec.validationResponse(rawBody); // op=13
}
if (!codec.verify(signatureHeader, timestampHeader, rawBody)) {
    throw new IllegalStateException("bad signature");
}
codec.dispatch(rawBody, bot.events());
return WebhookCodec.callbackAck(); // {"op":12}
```

## 能力（0.1+）

| 模块 | 状态 |
|------|------|
| access_token 自动刷新 | ✅ |
| 单聊/群聊发文本、Markdown、Keyboard | ✅ |
| 被动回复 / 撤回 | ✅ |
| URL / Base64 / 分片 富媒体上传 | ✅ |
| 流式单聊消息 | ✅ |
| 群管理（成员/黑名单/禁言/入群审批） | ✅（部分内邀权限） |
| WebSocket 事件 + 断线 Resume 重连 | ✅ |
| Webhook 验签 + 事件 | ✅ |
| 事件 `on(Class)` / `on(type)` | ✅ |
| 频道（Guild）能力 | ❌ 不做 |

开放平台文档摘要见 [`docs/`](docs/)。

## 构建

```bash
export JAVA_HOME=...   # JDK 17+
mvn test
mvn package
```

## 设计说明

见 [`docs/design/JAVA-SDK-DESIGN.md`](docs/design/JAVA-SDK-DESIGN.md)。
