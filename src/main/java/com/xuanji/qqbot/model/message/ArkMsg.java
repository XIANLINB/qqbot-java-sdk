package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ark 卡片消息体（msg_type=3）。
 * <p>
 * 对齐参考实现 Ark23/24/37 模板：
 * <ul>
 *   <li>24：大图/图文（#TITLE# #DESC# #IMG# #LINK# …）</li>
 *   <li>23：列表（#LIST# 等）</li>
 *   <li>37：大图通知（#METATITLE# 等）</li>
 * </ul>
 * kv 中 key/value 均为字符串。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArkMsg(
        /** 模板 ID，如 23/24/37 */
        @JsonProperty("template_id") Integer templateId,
        /** 键值列表 */
        @JsonProperty("kv") List<Kv> kv
) {
    /**
     * 键值对。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Kv(
            @JsonProperty("key") String key,
            @JsonProperty("value") String value
    ) {
    }

    /**
     * @param templateId 模板
     * @param kv 列表
     * @return Ark 消息
     */
    public static ArkMsg of(int templateId, List<Kv> kv) {
        return new ArkMsg(templateId, kv);
    }

    /** Ark24 图文/大图构建器（参考 Ark.Ark24）。 */
    public static final class Ark24 {
        private String title;
        private String desc;
        private String prompt;
        private String img;
        private String link;
        private String subtitle;
        private String metaDesc;

        /**
         * @return 构建器
         */
        public static Ark24 create() {
            return new Ark24();
        }

        /**
         * @param v 标题
         * @return this
         */
        public Ark24 title(String v) {
            this.title = v;
            return this;
        }

        /**
         * @param v 描述
         * @return this
         */
        public Ark24 desc(String v) {
            this.desc = v;
            return this;
        }

        /**
         * @param v 提示
         * @return this
         */
        public Ark24 prompt(String v) {
            this.prompt = v;
            return this;
        }

        /**
         * @param v 图片 URL
         * @return this
         */
        public Ark24 img(String v) {
            this.img = v;
            return this;
        }

        /**
         * @param v 跳转链接
         * @return this
         */
        public Ark24 link(String v) {
            this.link = v;
            return this;
        }

        /**
         * @param v 子标题
         * @return this
         */
        public Ark24 subtitle(String v) {
            this.subtitle = v;
            return this;
        }

        /**
         * @param v meta 描述
         * @return this
         */
        public Ark24 metaDesc(String v) {
            this.metaDesc = v;
            return this;
        }

        /**
         * @return ArkMsg
         */
        public ArkMsg build() {
            List<Kv> kvs = new ArrayList<>();
            if (desc != null) {
                kvs.add(new Kv("#DESC#", desc));
            }
            if (prompt != null) {
                kvs.add(new Kv("#PROMPT#", prompt));
            }
            if (title != null) {
                kvs.add(new Kv("#TITLE#", title));
            }
            if (metaDesc != null) {
                kvs.add(new Kv("#METADESC#", metaDesc));
            }
            if (img != null) {
                kvs.add(new Kv("#IMG#", img));
            }
            if (link != null) {
                kvs.add(new Kv("#LINK#", link));
            }
            if (subtitle != null) {
                kvs.add(new Kv("#SUBTITLE#", subtitle));
            }
            return new ArkMsg(24, kvs);
        }
    }

    /** Ark23 列表卡片构建器。 */
    public static final class Ark23 {
        private String desc;
        private String prompt;
        private String title;
        private String link;
        private String metaTitle;
        private String metaDesc;
        private String metaIcon;
        private final List<String> items = new ArrayList<>();

        /**
         * @return 构建器
         */
        public static Ark23 create() {
            return new Ark23();
        }

        /**
         * @param v 标题
         * @return this
         */
        public Ark23 title(String v) {
            this.title = v;
            return this;
        }

        /**
         * @param v 描述
         * @return this
         */
        public Ark23 desc(String v) {
            this.desc = v;
            return this;
        }

        /**
         * @param v 提示
         * @return this
         */
        public Ark23 prompt(String v) {
            this.prompt = v;
            return this;
        }

        /**
         * @param v 链接
         * @return this
         */
        public Ark23 link(String v) {
            this.link = v;
            return this;
        }

        /**
         * @param v meta 标题
         * @return this
         */
        public Ark23 metaTitle(String v) {
            this.metaTitle = v;
            return this;
        }

        /**
         * @param v meta 描述
         * @return this
         */
        public Ark23 metaDesc(String v) {
            this.metaDesc = v;
            return this;
        }

        /**
         * @param v meta 图标
         * @return this
         */
        public Ark23 metaIcon(String v) {
            this.metaIcon = v;
            return this;
        }

        /**
         * @param text 列表项文本
         * @return this
         */
        public Ark23 item(String text) {
            items.add(text);
            return this;
        }

        /**
         * @param text 列表项文本
         * @param link 链接
         * @return this
         */
        public Ark23 item(String text, String link) {
            items.add(text + "||" + link);
            return this;
        }

        /**
         * @return ArkMsg
         */
        public ArkMsg build() {
            StringBuilder list = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    list.append("|");
                }
                String item = items.get(i);
                int sep = item.indexOf("||");
                list.append(sep >= 0 ? item.substring(0, sep) : item);
            }
            List<Kv> kvs = new ArrayList<>();
            if (!list.isEmpty()) {
                kvs.add(new Kv("#LIST#", list.toString()));
            }
            if (desc != null) {
                kvs.add(new Kv("#DESC#", desc));
            }
            if (prompt != null) {
                kvs.add(new Kv("#PROMPT#", prompt));
            }
            if (title != null) {
                kvs.add(new Kv("#TITLE#", title));
            }
            if (link != null) {
                kvs.add(new Kv("#LINK#", link));
            }
            if (metaTitle != null) {
                kvs.add(new Kv("#METATITLE#", metaTitle));
            }
            if (metaDesc != null) {
                kvs.add(new Kv("#METADESC#", metaDesc));
            }
            if (metaIcon != null) {
                kvs.add(new Kv("#METAICON#", metaIcon));
            }
            return new ArkMsg(23, kvs);
        }
    }

    /** Ark37 大图通知卡片构建器。 */
    public static final class Ark37 {
        private String prompt;
        private String metaTitle;
        private String metaSubtitle;
        private String metaCover;
        private String metaUrl;

        /**
         * @return 构建器
         */
        public static Ark37 create() {
            return new Ark37();
        }

        /**
         * @param v 提示
         * @return this
         */
        public Ark37 prompt(String v) {
            this.prompt = v;
            return this;
        }

        /**
         * @param v 主标题
         * @return this
         */
        public Ark37 metaTitle(String v) {
            this.metaTitle = v;
            return this;
        }

        /**
         * @param v 副标题
         * @return this
         */
        public Ark37 metaSubtitle(String v) {
            this.metaSubtitle = v;
            return this;
        }

        /**
         * @param v 封面图
         * @return this
         */
        public Ark37 metaCover(String v) {
            this.metaCover = v;
            return this;
        }

        /**
         * @param v 跳转 URL
         * @return this
         */
        public Ark37 metaUrl(String v) {
            this.metaUrl = v;
            return this;
        }

        /**
         * @return ArkMsg
         */
        public ArkMsg build() {
            List<Kv> kvs = new ArrayList<>();
            if (prompt != null) {
                kvs.add(new Kv("#PROMPT#", prompt));
            }
            if (metaTitle != null) {
                kvs.add(new Kv("#METATITLE#", metaTitle));
            }
            if (metaSubtitle != null) {
                kvs.add(new Kv("#METASUBTITLE#", metaSubtitle));
            }
            if (metaCover != null) {
                kvs.add(new Kv("#METACOVER#", metaCover));
            }
            if (metaUrl != null) {
                kvs.add(new Kv("#METAURL#", metaUrl));
            }
            return new ArkMsg(37, kvs);
        }
    }

    /**
     * 从 Map 快速生成（调试用）。
     *
     * @param templateId 模板
     * @param map key→value
     * @return ArkMsg
     */
    public static ArkMsg fromMap(int templateId, Map<String, String> map) {
        List<Kv> kvs = new ArrayList<>();
        if (map != null) {
            for (Map.Entry<String, String> e : map.entrySet()) {
                kvs.add(new Kv(e.getKey(), e.getValue()));
            }
        }
        return new ArkMsg(templateId, kvs);
    }

    /**
     * @return 转 Map，便于调试
     */
    public Map<String, Object> asMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("template_id", templateId);
        List<Map<String, String>> list = new ArrayList<>();
        if (kv != null) {
            for (Kv k : kv) {
                Map<String, String> one = new LinkedHashMap<>();
                one.put("key", k.key());
                one.put("value", k.value());
                list.add(one);
            }
        }
        m.put("kv", list);
        return m;
    }
}
