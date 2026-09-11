package com.xuanji.qqbot.model.panel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 指令面板相关模型。对齐官方 /v2/panels。
 */
public final class PanelModels {
    private PanelModels() {
    }

    /** scope */
    public static final String SCOPE_C2C = "c2c";
    /** scope */
    public static final String SCOPE_GROUP = "group";
    /** scope */
    public static final String SCOPE_CHANNEL = "channel";
    /** scope */
    public static final String SCOPE_DM = "dm";
    /** target_type */
    public static final String TARGET_ALL = "all";
    /** target_type */
    public static final String TARGET_SPECIFIC = "specific";

    /**
     * 面板内容。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Panel(
            @JsonProperty("items") List<PanelItem> items,
            @JsonProperty("remark") String remark,
            @JsonProperty("version") Integer version
    ) {
        /**
         * @param items 元素
         * @param remark 备注
         * @return 面板
         */
        public static Panel of(List<PanelItem> items, String remark) {
            return new Panel(items, remark, null);
        }
    }

    /**
     * 面板元素。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PanelItem(
            @JsonProperty("name") String name,
            @JsonProperty("desc") String desc,
            /** command | link */
            @JsonProperty("type") String type,
            @JsonProperty("only_admin") Boolean onlyAdmin,
            @JsonProperty("link") String link
    ) {
        /** type=command */
        public static final String TYPE_COMMAND = "command";
        /** type=link */
        public static final String TYPE_LINK = "link";

        /**
         * @param name 指令名，点击填入输入框
         * @param desc 描述
         * @return 指令项
         */
        public static PanelItem command(String name, String desc) {
            return new PanelItem(name, desc, TYPE_COMMAND, null, null);
        }

        /**
         * @param name 展示名
         * @param desc 描述
         * @param link https 链接
         * @return 链接项
         */
        public static PanelItem link(String name, String desc, String link) {
            return new PanelItem(name, desc, TYPE_LINK, null, link);
        }

        /**
         * @param onlyAdmin 是否仅管理员
         * @return 本项副本
         */
        public PanelItem onlyAdmin(boolean onlyAdmin) {
            return new PanelItem(name, desc, type, onlyAdmin, link);
        }
    }

    /**
     * 列表中的面板记录。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PanelRecord(
            @JsonProperty("panel_id") String panelId,
            @JsonProperty("scope") String scope,
            @JsonProperty("target_type") String targetType,
            @JsonProperty("panel") Panel panel,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("updated_at") String updatedAt,
            @JsonProperty("version") Integer version
    ) {
    }

    /**
     * 面板分页列表。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PanelPage(
            @JsonProperty("records") List<PanelRecord> records,
            @JsonProperty("next_cursor") String nextCursor,
            @JsonProperty("is_end") Boolean isEnd
    ) {
    }

    /**
     * 面板详情。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PanelDetail(
            @JsonProperty("panel_id") String panelId,
            @JsonProperty("scope") String scope,
            @JsonProperty("target_type") String targetType,
            @JsonProperty("panel") Panel panel,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("updated_at") String updatedAt,
            @JsonProperty("version") Integer version,
            @JsonProperty("user_openids") List<String> userOpenids,
            @JsonProperty("group_openids") List<String> groupOpenids
    ) {
    }

    /**
     * 创建面板响应。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreatePanelResult(
            @JsonProperty("panel_id") String panelId
    ) {
    }

    /**
     * 创建/修改面板请求体。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateOrUpdatePanelRequest(
            @JsonProperty("scope") String scope,
            @JsonProperty("target_type") String targetType,
            @JsonProperty("user_openids") List<String> userOpenids,
            @JsonProperty("group_openids") List<String> groupOpenids,
            @JsonProperty("panel") Panel panel
    ) {
        /**
         * @param scope c2c/group/channel/dm
         * @param targetType all/specific
         * @param panel 内容
         * @return 请求
         */
        public static CreateOrUpdatePanelRequest of(String scope, String targetType, Panel panel) {
            return new CreateOrUpdatePanelRequest(scope, targetType, null, null, panel);
        }

        /**
         * @param groupOpenids 群列表
         * @return 带群关联
         */
        public CreateOrUpdatePanelRequest withGroups(List<String> groupOpenids) {
            return new CreateOrUpdatePanelRequest(scope, targetType, null, groupOpenids, panel);
        }

        /**
         * @param userOpenids 用户列表
         * @return 带用户关联
         */
        public CreateOrUpdatePanelRequest withUsers(List<String> userOpenids) {
            return new CreateOrUpdatePanelRequest(scope, targetType, userOpenids, null, panel);
        }
    }
}
