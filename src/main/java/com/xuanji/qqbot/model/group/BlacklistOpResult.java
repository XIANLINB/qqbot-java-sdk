package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 黑名单操作响应。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BlacklistOpResult(
        /** 操作失败的 openid */
        @JsonProperty("fail_openids") List<String> failOpenids
) {
}
