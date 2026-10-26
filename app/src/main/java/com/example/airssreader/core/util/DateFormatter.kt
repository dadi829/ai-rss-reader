package com.example.airssreader.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 时间展示格式化。
 *
 * 列表里用相对时间（"3 小时前"）提升可读性，超过一周回落到绝对日期；
 * 源管理页需要精确到分钟，用 [formatDateTime]。
 */
object DateFormatter {

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA)
    private val fullDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)

    /** 列表用：刚刚 / N 分钟前 / N 小时前 / 昨天 / N 天前 / 具体日期 */
    fun formatRelative(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        if (timestamp <= 0L) return "时间未知"
        val diff = now - timestamp
        return when {
            diff < 0L -> formatDate(timestamp)
            diff < MINUTE -> "刚刚"
            diff < HOUR -> "${diff / MINUTE} 分钟前"
            diff < DAY -> "${diff / HOUR} 小时前"
            diff < 2 * DAY -> "昨天"
            diff < 7 * DAY -> "${diff / DAY} 天前"
            else -> formatDate(timestamp)
        }
    }

    fun formatDate(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(dateFormatter)

    fun formatDateTime(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

    /** 详情页：列表里用相对时间，正文页需要确切日期 */
    fun formatFullDateTime(timestamp: Long): String =
        if (timestamp <= 0L) {
            "时间未知"
        } else {
            Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(fullDateTimeFormatter)
        }

    /** 源管理页：从未拉取时给出明确提示，避免显示 1970 年 */
    fun formatLastFetched(timestamp: Long): String =
        if (timestamp <= 0L) "从未拉取" else "上次更新 ${formatDateTime(timestamp)}"
}
