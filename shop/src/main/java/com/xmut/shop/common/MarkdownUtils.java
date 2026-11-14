package com.xmut.shop.common;

/**
 * <p>
 * 轻量 Markdown -> HTML 转换工具（用于报告预览与 Word 下载，不引入第三方依赖）
 * 支持：标题、粗体、表格、无序列表、有序条目、段落
 * </p>
 *
 * @author Bennu
 * @since 2026-11-14
 */
public class MarkdownUtils {

    private MarkdownUtils() {
    }

    public static String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        boolean inTable = false;
        for (String raw : markdown.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("|")) {
                if (isTableSeparator(line)) {
                    continue; // |---|---| 分隔行跳过
                }
                if (!inTable) {
                    html.append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;'>");
                    inTable = true;
                }
                html.append(toTableRow(line));
                continue;
            }
            if (inTable) {
                html.append("</table>");
                inTable = false;
            }
            if (line.startsWith("### ")) {
                html.append("<h3>").append(inline(line.substring(4))).append("</h3>");
            } else if (line.startsWith("## ")) {
                html.append("<h2>").append(inline(line.substring(3))).append("</h2>");
            } else if (line.startsWith("# ")) {
                html.append("<h1>").append(inline(line.substring(2))).append("</h1>");
            } else if (line.startsWith("- ")) {
                html.append("<li>").append(inline(line.substring(2))).append("</li>");
            } else {
                html.append("<p>").append(inline(line)).append("</p>");
            }
        }
        if (inTable) {
            html.append("</table>");
        }
        return html.toString();
    }

    private static boolean isTableSeparator(String line) {
        return line.contains("---") && line.replaceAll("[|\\s:-]", "").isEmpty();
    }

    private static String toTableRow(String line) {
        String[] cells = line.split("\\|");
        StringBuilder sb = new StringBuilder("<tr>");
        for (String c : cells) {
            String cell = c.trim();
            if (cell.isEmpty()) {
                continue;
            }
            sb.append("<td>").append(inline(cell)).append("</td>");
        }
        sb.append("</tr>");
        return sb.toString();
    }

    private static String inline(String text) {
        return text.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
    }

    /**
     * 包装成完整 HTML 页面（Word 下载与浏览器预览共用）
     */
    public static String wrapHtmlPage(String title, String bodyHtml) {
        return "<html><head><meta charset='utf-8'><title>" + escape(title)
                + "</title><style>body{font-family:'Microsoft YaHei',SimSun,sans-serif;padding:24px;}"
                + "h1{font-size:20px;}h2{font-size:17px;}h3{font-size:15px;}"
                + "table{font-size:13px;}</style></head><body>" + bodyHtml + "</body></html>";
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
