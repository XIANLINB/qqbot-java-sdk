package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 批量移除群成员响应。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchRemoveMembersResult(
        /** 成功时为 success */
        @JsonProperty("remove_members_result") String removeMembersResult,
        /** 同时拉黑失败的 openid */
        @JsonProperty("add_to_member_blacklist_fail_openids") List<String> addBlacklistFailOpenids
) {
}
