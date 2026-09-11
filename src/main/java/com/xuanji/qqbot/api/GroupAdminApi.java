package com.xuanji.qqbot.api;

import com.xuanji.qqbot.http.ApiCall;
import com.xuanji.qqbot.model.group.BatchRemoveMembersResult;
import com.xuanji.qqbot.model.group.BlacklistOpResult;
import com.xuanji.qqbot.model.group.BlacklistPage;
import com.xuanji.qqbot.model.group.GroupMember;
import com.xuanji.qqbot.model.group.GroupMemberPage;
import com.xuanji.qqbot.model.group.JoinRequestPage;
import com.xuanji.qqbot.model.group.GroupBotState;
import com.xuanji.qqbot.model.group.GroupInfo;
import com.xuanji.qqbot.model.group.RestrictChatSetting;
import com.xuanji.qqbot.model.group.SetMemberMuteState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 群管理动作：查群信息、看机器人群内状态、管成员、黑名单、禁言、入群审批。
 * 经 {@code Bot.group()} 访问。
 * <p>
 * 官方路径（2026-09）：
 * <ul>
 *   <li>GET  /info — 群基本信息</li>
 *   <li>GET  /bot_state — 机器人群内状态</li>
 *   <li>GET  /members — 成员列表</li>
 *   <li>GET  /members/{member_openid} — 成员详情</li>
 *   <li>POST /batch_remove_members — 批量移除</li>
 *   <li>GET/POST /member_blacklist — 黑名单</li>
 *   <li>GET/POST /restrict_chat_setting — 查询/设置禁言</li>
 *   <li>GET  /join_request_list — 入群申请列表</li>
 *   <li>POST /approval_join_request/{member_openid} — 审批</li>
 * </ul>
 * 部分接口官方标注内邀/白名单，无权限常见 err_code=11253。
 * 机器人需具备群管理员身份（禁言/审批类）。
 */
public final class GroupAdminApi {
    private final ApiCall api;

    /**
     * @param api 已注入 token 的调用器
     */
    public GroupAdminApi(ApiCall api) {
        this.api = api;
    }

    // ---- 成员 ----

    /**
     * 获取群成员列表（分页，每次最多约 30 条）。
     *
     * @param groupOpenid 群 OpenID
     * @param cursor      分页游标；首次可传 null/空串，后续传上次 next_cursor
     * @return 成员页
     */
    public GroupMemberPage listMembers(String groupOpenid, String cursor) {
        require(groupOpenid, "groupOpenid");
        String path = "/v2/groups/" + enc(groupOpenid) + "/members";
        if (cursor != null && !cursor.isBlank()) {
            path += "?cursor=" + ApiCall.pathEncode(cursor);
        }
        return api.get(path, GroupMemberPage.class);
    }

    /**
     * 获取指定群成员信息。
     *
     * @param groupOpenid  群 OpenID
     * @param memberOpenid 成员 OpenID（群聊场景 member_openid）
     * @return 成员详情
     */
    public GroupMember getMember(String groupOpenid, String memberOpenid) {
        require(groupOpenid, "groupOpenid");
        require(memberOpenid, "memberOpenid");
        return api.get("/v2/groups/" + enc(groupOpenid) + "/members/" + enc(memberOpenid), GroupMember.class);
    }

    /**
     * 批量移除群成员（单次最多 20 个）。
     *
     * @param groupOpenid     群 OpenID
     * @param memberOpenids   待移除 member_openid 列表
     * @param addToBlacklist  是否同时加入群黑名单
     * @return 移除结果
     */
    public BatchRemoveMembersResult batchRemoveMembers(String groupOpenid, List<String> memberOpenids, boolean addToBlacklist) {
        require(groupOpenid, "groupOpenid");
        Objects.requireNonNull(memberOpenids, "memberOpenids");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("member_openids", memberOpenids);
        body.put("add_to_member_blacklist", addToBlacklist);
        return api.post("/v2/groups/" + enc(groupOpenid) + "/batch_remove_members", body, BatchRemoveMembersResult.class);
    }

