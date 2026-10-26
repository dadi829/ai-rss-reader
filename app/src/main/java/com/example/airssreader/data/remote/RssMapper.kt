package com.example.airssreader.data.remote

import com.example.airssreader.core.util.RssDateParser
import com.example.airssreader.core.util.TextSanitizer
import com.example.airssreader.data.local.entity.ArticleEntity
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem

/**
 * 网络模型 → 数据库实体的映射。
 *
 * 映射阶段就完成三件事，保证入库数据是干净的：
 * 1. HTML 清洗成纯文本摘要；
 * 2. 时间字符串解析成毫秒时间戳，失败则回退为当前时间；
 * 3. 过滤掉没有链接的条目（无法打开原文，留着没意义），并按链接去重。
 */
object RssMapper {

    /**
     * 单次刷新最多入库的条目数。
     *
     * arXiv cs.AI 单次返回 300+ 条，全量入库会让首屏渲染和搜索明显变慢，
     * 而用户实际只会看最新的一批，因此截断。
     */
    const val MAX_ITEMS_PER_FETCH = 60

    fun toArticleEntities(
        feedId: Long,
        channel: RssChannel,
        now: Long = System.currentTimeMillis(),
        limit: Int = MAX_ITEMS_PER_FETCH,
    ): List<ArticleEntity> =
        channel.items
            .asSequence()
            .mapNotNull { it.toEntity(feedId, now) }
            .distinctBy { it.link }
            .take(limit)
            .toList()

    private fun RssItem.toEntity(feedId: Long, now: Long): ArticleEntity? {
        val normalizedLink = link?.trim().orEmpty()
        if (normalizedLink.isEmpty()) return null

        val published = RssDateParser.parse(pubDate) ?: now

        return ArticleEntity(
            feedId = feedId,
            title = TextSanitizer.normalizeTitle(title),
            link = normalizedLink,
            // description 更常见且有长度控制，content 作为兜底
            summary = TextSanitizer.toSummary(description?.takeIf { it.isNotBlank() } ?: content),
            publishedAt = published,
            isRead = false,
            cachedAt = now,
        )
    }
}
