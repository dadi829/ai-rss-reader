package com.example.airssreader.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * RSS 发布时间解析。
 *
 * RSS 规范建议用 RFC 822，但实际源五花八门：Atom 用 ISO 8601，
 * 国内源常用 "yyyy-MM-dd HH:mm:ss"，还有带时区缩写的写法。
 * 这里按「RFC 1123 → ISO → 常见自定义格式 → 纯日期」的顺序逐个尝试，
 * 全部失败返回 null，由调用方回退为入库时间。
 *
 * 注意：解析英文月份缩写必须显式指定 Locale.ENGLISH，
 * 否则在中文系统默认 Locale 下 "Oct" 无法匹配 MMM。
 */
object RssDateParser {

    private val englishLocale = Locale.ENGLISH

    private val customPatterns: List<DateTimeFormatter> = listOf(
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", englishLocale),
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", englishLocale),
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", englishLocale),
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss zzz", englishLocale),
        DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", englishLocale),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
    )

    fun parse(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null

        // 1. RFC 1123：绝大多数 RSS 2.0 源
        runCatching {
            return OffsetDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant().toEpochMilli()
        }

        // 2. ISO 8601 带偏移：Atom 源
        runCatching {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant().toEpochMilli()
        }

        // 3. ISO 瞬时时间（以 Z 结尾）
        runCatching {
            return Instant.parse(value).toEpochMilli()
        }

        // 4. 常见自定义格式
        for (pattern in customPatterns) {
            runCatching {
                return OffsetDateTime.parse(value, pattern).toInstant().toEpochMilli()
            }
            runCatching {
                return LocalDateTime.parse(value, pattern)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }

        // 5. 只有日期，按当天零点处理
        runCatching {
            return LocalDate.parse(value)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        return null
    }
}
