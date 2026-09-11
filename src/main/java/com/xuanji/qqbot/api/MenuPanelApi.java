package com.xuanji.qqbot.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xuanji.qqbot.http.ApiCall;
import com.xuanji.qqbot.model.menu.MenuConfig;
import com.xuanji.qqbot.model.panel.PanelModels;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 生成分享链接 + 自定义菜单 + 指令面板。
 * 对齐官方 /v2/generate_url_link、/v2/menu、/v2/panels。
 */
public final class MenuPanelApi {
    private final ApiCall api;

    /**
     * @param api 已注入 token 的调用器
     */
    public MenuPanelApi(ApiCall api) {
        this.api = api;
    }

    // ---- 生成分享链接 ----

    /**
     * 生成机器人分享链接（可不带 callback）。
     *
     * @return 分享链接结果
     */
    public ShareUrlLink generateShareUrl() {
        return generateShareUrl(null);
    }

    /**
     * 生成带 callback_data 的分享链接（最长 32 字符）。
     *
     * @param callbackData 用户通过链接添加时透传
     * @return 分享链接
     */
    public ShareUrlLink generateShareUrl(String callbackData) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (callbackData != null && !callbackData.isBlank()) {
            body.put("callback_data", callbackData);
        }
        return api.post("/v2/generate_url_link", body, ShareUrlLink.class);
    }

    // ---- 全局自定义菜单（单聊）----

    /**
     * 查询全局自定义菜单。
     *
     * @return 菜单配置与版本
     */
    public MenuConfig getMenu() {
        return api.get("/v2/menu", MenuConfig.class);
    }

    /**
     * 修改全局自定义菜单（覆盖整份配置）。频控较严（约 5 QPM）。
     *
     * @param menu 菜单
     * @return 新版本号
     */
    public MenuUpdateResult putMenu(MenuConfig.Menu menu) {
        Objects.requireNonNull(menu, "menu");
        Map<String, Object> body = Map.of("menu", menu);
        return api.put("/v2/menu", body, MenuUpdateResult.class);
    }

    // ---- 指令面板 ----

    /**
     * 按场景查询指令面板列表。
     *
     * @param scope  c2c / group / channel / dm
     * @param cursor 分页游标，首次 null
     * @param limit  每页条数，默认 20，最大 50
     * @return 面板页
     */
    public PanelModels.PanelPage listPanels(String scope, String cursor, Integer limit) {
        require(scope, "scope");
        StringBuilder path = new StringBuilder("/v2/panels?scope=").append(ApiCall.pathEncode(scope));
        if (cursor != null && !cursor.isBlank()) {
            path.append("&cursor=").append(ApiCall.pathEncode(cursor));
        }
        if (limit != null) {
            path.append("&limit=").append(limit);
        }
        return api.get(path.toString(), PanelModels.PanelPage.class);
    }

    /**
     * 创建指令面板。
     *
     * @param request 创建请求
     * @return 新 panel_id
     */
    public PanelModels.CreatePanelResult createPanel(PanelModels.CreateOrUpdatePanelRequest request) {
        Objects.requireNonNull(request, "request");
        return api.post("/v2/panels", request, PanelModels.CreatePanelResult.class);
    }

    /**
     * 查询面板详情。
     *
     * @param panelId 面板 ID
     * @return 详情
     */
    public PanelModels.PanelDetail getPanel(String panelId) {
        require(panelId, "panelId");
        return api.get("/v2/panels/" + ApiCall.pathEncode(panelId), PanelModels.PanelDetail.class);
    }

    /**
     * 修改指令面板（覆盖 panel 内容；频控较严）。
     *
     * @param panelId 面板 ID
     * @param request 修改请求（可含 scope/target/关联对象/panel）
     * @return 新版本
     */
    /**
     * 修改指令面板（覆盖 panel 元素与备注；不影响关联用户/群）。
     * 官方 PUT Body 以 panel 为主。
     *
     * @param panelId 面板 ID
     * @param panel   新面板内容
     * @return 新版本号
     */
    public MenuUpdateResult putPanel(String panelId, PanelModels.Panel panel) {
        require(panelId, "panelId");
        Objects.requireNonNull(panel, "panel");
        return api.put("/v2/panels/" + ApiCall.pathEncode(panelId), Map.of("panel", panel),
                MenuUpdateResult.class);
    }

    /**
     * 修改指令面板（兼容旧签名：从 request 取 panel）。
     *
     * @param panelId 面板 ID
     * @param request 创建/修改请求（使用其中 panel 字段）
     * @return 新版本号
     */
    public MenuUpdateResult putPanel(String panelId, PanelModels.CreateOrUpdatePanelRequest request) {
        return putPanel(panelId, request.panel());
    }

    /**
     * 删除指令面板。
     *
     * @param panelId 面板 ID
     */
    public void deletePanel(String panelId) {
        require(panelId, "panelId");
        api.delete("/v2/panels/" + ApiCall.pathEncode(panelId), Object.class);
    }

    /**
     * 修改指令面板关联对象（仅 specific 模式；c2c 操作用户，group 操作群）。
     * PUT /v2/panels/{panel_id}/target
     *
     * @param panelId 面板 ID
     * @param op      add / del
     * @param userOpenids c2c 场景用户 openid，可 null
     * @param groupOpenids group 场景群 openid，可 null
     * @return 执行结果
     */
    public PanelTargetResult updatePanelTarget(String panelId, String op,
                                               List<String> userOpenids, List<String> groupOpenids) {
        require(panelId, "panelId");
        require(op, "op");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("op", op);
        if (userOpenids != null && !userOpenids.isEmpty()) {
            body.put("user_openids", userOpenids);
        }
        if (groupOpenids != null && !groupOpenids.isEmpty()) {
            body.put("group_openids", groupOpenids);
        }
        return api.put("/v2/panels/" + ApiCall.pathEncode(panelId) + "/target",
                body, PanelTargetResult.class);
    }

    /**
     * 给面板添加群关联。
     *
     * @param panelId 面板 ID
     * @param groupOpenids 群 openid
     * @return 结果
     */
    public PanelTargetResult addPanelGroups(String panelId, List<String> groupOpenids) {
        return updatePanelTarget(panelId, "add", null, groupOpenids);
    }

    /**
     * 从面板移除群关联。
     *
     * @param panelId 面板 ID
     * @param groupOpenids 群 openid
     * @return 结果
     */
    public PanelTargetResult removePanelGroups(String panelId, List<String> groupOpenids) {
        return updatePanelTarget(panelId, "del", null, groupOpenids);
    }

    /**
     * 给面板添加用户关联（c2c）。
     *
     * @param panelId 面板 ID
     * @param userOpenids 用户 openid
     * @return 结果
     */
    public PanelTargetResult addPanelUsers(String panelId, List<String> userOpenids) {
        return updatePanelTarget(panelId, "add", userOpenids, null);
    }

    /**
     * 从面板移除用户关联。
     *
     * @param panelId 面板 ID
     * @param userOpenids 用户 openid
     * @return 结果
     */
    public PanelTargetResult removePanelUsers(String panelId, List<String> userOpenids) {
        return updatePanelTarget(panelId, "del", userOpenids, null);
    }

    private static void require(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    /**
     * 生成分享链接响应。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ShareUrlLink(
            @JsonProperty("url_link") String urlLink
    ) {
    }

    /**
     * 菜单/面板修改响应。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MenuUpdateResult(
            @JsonProperty("version") Integer version
    ) {
    }

    /**
     * 面板关联对象修改响应（官方可能无 body，宽松解析）。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PanelTargetResult(
            @JsonProperty("message") String message
    ) {
    }
}
