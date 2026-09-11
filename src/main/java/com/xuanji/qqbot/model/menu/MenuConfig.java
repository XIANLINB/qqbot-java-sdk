package com.xuanji.qqbot.model.menu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 全局自定义菜单（单聊底部）。
 * GET/PUT /v2/menu
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MenuConfig(
        @JsonProperty("menu") Menu menu,
        @JsonProperty("version") Integer version
) {
    /**
     * 菜单主体。
     *
     * @param items 菜单项，最多 10 个
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Menu(
            @JsonProperty("items") List<MenuItem> items
    ) {
        /**
         * @return 快捷构造
         */
        public static Menu of(List<MenuItem> items) {
            return new Menu(items);
        }
    }

    /**
     * 一级菜单项。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MenuItem(
            /** 按钮名，约 ≤10 字符宽度（中文算 2） */
            @JsonProperty("name") String name,
            /** switch | send_message | link | menu */
            @JsonProperty("type") String type,
            /** type=menu 时的子菜单，最多 5 个 */
            @JsonProperty("sub_menu_items") List<SubMenuItem> subMenuItems,
            /** type=send_message */
            @JsonProperty("send_message") String sendMessage,
            /** type=link，https:// */
            @JsonProperty("link") String link,
            /** type=switch */
            @JsonProperty("switch") SwitchConfig switchConfig
    ) {
        public static final String TYPE_SWITCH = "switch";
        public static final String TYPE_SEND_MESSAGE = "send_message";
        public static final String TYPE_LINK = "link";
        public static final String TYPE_MENU = "menu";

        /**
         * @param name 名称
         * @param message 发送内容
         * @return 发送消息按钮
         */
        public static MenuItem sendMessage(String name, String message) {
            return new MenuItem(name, TYPE_SEND_MESSAGE, null, message, null, null);
        }

        /**
         * @param name 名称
         * @param link https 链接
         * @return 链接按钮
         */
        public static MenuItem link(String name, String link) {
            return new MenuItem(name, TYPE_LINK, null, null, link, null);
        }

        /**
         * @param name 名称
         * @param switchId 开关 id
         * @param defaultOn 默认是否开
         * @return 开关按钮
         */
        public static MenuItem switchOn(String name, String switchId, boolean defaultOn) {
            return new MenuItem(name, TYPE_SWITCH, null, null, null,
                    new SwitchConfig(switchId, defaultOn));
        }

        /**
         * @param name 名称
         * @param sub 子菜单
         * @return 折叠菜单
         */
        public static MenuItem subMenu(String name, List<SubMenuItem> sub) {
            return new MenuItem(name, TYPE_MENU, sub, null, null, null);
        }
    }

    /**
     * 二级菜单项（仅 send_message / link）。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubMenuItem(
            @JsonProperty("name") String name,
            @JsonProperty("type") String type,
            @JsonProperty("send_message") String sendMessage,
            @JsonProperty("link") String link
    ) {
        /**
         * @param name 名称
         * @param message 内容
         * @return 子菜单发送项
         */
        public static SubMenuItem sendMessage(String name, String message) {
            return new SubMenuItem(name, MenuItem.TYPE_SEND_MESSAGE, message, null);
        }

        /**
         * @param name 名称
         * @param link 链接
         * @return 子菜单链接
         */
        public static SubMenuItem link(String name, String link) {
            return new SubMenuItem(name, MenuItem.TYPE_LINK, null, link);
        }
    }

    /**
     * 开关配置。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SwitchConfig(
            @JsonProperty("switch_id") String switchId,
            @JsonProperty("default") Boolean defaultOn
    ) {
    }
}
