package com.example.airssreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.airssreader.data.local.entity.ArticleEntity
import com.example.airssreader.data.local.entity.ArticleWithFeed
import kotlinx.coroutines.flow.Flow

/**
 * 资讯条目数据访问。
 *
 * 列表查询统一 JOIN 源表带出 feedTitle，因为跨源搜索必须标明来源。
 */
@Dao
interface ArticleDao {

    /** 单源资讯流，按发布时间倒序；同时间用 id 兜底保证顺序稳定 */
    @Query(
        """
        SELECT a.*, f.title AS feedTitle
        FROM articles a
        INNER JOIN feeds f ON f.id = a.feedId
        WHERE a.feedId = :feedId
        ORDER BY a.publishedAt DESC, a.id DESC
        """
    )
    fun observeByFeed(feedId: Long): Flow<List<ArticleWithFeed>>

    /**
     * 全局搜索：跨所有源，同时匹配标题与摘要。
     *
     * 说明：
     * 1. SQLite 的 LIKE 对 ASCII 默认大小写不敏感，中文按字节精确匹配，符合预期；
     * 2. 用 ESCAPE '\' 让 % 和 _ 作为普通字符参与匹配（关键词里的这两个符号由调用方转义）；
     * 3. LIMIT 200 防止极端关键词把整库拉进内存。
     */
    @Query(
        """
        SELECT a.*, f.title AS feedTitle
        FROM articles a
        INNER JOIN feeds f ON f.id = a.feedId
        WHERE a.title LIKE '%' || :keyword || '%' ESCAPE '\'
           OR a.summary LIKE '%' || :keyword || '%' ESCAPE '\'
        ORDER BY a.publishedAt DESC, a.id DESC
        LIMIT 200
        """
    )
    fun search(keyword: String): Flow<List<ArticleWithFeed>>

    /** IGNORE：已存在的条目（同 feedId + link）跳过，因此刷新不会覆盖已读状态 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<ArticleEntity>): List<Long>

    @Query("UPDATE articles SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE articles SET isRead = 1 WHERE feedId = :feedId")
    suspend fun markAllRead(feedId: Long)

    /** 搜索页的空态提示需要展示缓存总量 */
    @Query("SELECT COUNT(*) FROM articles")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun totalCount(): Int
}
