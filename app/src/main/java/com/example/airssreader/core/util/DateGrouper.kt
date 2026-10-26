package com.example.airssreader.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 聚合时间线的日期分组。
 *
 * 声明顺序即展示顺序，[FeedScreen] 依赖 ordinal 排序，
 * 所以新增分组必须插在语义正确的位置，不能追加到末尾。
 */
enum class DateBucket(val label: String) {
    TODAY("今天"),
    YESTERDAY("昨天"),
    THIS_WEEK("本周"),
    EARLIER("更早"),
}

/**
 * 把发布时间戳归入日期分组。
 *
 * 用 [LocalDate] 比较而不是「时间差 / 86400000」：
 * 后者在跨时区或夏令时切换时会算错，而且「今天」应该按本地日历日界定，
 * 不是按过去 24 小时滚动。
 */
object DateGrouper {

    fun bucketOf(
        timestamp: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone),
    ): DateBucket {
        if (timestamp <= 0L) return DateBucket.EARLIER
        val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        return when {
            date.isEqual(today) -> DateBucket.TODAY
            date.isEqual(today.minusDays(1)) -> DateBucket.YESTERDAY
            date.isAfter(today.minusDays(7)) -> DateBucket.THIS_WEEK
            else -> DateBucket.EARLIER
        }
    }
}
