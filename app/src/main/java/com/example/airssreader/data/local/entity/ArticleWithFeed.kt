package com.example.airssreader.data.local.entity

import androidx.room.Embedded

/**
 * 查询投影：资讯 + 所属源名称。
 *
 * 用于跨源搜索与单源列表，UI 需要显示「来自哪个源」。
 */
data class ArticleWithFeed(
    @Embedded val article: ArticleEntity,
    val feedTitle: String,
)
