package com.xuanji.qqbot.model.message;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * QQ Markdown 扩展标签：指令操作与 @。
 * <ul>
 *   <li>回车指令：{@code <qqbot-cmd-enter text="..."/>} 点击后直接发送（仅单聊）</li>
 *   <li>参数指令：{@code <qqbot-cmd-input text="..." show="..." reference="false"/>} 点击后插入输入框</li>
 *   <li>@ 成员：{@code <@member_openid>}（群聊 member_openid）</li>
 * </ul>
 * text/show 最大 100 字符，需 urlencode。
 */
public final class MarkdownTags {
    private static final int MAX_LEN = 100;

    private MarkdownTags() {
    }

    /**
     * @param text 原文
     * @return urlencoded
     */
    public static String urlEncode(String text) {
        return URLEncoder.encode(text == null ? "" : text, StandardCharsets.UTF_8);
    }

    private static String check(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("指令文本不能为空");
        }
        if (s.length() > MAX_LEN) {
            throw new IllegalArgumentException("指令文本超过 100 字符限制");
        }
        return s;
    }

    /**
     * 回车指令：点击后直接发送（仅单聊可用）。
     *
     * @param text 发送文本（原文，自动 urlencode）
     * @return 标签
     */
    public static String cmdEnter(String text) {
        return "<qqbot-cmd-enter text=\"" + urlEncode(check(text)) + "\" />";
    }

    /**
     * 参数指令：点击后插入输入框（默认 show=text，不带引用）。
     *
     * @param text 插入文本
     * @return 标签
     */
    public static String cmdInput(String text) {
        return cmdInput(text, null, false);
    }

    /**
     * 参数指令完整版。
     *
     * @param text 插入文本
     * @param show 展示文本，null 用 text
     * @param reference 是否带原文引用
     * @return 标签
     */
    public static String cmdInput(String text, String show, boolean reference) {
        String t = urlEncode(check(text));
        String s = show == null || show.isBlank() ? t : urlEncode(check(show));
        return "<qqbot-cmd-input text=\"" + t + "\" show=\"" + s + "\" reference=\"" + reference + "\" />";
    }

    /**
     * 群聊 @（member_openid）。
     *
     * @param memberOpenid 群成员 OpenID
     * @return 标签
     */
    public static String at(String memberOpenid) {
        if (memberOpenid == null || memberOpenid.isBlank()) {
            throw new IllegalArgumentException("openid 不能为空");
        }
        return "<@" + memberOpenid + ">";
    }

    /**
     * 拼接：text + 标签。
     *
     * @param prefix 前缀文本
     * @param tag 标签
     * @return 完整片段
     */
    public static String with(String prefix, String tag) {
        return (prefix == null ? "" : prefix) + tag;
    }
}