    // ---- 黑名单 ----

    /**
     * 查询群黑名单列表（分页）。
     *
     * @param groupOpenid 群 OpenID
     * @param cursor      分页游标
     * @param limit       单页数量，默认 20，最大 100；可 null
     * @return 黑名单页
     */
    public BlacklistPage listBlacklist(String groupOpenid, String cursor, Integer limit) {
        require(groupOpenid, "groupOpenid");
        StringBuilder path = new StringBuilder("/v2/groups/" + enc(groupOpenid) + "/member_blacklist");
        boolean first = true;
        if (cursor != null && !cursor.isBlank()) {
            path.append(first ? '?' : '&').append("cursor=").append(ApiCall.pathEncode(cursor));
            first = false;
        }
        if (limit != null) {
            path.append(first ? '?' : '&').append("limit=").append(limit);
        }
        return api.get(path.toString(), BlacklistPage.class);
    }

    /**
     * 加入群黑名单（目标须已不在群中）。
     *
     * @param groupOpenid   群 OpenID
     * @param memberOpenids 成员 OpenID 列表，最多 20 个
     * @return 失败的 openid 列表等
     */
    public BlacklistOpResult blacklistAdd(String groupOpenid, List<String> memberOpenids) {
        return blacklistOp(groupOpenid, "add", memberOpenids);
    }

    /**
     * 移出群黑名单。
     *
     * @param groupOpenid   群 OpenID
     * @param memberOpenids 成员 OpenID 列表
     * @return 失败的 openid 列表等
     */
    public BlacklistOpResult blacklistDel(String groupOpenid, List<String> memberOpenids) {
        return blacklistOp(groupOpenid, "del", memberOpenids);
    }

    private BlacklistOpResult blacklistOp(String groupOpenid, String op, List<String> memberOpenids) {
        require(groupOpenid, "groupOpenid");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("op", op);
        body.put("member_openids", memberOpenids);
        return api.post("/v2/groups/" + enc(groupOpenid) + "/member_blacklist", body, BlacklistOpResult.class);
    }

    // ---- 群信息 / 机器人群内状态 ----

    /**
     * 获取群基本信息（名称、简介、人数等）。
     * 对应 GET /v2/groups/{group_openid}/info。
     *
     * @param groupOpenid 群 OpenID
     * @return 群名称/简介/分类/标签/人数
     */
    public GroupInfo getGroupInfo(String groupOpenid) {
        require(groupOpenid, "groupOpenid");
        return api.get("/v2/groups/" + enc(groupOpenid) + "/info", GroupInfo.class);
    }

    /**
     * 获取机器人在该群的状态：角色、入群时间、是否允许主动推送、接收消息设置。
     * 对应 GET /v2/groups/{group_openid}/bot_state。
     *
     * @param groupOpenid 群 OpenID
     * @return 机器人群内状态
     */
    public GroupBotState getBotState(String groupOpenid) {
        require(groupOpenid, "groupOpenid");
        return api.get("/v2/groups/" + enc(groupOpenid) + "/bot_state", GroupBotState.class);
    }

    // ---- 禁言 ----

    /**
     * 查询禁言状态：全员禁言模式 + 当前被禁言成员列表。
     * 机器人需群管理员身份。
     *
     * @param groupOpenid 群 OpenID
     * @return global_rule + members
     */
    public RestrictChatSetting getRestrictChatSetting(String groupOpenid) {
        require(groupOpenid, "groupOpenid");
        return api.get("/v2/groups/" + enc(groupOpenid) + "/restrict_chat_setting", RestrictChatSetting.class);
    }

    /**
     * 批量设置成员禁言。每项 op=add/update/del；单次 ≤20；最长约 30 天。
     * 仅可禁言普通成员（不能禁言群主/管理员/机器人）。
     *
     * @param groupOpenid 群 OpenID
     * @param members     操作列表，见 {@link SetMemberMuteState}
     */
    public void setMemberMute(String groupOpenid, List<SetMemberMuteState> members) {
        require(groupOpenid, "groupOpenid");
        Objects.requireNonNull(members, "members");
        Map<String, Object> body = Map.of("members", members);
        api.post("/v2/groups/" + enc(groupOpenid) + "/restrict_chat_setting", body, Object.class);
    }

