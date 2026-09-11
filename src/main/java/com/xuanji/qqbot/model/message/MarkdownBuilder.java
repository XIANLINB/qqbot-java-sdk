package com.xuanji.qqbot.model.message;

/**
 * QQ Markdown 构建器（链式，对齐实测手册与参考实现）。
 * <p>
 * 生成纯文本，可 {@code bot.replyMarkdown(event, md.build())}。
 * 注意：返回 String 给「通用 reply」会走纯文本通道导致不渲染，
 * 必须走 replyMarkdown / msg_type=2。
 */
public class MarkdownBuilder {
    private final StringBuilder sb = new StringBuilder();

    /**
     * @return 构建器
     */
    public static MarkdownBuilder create() {
        return new MarkdownBuilder();
    }

    /**
     * @param text 一级标题
     * @return this
     */
    public MarkdownBuilder h1(String text) {
        sb.append("# ").append(text).append("\n\n");
        return this;
    }

    /**
     * @param text 二级标题
     * @return this
     */
    public MarkdownBuilder h2(String text) {
        sb.append("## ").append(text).append("\n\n");
        return this;
    }

    /**
     * @param text 三级标题
     * @return this
     */
    public MarkdownBuilder h3(String text) {
        sb.append("### ").append(text).append("\n\n");
        return this;
    }

    /**
     * @param text 单行
     * @return this
     */
    public MarkdownBuilder line(String text) {
        sb.append(text).append("\n");
        return this;
    }

    /**
     * @param text 段落
     * @return this
     */
    public MarkdownBuilder text(String text) {
        sb.append(text).append("\n\n");
        return this;
    }

    /**
     * @return 空行
     */
    public MarkdownBuilder blank() {
        sb.append("\n");
        return this;
    }

    /**
     * @return 软换行（两空格+换行）
     */
    public MarkdownBuilder br() {
        sb.append("  \n");
        return this;
    }

    /**
     * @param text 原样追加
     * @return this
     */
    public MarkdownBuilder raw(String text) {
        if (text != null) {
            sb.append(text);
        }
        return this;
    }

    /**
     * @param text 加粗
     * @return this
     */
    public MarkdownBuilder bold(String text) {
        sb.append("**").append(text).append("**");
        return this;
    }

    /**
     * @param title 键
     * @param content 值
     * @return this
     */
    public MarkdownBuilder bold(String title, String content) {
        sb.append("**").append(title).append("**: ").append(content).append("\n\n");
        return this;
    }

    /**
     * @param text 斜体
     * @return this
     */
    public MarkdownBuilder italic(String text) {
        sb.append("_").append(text).append("_");
        return this;
    }

    /**
     * @param text 粗斜体
     * @return this
     */
    public MarkdownBuilder boldItalic(String text) {
        sb.append("***").append(text).append("***");
        return this;
    }

    /**
     * @param text 下划线
     * @return this
     */
    public MarkdownBuilder underline(String text) {
        sb.append("<u>").append(text).append("</u>");
        return this;
    }

    /**
     * @param text 删除线
     * @return this
     */
    public MarkdownBuilder strikethrough(String text) {
        sb.append("~~").append(text).append("~~");
        return this;
    }

    /**
     * @param text 上标
     * @return this
     */
    public MarkdownBuilder sup(String text) {
        sb.append("<sup>").append(text).append("</sup>");
        return this;
    }

    /**
     * @param text 下标
     * @return this
     */
    public MarkdownBuilder sub(String text) {
        sb.append("<sub>").append(text).append("</sub>");
        return this;
    }

    /**
     * @param code 行内代码
     * @return this
     */
    public MarkdownBuilder inlineCode(String code) {
        sb.append("`").append(code).append("`");
        return this;
    }

    /**
     * @param code 代码块
     * @return this
     */
    public MarkdownBuilder code(String code) {
        sb.append("```\n").append(code).append("\n```\n\n");
        return this;
    }

    /**
     * @param lang 语言
     * @param code 代码
     * @return this
     */
    public MarkdownBuilder code(String lang, String code) {
        sb.append("```").append(lang == null ? "" : lang).append("\n")
                .append(code).append("\n```\n\n");
        return this;
    }

    /**
     * @param text 引用
     * @return this
     */
    public MarkdownBuilder quote(String text) {
        sb.append("> ").append(text).append("\n\n");
        return this;
    }

    /**
     * @param lines 多行引用
     * @return this
     */
    public MarkdownBuilder quote(String... lines) {
        for (String line : lines) {
            sb.append("> ").append(line).append("\n");
        }
        sb.append("\n");
        return this;
    }

    /**
     * @param text 列表项
     * @return this
     */
    public MarkdownBuilder bullet(String text) {
        sb.append("- ").append(text).append("\n");
        return this;
    }

    /**
     * @param items 多项
     * @return this
     */
    public MarkdownBuilder bullets(String... items) {
        for (String it : items) {
            sb.append("- ").append(it).append("\n");
        }
        return this;
    }

