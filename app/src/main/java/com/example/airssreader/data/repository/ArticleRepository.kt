package com.example.airssreader.data.repository

import com.example.airssreader.data.local.dao.ArticleDao
import com.example.airssreader.data.mapper.toDomain
import com.example.airssreader.domain.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 资讯仓库：只读本地缓存 + 已读状态维护。
 *
 * 这里不发起任何网络请求——条目只由 [FeedRepository.refresh] 写入，
 * 保证「数据从哪来」的路径唯一，便于排查问题。
 */
class ArticleRepository(
    private val articleDao: ArticleDao,
) {

    fun observeByFeed(feedId: Long): Flow<List<Article>> =
        articleDao.observeByFeed(feedId).map { list -> list.map { it.toDomain() } }

    /** 聚合视图：所有源的条目混排，按发布时间倒序 */
    fun observeAll(): Flow<List<Article>> =
        articleDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** 详情页：订阅单条记录，已读状态变化会自动推回来 */
    fun observeById(articleId: Long): Flow<Article?> =
        articleDao.observeById(articleId).map { it?.toDomain() }

    /**
     * 全局搜索：跨所有已缓存的源，匹配标题与摘要。
     *
     * 关键词里的 `%` `_` `\` 会被转义，避免用户输入的通配符
     * 被 SQLite 当成模式匹配（例如搜 "100%" 不该匹配所有内容）。
     */
    fun search(rawKeyword: String): Flow<List<Article>> =
        articleDao.search(escapeLikePattern(rawKeyword))
            .map { list -> list.map { it.toDomain() } }

    /** 已缓存条目总数，搜索页空态展示用 */
    fun observeTotalCachedCount(): Flow<Int> = articleDao.observeTotalCount()

    suspend fun markRead(articleId: Long) {
        articleDao.markRead(articleId)
    }

    /** 聚合视图下的「全部已读」 */
    suspend fun markAllReadGlobally() {
        articleDao.markAllReadGlobally()
    }

    /**
     * 转义 LIKE 通配符。
     *
     * 顺序很重要：必须先转义反斜杠本身，否则后续替换产生的反斜杠会被二次转义。
     * 配合 DAO 里的 `ESCAPE '\'` 使用。
     */
    private fun escapeLikePattern(keyword: String): String =
        keyword.trim()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
}
