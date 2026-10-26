package com.example.airssreader.data.repository

import com.example.airssreader.data.local.dao.ArticleDao
import com.example.airssreader.data.local.dao.FeedDao
import com.example.airssreader.data.local.entity.FeedEntity
import com.example.airssreader.data.local.seed.DefaultFeeds
import com.example.airssreader.data.mapper.toDomain
import com.example.airssreader.data.remote.RssMapper
import com.example.airssreader.data.remote.RssService
import com.example.airssreader.domain.model.Feed
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.exception.RssParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** 添加源的结果，UI 依据它决定提示文案 */
sealed interface AddFeedResult {
    data class Success(val feedTitle: String) : AddFeedResult
    data object Duplicate : AddFeedResult
    data class Failure(val message: String) : AddFeedResult
}

/** 刷新源的结果 */
sealed interface RefreshResult {
    data class Success(val newItemCount: Int, val totalItemCount: Int) : RefreshResult
    data class Failure(val message: String) : RefreshResult
}

/**
 * 批量刷新结果。
 *
 * 刻意不做成「全成功 / 全失败」：8 个源里挂掉 1 个是常态，
 * 已经拉到的新内容不该因为一个源失败就被忽略，所以分别记成功数与失败数。
 */
data class RefreshAllResult(
    val feedCount: Int,
    val successCount: Int,
    val failedCount: Int,
    val newItemCount: Int,
    val firstErrorMessage: String?,
)

/**
 * 源仓库：源的增删查 + 手动刷新。
 *
 * 刷新是唯一会发起网络请求的入口，且只能由 UI 的显式操作（点刷新 / 下拉）调用，
 * 仓库内部没有任何定时器、没有 WorkManager、没有后台服务。
 */
