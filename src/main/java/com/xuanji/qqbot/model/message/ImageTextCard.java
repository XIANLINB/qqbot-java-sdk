package com.xuanji.qqbot.model.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 图文卡片消息体（msg_type=8）。
 * 对齐参考 Card：type=tuwen + content{title,description,pic_url,url}。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageTextCard(
        /** 固定 tuwen */
        @JsonProperty("type") String type,
        @JsonProperty("content") Content content
) {
    /** 卡片类型：图文 */
    public static final String TYPE_TUWEN = "tuwen";

    /**
     * 卡片内容。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("pic_url") String picUrl,
            @JsonProperty("url") String url
    ) {
    }

    /**
     * @return 构建器
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * @param title 标题
     * @param desc 描述
     * @param picUrl 图片
     * @param url 跳转
     * @return 图文卡片
     */
    public static ImageTextCard of(String title, String desc, String picUrl, String url) {
        return new ImageTextCard(TYPE_TUWEN, new Content(title, desc, picUrl, url));
    }

    /**
     * 流式构建。
     */
    public static final class Builder {
        private String title;
        private String description;
        private String picUrl;
        private String url;

        /**
         * @param v 标题
         * @return this
         */
        public Builder title(String v) {
            this.title = v;
            return this;
        }

        /**
         * @param v 描述
         * @return this
         */
        public Builder desc(String v) {
            this.description = v;
            return this;
        }

        /**
         * @param v 图片 URL
         * @return this
         */
        public Builder picUrl(String v) {
            this.picUrl = v;
            return this;
        }

        /**
         * @param v 跳转 URL
         * @return this
         */
        public Builder url(String v) {
            this.url = v;
            return this;
        }

        /**
         * @return ImageTextCard
         */
        public ImageTextCard build() {
            return new ImageTextCard(TYPE_TUWEN,
                    new Content(title, description, picUrl, url));
        }
    }
}
