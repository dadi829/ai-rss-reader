package com.example.airssreader.domain.model

/**
 * 领域模型：一条 RSS 资讯。
 *
 * [feedTitle] 在跨源搜索时用于标识来源，单源列表里同样填充。
 */
data class Article(
    val id: Long,
    val feedId: Long,
    val feedTitle: String,
    val title: String,
    val link: String,
    /** 已去除 HTML 标签并截断的纯文本摘要 */
    val summary: String,
    /** 发布时间戳（毫秒）；解析失败时回退为入库时间 */
    val publishedAt: Long,
    val isRead: Boolean,
)
