package com.example.airssreader.ui.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * 把命中的关键词包成高亮片段。
 *
 * 用 AnnotatedString 而不是拆成多个 Text：能保持换行、省略号等排版行为不变。
 * 忽略大小写，覆盖英文标题里 "AI" / "ai" 混写的情况。
 */
fun buildHighlightedText(
    text: String,
    keyword: String,
    highlightStyle: SpanStyle,
): AnnotatedString {
    if (keyword.isBlank() || text.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val hit = text.indexOf(keyword, cursor, ignoreCase = true)
            if (hit < 0) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, hit))
            withStyle(highlightStyle) {
                append(text.substring(hit, hit + keyword.length))
            }
            cursor = hit + keyword.length
        }
    }
}
