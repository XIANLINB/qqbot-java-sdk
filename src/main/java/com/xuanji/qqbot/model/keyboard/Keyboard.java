package com.xuanji.qqbot.model.keyboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 内嵌键盘：模板 id 或自定义 content.rows，二者互斥。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Keyboard(
        @JsonProperty("id") String id,
        @JsonProperty("content") KeyboardContent content
) {
    public static Keyboard ofTemplate(String templateId) {
        return new Keyboard(templateId, null);
    }

    public static Keyboard ofRows(List<Row> rows) {
        return new Keyboard(null, new KeyboardContent(rows));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 内嵌键盘构建器：官方最多 5 行 × 5 列。
     */
    public static final class Builder {
        private final List<Row> rows = new ArrayList<>();

        /**
         * @param config 配置一行按钮
         * @return this
         */
        public Builder row(java.util.function.Consumer<RowBuilder> config) {
            RowBuilder rb = new RowBuilder();
            config.accept(rb);
            rows.add(rb.build());
            return this;
        }

        /**
         * @return 键盘（校验 ≤5 行，每行 ≤5 按钮）
         */
        public Keyboard build() {
            if (rows.size() > 5) {
                throw new IllegalArgumentException("内嵌键盘最多 5 行");
            }
            for (Row r : rows) {
                if (r.buttons() != null && r.buttons().size() > 5) {
                    throw new IllegalArgumentException("内嵌键盘每行最多 5 个按钮");
                }
            }
            return ofRows(List.copyOf(rows));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeyboardContent(@JsonProperty("rows") List<Row> rows) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Row(@JsonProperty("buttons") List<Button> buttons) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Button(
            @JsonProperty("id") String id,
            @JsonProperty("render_data") RenderData renderData,
            @JsonProperty("action") Action action,
            @JsonProperty("group_id") String groupId
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RenderData(
            @JsonProperty("label") String label,
            @JsonProperty("visited_label") String visitedLabel,
            @JsonProperty("style") Integer style
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Action(
            @JsonProperty("type") Integer type,
            @JsonProperty("permission") Permission permission,
            @JsonProperty("data") String data,
            @JsonProperty("unsupport_tips") String unsupportTips,
            @JsonProperty("enter") Boolean enter,
            @JsonProperty("reply") Boolean reply,
            @JsonProperty("anchor") Integer anchor,
            @JsonProperty("modal") Modal modal
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Permission(
            @JsonProperty("type") Integer type,
            @JsonProperty("specify_user_ids") List<String> specifyUserIds,
            @JsonProperty("specify_role_ids") List<String> specifyRoleIds
    ) {
        public static Permission everyone() {
            return new Permission(2, null, null);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Modal(
            @JsonProperty("content") String content,
            @JsonProperty("confirm_text") String confirmText,
            @JsonProperty("cancel_text") String cancelText
    ) {
    }

    public static final class RowBuilder {
        private final List<Button> buttons = new ArrayList<>();

        public RowBuilder button(java.util.function.Consumer<ButtonBuilder> config) {
            ButtonBuilder bb = new ButtonBuilder();
            config.accept(bb);
            buttons.add(bb.build());
            return this;
        }

        Row build() {
            return new Row(List.copyOf(buttons));
        }
    }

    /**
     * 按钮构建器。
     * style：0=灰线框 1=蓝线框 2=白字 3=蓝底白字（实测仅 4 档）。
     * permission 必须显式设置，默认所有人 type=2；否则客户端无权限点击。
     * visited_label：点击后文案（跳转/回调点击后 label 会清空，建议配置）。
     */
    public static final class ButtonBuilder {
        private String id = "btn";
        private String label;
        private String visitedLabel;
        private Integer style = 1;
        private Integer actionType = 2;
        private String data;
        private Boolean enter;
        private Boolean reply;
        private String unsupportTips;
        private Permission permission = Permission.everyone();

        public ButtonBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ButtonBuilder label(String label) {
            this.label = label;
            return this;
        }

        /** 点击后显示文字（强烈建议配置）。 */
        public ButtonBuilder visitedLabel(String visitedLabel) {
            this.visitedLabel = visitedLabel;
            return this;
        }

        /** 0灰 1蓝线框 2白字 3蓝底白字。 */
        public ButtonBuilder style(int style) {
            this.style = style;
            return this;
        }

        /** 指令按钮：输入框插入 data；enter=true 点击即发送（仅单聊）。 */
        public ButtonBuilder command(String data, boolean enter) {
            this.actionType = 2;
            this.data = data;
            this.enter = enter;
            return this;
        }

        /** 回调按钮：触发 INTERACTION_CREATE，data 传后台。 */
        public ButtonBuilder callback(String data) {
            this.actionType = 1;
            this.data = data;
            return this;
        }

        /** 跳转按钮：data 为 https 链接。 */
        public ButtonBuilder jump(String url) {
            this.actionType = 0;
            this.data = url;
            return this;
        }

        public ButtonBuilder replyOnCommand(boolean reply) {
            this.reply = reply;
            return this;
        }

        public ButtonBuilder unsupportTips(String tips) {
            this.unsupportTips = tips;
            return this;
        }

        public ButtonBuilder permission(Permission permission) {
            this.permission = permission;
            return this;
        }

        Button build() {
            return new Button(
                    id,
                    new RenderData(label, visitedLabel != null ? visitedLabel : label, style),
                    new Action(actionType, permission, data, unsupportTips, enter, reply, null, null),
                    null
            );
        }
    }
}