    /**
     * @param n 序号
     * @param text 内容
     * @return this
     */
    public MarkdownBuilder numbered(int n, String text) {
        sb.append(n).append(". ").append(text).append("\n");
        return this;
    }

    /**
     * @param items 自动编号
     * @return this
     */
    public MarkdownBuilder ordered(String... items) {
        for (int i = 0; i < items.length; i++) {
            sb.append(i + 1).append(". ").append(items[i]).append("\n");
        }
        return this;
    }

    /**
     * @param text 嵌套项
     * @return this
     */
    public MarkdownBuilder nestedBullet(String text) {
        sb.append("    - ").append(text).append("\n");
        return this;
    }

    /**
     * @param text 任务完成项
     * @return this
     */
    public MarkdownBuilder taskDone(String text) {
        sb.append("- [x] ").append(text).append("\n");
        return this;
    }

    /**
     * @param text 任务未完成项
     * @return this
     */
    public MarkdownBuilder task(String text) {
        sb.append("- [ ] ").append(text).append("\n");
        return this;
    }

    /**
     * @param alt 图片 alt
     * @param url 图片 URL
     * @return this
     */
    public MarkdownBuilder image(String alt, String url) {
        sb.append("![").append(alt).append("](").append(url).append(")\n\n");
        return this;
    }

    /**
     * 指定尺寸图片（QQ 扩展 #宽px #高px）。
     *
     * @param alt alt
     * @param url url
     * @param widthPx 宽
     * @param heightPx 高
     * @return this
     */
    public MarkdownBuilder image(String alt, String url, int widthPx, int heightPx) {
        sb.append("![").append(alt).append(" #").append(widthPx).append("px #")
                .append(heightPx).append("px](").append(url).append(")\n\n");
        return this;
    }

    /**
     * @param name 名称
     * @param url 链接
     * @return this
     */
    public MarkdownBuilder link(String name, String url) {
        sb.append("[").append(name).append("](").append(url).append(")\n\n");
        return this;
    }

    /**
     * @return 分割线
     */
    public MarkdownBuilder divider() {
        sb.append("---\n\n");
        return this;
    }

    /**
     * @param openid 被 @ 用户
     * @return this
     */
    public MarkdownBuilder at(String openid) {
        sb.append("<@").append(openid).append(">");
        return this;
    }

    /**
     * @param expr 行内 KaTeX
     * @return this
     */
    public MarkdownBuilder katex(String expr) {
        sb.append("$").append(expr).append("$");
        return this;
    }

    /**
     * @param expr 块级 KaTeX
     * @return this
     */
    public MarkdownBuilder katexBlock(String expr) {
        sb.append("$$").append(expr).append("$$\n\n");
        return this;
    }

    /**
     * @param expr boxed 公式
     * @return this
     */
    public MarkdownBuilder katexBoxed(String expr) {
        sb.append("$\\boxed{").append(expr).append("}$");
        return this;
    }

    /**
     * @param text 文本
     * @param color 色
     * @return this
     */
    public MarkdownBuilder katexColor(String text, String color) {
        sb.append("$\\textcolor{").append(color).append("}{").append(text).append("}$");
        return this;
    }

    /**
     * @param text 文本
     * @param color 底色
     * @return this
     */
    public MarkdownBuilder katexColorBg(String text, String color) {
        sb.append("$\\colorbox{").append(color).append("}{").append(text).append("}$");
        return this;
    }

    /**
     * 居中（$$\text{...}$$）。
     *
     * @param text 文本
     * @return this
     */
    public MarkdownBuilder center(String text) {
        sb.append("$$\\text{").append(text).append("}$$\n\n");
        return this;
    }

    /**
     * @param cols 表头
     * @return this
     */
    public MarkdownBuilder tableHeader(String... cols) {
        sb.append("| ");
        join(cols, " | ");
        sb.append(" |\n");
        return this;
    }

    /**
     * @param cells 数据行
     * @return this
     */
    public MarkdownBuilder tableRow(String... cells) {
        sb.append("| ");
        join(cells, " | ");
        sb.append(" |\n");
        return this;
    }

    /**
     * @param headers 表头
     * @param rows 数据
     * @return this
     */
    public MarkdownBuilder table(String[] headers, String[]... rows) {
        tableHeader(headers);
        sb.append("| ");
        for (int i = 0; i < headers.length; i++) {
            sb.append(i == 0 ? "---" : " | ---");
        }
        sb.append(" |\n");
        for (String[] r : rows) {
            tableRow(r);
        }
        sb.append("\n");
        return this;
    }

    /**
     * @return Markdown 全文
     */
    public String build() {
        String s = sb.toString();
        while (s.endsWith("\n")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * @return 转消息模型
     */
    public Markdown toMarkdown() {
        return Markdown.of(build());
    }

    private void join(String[] arr, String sep) {
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(arr[i] == null ? "" : arr[i]);
        }
    }
}
