package com.example.airssreader.data.local.seed

import com.example.airssreader.data.local.entity.FeedEntity

/**
 * 预置 RSS 源。
 *
 * 选源原则：覆盖「国内媒体 / 论文 / 官方博客 / 海外媒体」四类，
 * 保证首次打开就有内容可看。
 *
 * 排序不是随意的：中文源排在最前，因为首次打开时它们加载最快、
 * 内容也最直观，用户第一眼就能看到能读的东西。
 *
 * 三处地址与常见写法不同，原因如下（2026-10 逐个实测）：
 * 1. arXiv：`arxiv.org/rss/cs.AI` 会 302 到 `export.arxiv.org`，直接写最终地址少一次跳转；
 * 2. 机器之心 `jiqizhixin.com/rss` 已下线（302 到网页），The Batch 的 `/rss` 返回 404，
 *    因此替换为 InfoQ AI 与 Google AI Blog；
 * 3. Anthropic 官方 RSS 返回 404、VentureBeat 返回 429 限流，均未采用。
 */
object DefaultFeeds {

    private data class Seed(
        val title: String,
        val url: String,
    )

    private val seeds = listOf(
        Seed("量子位", "https://www.qbitai.com/feed"),
        Seed("InfoQ AI", "https://www.infoq.cn/feed/AI"),
        Seed("雷锋网", "https://www.leiphone.com/feed"),
        Seed("arXiv cs.AI", "https://export.arxiv.org/rss/cs.AI"),
        Seed("HuggingFace Blog", "https://huggingface.co/blog/feed.xml"),
        Seed("Google AI Blog", "https://blog.google/technology/ai/rss/"),
        Seed("DeepMind Blog", "https://deepmind.google/blog/rss.xml"),
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
