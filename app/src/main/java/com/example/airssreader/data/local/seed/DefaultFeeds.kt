package com.example.airssreader.data.local.seed

import com.example.airssreader.data.local.entity.FeedEntity

/**
 * 预置 RSS 源。
 *
 * 选源原则：覆盖「论文 / 官方博客 / 国内媒体 / 海外媒体」四类，保证首次打开就有内容可看。
 *
 * 两处地址与常见写法不同，原因如下（2026-10 实测）：
 * 1. arXiv：`arxiv.org/rss/cs.AI` 会 302 到 `export.arxiv.org`，直接写最终地址少一次跳转；
 * 2. 机器之心 `jiqizhixin.com/rss` 已下线（302 到网页），The Batch 的 `/rss` 返回 404，
 *    因此替换为 InfoQ AI 与 Google AI Blog 两个同样稳定的 AI 垂类源。
 */
object DefaultFeeds {

    private data class Seed(
        val title: String,
        val url: String,
    )

    private val seeds = listOf(
        Seed("arXiv cs.AI", "https://export.arxiv.org/rss/cs.AI"),
        Seed("HuggingFace Blog", "https://huggingface.co/blog/feed.xml"),
        Seed("量子位", "https://www.qbitai.com/feed"),
        Seed("InfoQ AI", "https://www.infoq.cn/feed/AI"),
        Seed("Google AI Blog", "https://blog.google/technology/ai/rss/"),
        Seed("MIT Tech Review · AI", "https://www.technologyreview.com/topic/artificial-intelligence/feed/"),
    )

    /**
     * 生成待插入的实体列表。
     *
     * createdAt 按索引递增，保证源列表顺序与上面的定义顺序一致
     * （DAO 按 createdAt 升序排序，同一毫秒插入会导致顺序随机）。
     */
    fun buildEntities(now: Long = System.currentTimeMillis()): List<FeedEntity> =
        seeds.mapIndexed { index, seed ->
            FeedEntity(
                title = seed.title,
                url = seed.url,
                lastFetchedAt = 0L,
                createdAt = now + index,
            )
        }
}