    /**
     * 禁言指定成员到某一时刻。
     *
     * @param groupOpenid  群 OpenID
     * @param memberOpenid 成员 OpenID（普通成员）
     * @param expireAt     到期 RFC3339，如 2026-08-05T11:23:05+08:00
     */
    public void muteMember(String groupOpenid, String memberOpenid, String expireAt) {
        setMemberMute(groupOpenid, List.of(SetMemberMuteState.add(memberOpenid, expireAt)));
    }

    /**
     * 禁言指定成员若干时长（从当前时刻起算，最长 30 天）。
     *
     * @param groupOpenid  群 OpenID
     * @param memberOpenid 成员 OpenID
     * @param duration     时长
     */
    public void muteMember(String groupOpenid, String memberOpenid, java.time.Duration duration) {
        setMemberMute(groupOpenid, List.of(SetMemberMuteState.add(memberOpenid, duration)));
    }

    /**
     * 解除禁言（op=del）。
     *
     * @param groupOpenid  群 Open ID
     * @param memberOpenid 成员 OpenID
     */
    public void unmuteMember(String groupOpenid, String memberOpenid) {
        setMemberMute(groupOpenid, List.of(SetMemberMuteState.unmute(memberOpenid)));
    }

    // ---- 入群审批 ----

    /**
     * 拉取入群申请列表（分页）。
     *
     * @param groupOpenid 群 OpenID
     * @param cursor      分页游标
     * @param limit       单页数量，默认 20，最大 50；可 null
     * @return 申请列表页
     */
    public JoinRequestPage listJoinRequests(String groupOpenid, String cursor, Integer limit) {
        require(groupOpenid, "groupOpenid");
        StringBuilder path = new StringBuilder("/v2/groups/" + enc(groupOpenid) + "/join_request_list");
        boolean first = true;
        if (cursor != null && !cursor.isBlank()) {
            path.append(first ? '?' : '&').append("cursor=").append(ApiCall.pathEncode(cursor));
            first = false;
        }
        if (limit != null) {
            path.append(first ? '?' : '&').append("limit=").append(limit);
        }
        return api.get(path.toString(), JoinRequestPage.class);
    }

    /**
     * 通过入群申请。
     *
     * @param groupOpenid   群 OpenID
     * @param memberOpenid  申请人 OpenID
     * @param joinRequestId 申请 ID（列表中的 join_request_id）
     */
    public void approveJoinRequest(String groupOpenid, String memberOpenid, String joinRequestId) {
        approval(groupOpenid, memberOpenid, "approve", joinRequestId, null, false);
    }

    /**
     * 拒绝入群申请。
     *
     * @param groupOpenid    群 OpenID
     * @param memberOpenid   申请人 OpenID
     * @param joinRequestId  申请 ID
     * @param rejectReason   拒绝理由
     * @param addToBlacklist 是否同时拉黑
     */
    public void declineJoinRequest(String groupOpenid, String memberOpenid, String joinRequestId,
                                   String rejectReason, boolean addToBlacklist) {
        approval(groupOpenid, memberOpenid, "decline", joinRequestId, rejectReason, addToBlacklist);
    }

    private void approval(String groupOpenid, String memberOpenid, String op, String joinRequestId,
                          String rejectReason, boolean addToBlacklist) {
        require(groupOpenid, "groupOpenid");
        require(memberOpenid, "memberOpenid");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("op", op);
        if (joinRequestId != null) {
            body.put("join_request_id", joinRequestId);
        }
        if (rejectReason != null) {
            body.put("reject_reason", rejectReason);
        }
        body.put("add_to_member_blacklist", addToBlacklist);
        api.post("/v2/groups/" + enc(groupOpenid) + "/approval_join_request/" + enc(memberOpenid),
                body, Object.class);
    }

    private static String enc(String s) {
        return ApiCall.pathEncode(s);
    }

    private static void require(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }
}
