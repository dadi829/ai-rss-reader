package com.example.airssreader.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * RSS 源表。
 *
 * url 建唯一索引：同一地址不允许重复添加，添加时靠冲突策略兜底。
 */
@Entity(
    tableName = "feeds",
    indices = [Index(value = ["url"], unique = true)],
)
data class FeedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** 源名称，添加时自动从 feed 的 <title> 抓取，抓不到则用域名兜底 */
    val title: String,
    /** 源地址（原始 URL，可能被重定向，保持用户输入值） */
    val url: String,
    /** 上次成功拉取时间戳，0 = 从未拉取 */
    val lastFetchedAt: Long = 0L,
    /** 创建时间，用于默认排序 */
    val createdAt: Long = System.currentTimeMillis(),
)
