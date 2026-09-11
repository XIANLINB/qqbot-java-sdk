package com.xuanji.qqbot.model.message;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 群消息中的表情标记解析。
 * <p>
 * 实测 content 形如：{@code <faceType=1,faceId="277",ext="eyJ0ZXh0Ijoi5rGq5rGqIn0=">}
 * 单聊 @ 通道里可能带前导空格，全量通道可能无空格。
 */
public final class FaceTags {
    private static final Pattern FACE = Pattern.compile(
            "<faceType=(\\d+),faceId=\"(\\d+)\",ext=\"([^\"]*)\">");

    private FaceTags() {
    }

    /**
     * 解析 content 中全部表情标记。
     *
     * @param content 消息正文
     * @return 表情列表，无则空
     */
    public static List<Face> parseAll(String content) {
        List<Face> out = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return out;
        }
        Matcher m = FACE.matcher(content);
        while (m.find()) {
            out.add(new Face(
                    Integer.parseInt(m.group(1)),
                    m.group(2),
                    m.group(3)
            ));
        }
        return out;
    }

    /**
     * 去掉 content 中全部表情标记并 trim。
     *
     * @param content 原文
     * @return 纯文本
     */
    public static String strip(String content) {
        if (content == null) {
            return null;
        }
        return FACE.matcher(content).replaceAll("").trim();
    }

    /**
     * 表情元素。
     *
     * @param faceType 类型，实测为 1
     * @param faceId   表情 ID，如 "277"
     * @param ext      扩展字段（Base64 等），可能为空串
     */
    public record Face(int faceType, String faceId, String ext) {
    }
}
