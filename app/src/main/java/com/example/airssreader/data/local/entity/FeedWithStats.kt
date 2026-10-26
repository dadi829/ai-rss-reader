package com.example.airssreader.data.local.entity

import androidx.room.Embedded

/**
 * 查询投影：源 + 该源的条目统计。
 *
 * 未读数用相关子查询算，避免把全表条目读进内存再分组。
 */
data class FeedWithStats(
    @Embedded val feed: FeedEntity,
    val unreadCount: Int,
    val articleCount: Int,
)
