package com.example.airssreader.domain.model

/**
 * 领域模型：RSS 源。
 *
 * 与 Room 的 [com.example.airssreader.data.local.entity.FeedEntity] 分离，
 * 让 UI 层不感知持久化细节；[unreadCount] / [articleCount] 由聚合查询算出。
 */
data class Feed(
    val id: Long,
    val title: String,
    val url: String,
    /** 上次成功拉取的时间戳（毫秒），0 表示从未拉取 */
    val lastFetchedAt: Long,
    val unreadCount: Int,
    val articleCount: Int,
) {
    val hasFetched: Boolean get() = lastFetchedAt > 0L
}
