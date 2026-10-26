package com.example.airssreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.airssreader.data.local.entity.FeedEntity
import com.example.airssreader.data.local.entity.FeedWithStats
import kotlinx.coroutines.flow.Flow

/**
 * RSS 源数据访问。
 *
 * 所有观察型查询返回 Flow，Room 在表数据变化时自动重新发射，
 * 因此添加/删除源之后列表无需手动刷新。
 */
@Dao
interface FeedDao {

    /**
     * 源列表 + 统计信息。
     *
     * 未读数用相关子查询逐源计算：一次查询拿到「源 + 未读数 + 总数」，
     * 避免在内存里做 N+1 聚合。按创建时间升序，保证预置源顺序稳定。
     */
    @Query(
        """
        SELECT f.*,
               (SELECT COUNT(*) FROM articles a WHERE a.feedId = f.id AND a.isRead = 0) AS unreadCount,
               (SELECT COUNT(*) FROM articles a WHERE a.feedId = f.id) AS articleCount
        FROM feeds f
        ORDER BY f.createdAt ASC, f.id ASC
        """
    )
    fun observeFeeds(): Flow<List<FeedWithStats>>

    @Query("SELECT * FROM feeds WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): FeedEntity?

    @Query("SELECT * FROM feeds WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): FeedEntity?

    @Query("SELECT COUNT(*) FROM feeds")
    suspend fun count(): Int

    /** 冲突时返回 -1，调用方据此判断是否重复添加 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(feed: FeedEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(feeds: List<FeedEntity>): List<Long>

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE feeds SET lastFetchedAt = :timestamp WHERE id = :id")
    suspend fun updateLastFetchedAt(id: Long, timestamp: Long)
}
