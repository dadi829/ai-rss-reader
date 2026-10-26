package com.example.airssreader.data.mapper

import com.example.airssreader.data.local.entity.ArticleWithFeed
import com.example.airssreader.data.local.entity.FeedWithStats
import com.example.airssreader.domain.model.Article
import com.example.airssreader.domain.model.Feed

/**
 * 持久化模型 → 领域模型。
 *
 * UI 层只认 [Feed] / [Article]，Room 的实体类不外泄，
 * 后续换存储实现时上层不用改。
 */

fun FeedWithStats.toDomain(): Feed = Feed(
    id = feed.id,
    title = feed.title,
    url = feed.url,
    lastFetchedAt = feed.lastFetchedAt,
    unreadCount = unreadCount,
    articleCount = articleCount,
)

fun ArticleWithFeed.toDomain(): Article = Article(
    id = article.id,
    feedId = article.feedId,
    feedTitle = feedTitle,
    title = article.title,
    link = article.link,
    summary = article.summary,
    publishedAt = article.publishedAt,
    isRead = article.isRead,
)
