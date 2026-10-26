package com.example.airssreader.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 资讯条目表。
 *
 * 关键设计：
 * 1. 外键级联删除——删源时该源缓存的所有条目一并清掉，不留孤儿数据；
 * 2. (feedId, link) 联合唯一索引——同一源内同链接只存一条，重复刷新靠 IGNORE 策略
 *    静默跳过，因此已读状态不会被刷新重置；
 * 3. publishedAt 建普通索引，列表按时间倒序取数时命中。
 */
@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feedId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["feedId", "link"], unique = true),
        Index(value = ["publishedAt"]),
    ],
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val feedId: Long,
    val title: String,
    /** 原文链接，同时作为去重依据 */
    val link: String,
    /** 已清洗的纯文本摘要 */
    val summary: String,
    /** 发布时间戳（毫秒） */
    val publishedAt: Long,
    /** 是否已读，用户点开详情后置为 true */
    val isRead: Boolean = false,
    /** 入库时间，用于判断缓存新鲜度 */
    val cachedAt: Long = System.currentTimeMillis(),
)