class FeedRepository(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val rssService: RssService,
) {

    fun observeFeeds(): Flow<List<Feed>> =
        feedDao.observeFeeds().map { list -> list.map { it.toDomain() } }

    suspend fun findFeed(feedId: Long): Feed? =
        feedDao.findById(feedId)?.let { entity ->
            Feed(
                id = entity.id,
                title = entity.title,
                url = entity.url,
                lastFetchedAt = entity.lastFetchedAt,
                unreadCount = 0,
                articleCount = 0,
            )
        }

    /**
     * 首次启动时写入预置源。
     *
     * 只在库里一个源都没有时执行，用户把预置源全删光后也不会被强行塞回来
     * （count 为 0 会重新种入，这是刻意的：清空等于恢复出厂）。
     */
    suspend fun ensureSeeded() {
        if (feedDao.count() > 0) return
        feedDao.insertAll(DefaultFeeds.buildEntities())
    }

    /**
     * 添加一个 RSS 源。
     *
     * 先真实拉取一次再入库，好处是：
     * 1. 地址写错能立刻发现，不会留下一个永远拉不到内容的死源；
     * 2. 顺带拿到源的正式标题，不用让用户手动输入；
     * 3. 首次添加即有内容可读，体验完整。
     */
    suspend fun addFeed(rawUrl: String): AddFeedResult {
        val url = rawUrl.trim()
        if (url.isEmpty()) return AddFeedResult.Failure("请输入 RSS 地址")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return AddFeedResult.Failure("地址需要以 http:// 或 https:// 开头")
        }
        if (feedDao.findByUrl(url) != null) return AddFeedResult.Duplicate

        val channel = try {
            rssService.fetch(url)
        } catch (e: Exception) {
            return AddFeedResult.Failure(e.toUserMessage())
        }

        val title = channel.title?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: url.substringAfter("://").substringBefore("/")

        val feedId = feedDao.insert(
            FeedEntity(
                title = title,
                url = url,
                lastFetchedAt = System.currentTimeMillis(),
            ),
        )
        if (feedId <= 0L) return AddFeedResult.Duplicate

        // 映射阶段要逐条做 HTML 清洗，放在 IO 线程，避免 60 条一起处理时卡住首帧
        val entities = withContext(Dispatchers.Default) {
            RssMapper.toArticleEntities(feedId = feedId, channel = channel)
        }
        articleDao.insertAll(entities)
        return AddFeedResult.Success(title)
    }

    /** 删除源。articles 表靠外键 CASCADE 一并清理，无需手工删条目 */
    suspend fun deleteFeed(feedId: Long) {
        feedDao.deleteById(feedId)
    }

    /**
     * 手动刷新单个源。
     *
     * 流程：拉取 → 映射 → IGNORE 入库 → 更新 lastFetchedAt。
     * 已存在的条目会被跳过，因此刷新不会把已读状态重置为未读。
     */
    suspend fun refresh(feedId: Long): RefreshResult {
        val feed = feedDao.findById(feedId)
            ?: return RefreshResult.Failure("源不存在，可能已被删除")

        return try {
            val newCount = fetchInto(feedId = feed.id, url = feed.url)
            RefreshResult.Success(
                newItemCount = newCount,
                totalItemCount = articleDao.totalCount(),
            )
        } catch (e: Exception) {
            RefreshResult.Failure(e.toUserMessage())
        }
    }

    /**
     * 批量刷新全部源。
     *
     * 实现要点：
     * 1. 分块并发而不是一次性 [feeds.size] 个协程——同时打 8 个请求很容易被源站限流，
     *    这里每块 3 个，块内并发、块间串行；
     * 2. 单源异常用 runCatching 就地吞掉，一个源失败不影响同块其他源；
     * 3. 计数用 Mutex 保护：并发协程写同一个 var 不是原子操作，不加锁会丢计数。
     *
     * 注意这依然是**用户手动触发**的：只有聚合视图下点刷新或下拉才会走到这里。
     */
    suspend fun refreshAll(): RefreshAllResult {
        val feeds = feedDao.allFeeds()
        if (feeds.isEmpty()) {
            return RefreshAllResult(0, 0, 0, 0, null)
        }

        var successCount = 0
        var failedCount = 0
        var newItemCount = 0
        var firstErrorMessage: String? = null
        val lock = Mutex()

        feeds.chunked(MAX_CONCURRENT_REFRESH).forEach { chunk ->
            coroutineScope {
                chunk.map { feed ->
                    async(Dispatchers.IO) {
                        val outcome = runCatching { fetchInto(feedId = feed.id, url = feed.url) }
                        lock.withLock {
                            outcome
                                .onSuccess { count ->
                                    successCount++
                                    newItemCount += count
                                }
                                .onFailure { error ->
                                    failedCount++
                                    if (firstErrorMessage == null) {
                                        firstErrorMessage = error.toUserMessage()
                                    }
                                }
                        }
                    }
                }.awaitAll()
            }
        }

        return RefreshAllResult(
            feedCount = feeds.size,
            successCount = successCount,
            failedCount = failedCount,
            newItemCount = newItemCount,
            firstErrorMessage = firstErrorMessage,
        )
    }

    /**
     * 拉取一个源并入库，返回新增条数。
     *
     * 抽出来是因为单源刷新与批量刷新共用同一段逻辑，
     * 差别只在于异常由谁接住：单源直接翻译成 [RefreshResult.Failure]，
     * 批量则交给 runCatching 后统一计数。
     */
    private suspend fun fetchInto(feedId: Long, url: String): Int {
        val channel = rssService.fetch(url)
        // 映射阶段要逐条做 HTML 清洗，放在 Default 线程，避免 60 条一起处理时卡住主线程
        val entities = withContext(Dispatchers.Default) {
            RssMapper.toArticleEntities(feedId = feedId, channel = channel)
        }
        val inserted = articleDao.insertAll(entities)
        feedDao.updateLastFetchedAt(feedId, System.currentTimeMillis())
        return inserted.count { it != -1L }
    }

    /** 把某个源的全部条目标记为已读 */
    suspend fun markAllRead(feedId: Long) {
        articleDao.markAllRead(feedId)
    }

    private companion object {
        /**
         * 批量刷新的并发上限。
         *
         * 取 4 的算法：默认 8 个源刚好两轮跑完；再高意义不大，
         * 因为瓶颈通常是单个慢源而不是并发度，而并发过高容易被同一源站限流。
         */
        const val MAX_CONCURRENT_REFRESH = 4
    }
}

/**
 * 把网络/解析异常翻译成用户能看懂的一句话。
 *
 * 集中在这里处理，UI 层只负责显示 message。
 * 接收 [Throwable] 而不是 [Exception]：批量刷新里用 runCatching 拿到的是 Throwable。
 */
private fun Throwable.toUserMessage(): String = when (this) {
    is UnknownHostException -> "无法解析域名，请检查网络或代理设置"
    is SocketTimeoutException -> "连接超时，请稍后重试"
    is SSLException -> "HTTPS 证书校验失败，该源可能不被信任"
    is HttpException -> "服务器返回 ${code}，该地址可能已失效"
    is RssParsingException -> "该地址不是有效的 RSS / Atom 源"
    is IllegalArgumentException -> message ?: "地址格式不正确"
    else -> message?.takeIf { it.isNotBlank() } ?: "拉取失败，请稍后重试"
}
