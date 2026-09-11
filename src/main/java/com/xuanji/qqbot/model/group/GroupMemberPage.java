package com.xuanji.qqbot.model.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 群成员分页列表响应。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupMemberPage(
        /** 成员列表，每页最多约 30 条 */
        @JsonProperty("members") List<GroupMember> members,
        /** 下一页游标；空串表示末页 */
        @JsonProperty("next_cursor") String nextCursor
) {
    /**
     * @return 是否还有下一页
     */
    public boolean hasMore() {
        return nextCursor != null && !nextCursor.isBlank();
    }
}
