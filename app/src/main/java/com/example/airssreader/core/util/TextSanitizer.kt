package com.example.airssreader.core.util

import android.text.Html

/**
 * RSS 正文清洗工具。
 *
 * RSS 的 description/content 里常混着 HTML 片段与转义实体，
 * 直接展示会出现 `<p>` 之类的标签，所以入库前统一转成纯文本。
 */
object TextSanitizer {

    /** 摘要展示长度上限，超出截断并加省略号 */
    private const val SUMMARY_MAX_LENGTH = 180

    private val whitespaceRegex = Regex("\\s+")
    private val brRegex = Regex("(?i)<br\\s*/?>")
    private val blockEndRegex = Regex("(?i)</(p|div|li|h[1-6])>")

    /**
     * HTML → 纯文本。
     *
     * 先把换行标签替换成空格，避免 `Html.fromHtml` 之后段落粘连成一个长串，
     * 再压缩连续空白。
     */
    fun toPlainText(html: String?): String {
        if (html.isNullOrBlank()) return ""
        val prepared = brRegex.replace(html, " ")
            .let { blockEndRegex.replace(it, " ") }
        val text = Html.fromHtml(prepared, Html.FROM_HTML_MODE_LEGACY).toString()
        return whitespaceRegex.replace(text, " ").trim()
    }

    /** 按字符数截断，保留语义完整的中文表达 */
    fun truncate(text: String, maxLength: Int = SUMMARY_MAX_LENGTH): String {
        val clean = text.trim()
        if (clean.length <= maxLength) return clean
        return clean.take(maxLength).trimEnd() + "…"
    }

    /** 一步到位：清洗 + 截断，产出可直接入库的摘要 */
    fun toSummary(html: String?, maxLength: Int = SUMMARY_MAX_LENGTH): String =
        truncate(toPlainText(html), maxLength)

    /** 标题兜底：部分源会给空标题，用固定占位符避免列表出现空白行 */
    fun normalizeTitle(raw: String?): String {
        val clean = toPlainText(raw)
        return clean.ifBlank { "(无标题)" }
    }
}
