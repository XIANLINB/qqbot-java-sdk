package com.xuanji.qqbot.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.http.ApiCall;

/**
 * 互动事件响应（PUT /interactions/{interaction_id}）。
 * 收到 type=11/12 的 INTERACTION_CREATE 后必须调用，否则客户端一直 loading。
 * 同一 interaction_id 仅能回应一次。
 */
public final class InteractionApi {
    private final ApiCall api;

    /**
     * @param api 已注入 token 的调用器
     */
    public InteractionApi(ApiCall api) {
        this.api = api;
    }

    /**
     * 响应互动事件。
     * <p>
     * <b>响应超时：指令回调类场景为 3 秒</b>，收到事件后应尽快调用；
     * 同一 interaction_id 只能回应一次，超时后失效。
     *
     * @param interactionId 事件 d.id（不带 INTERACTION_CREATE: 前缀）
     * @param code 0=成功 1=失败 2=频繁 3=重复 4=无权限 5=仅管理员
     */
    public void respond(String interactionId, int code) {
        if (interactionId == null || interactionId.isBlank()) {
            throw new IllegalArgumentException("interactionId 不能为空");
        }
        api.put("/interactions/" + ApiCall.pathEncode(interactionId),
                new RespondBody(code), Object.class);
    }

    /**
     * 以成功码响应。
     *
     * @param interactionId 互动 id
     */
    public void respondSuccess(String interactionId) {
        respond(interactionId, 0);
    }

    /**
     * 响应体。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RespondBody(
            @JsonProperty("code") Integer code
    ) {
    }
}
