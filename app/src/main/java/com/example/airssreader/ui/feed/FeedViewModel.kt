package com.example.airssreader.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airssreader.core.state.FeedSelection
import com.example.airssreader.data.repository.ArticleRepository
import com.example.airssreader.data.repository.FeedRepository
import com.example.airssreader.data.repository.RefreshAllResult
import com.example.airssreader.data.repository.RefreshResult
import com.example.airssreader.domain.model.Article
import com.example.airssreader.domain.model.Feed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** 资讯流页面的完整状态 */
data class FeedUiState(
    val feeds: List<Feed> = emptyList(),
    val selectedFeedId: Long = FeedSelection.ALL_FEED_ID,
    /** 聚合视图下为 null，单源视图下是被选中的源 */
    val selectedFeed: Feed? = null,
    val articles: List<Article> = emptyList(),
    /** 正在执行用户手动触发的刷新 */
    val isRefreshing: Boolean = false,
    /** 启动初期等待数据库播种完成，避免闪一下「没有订阅源」 */
    val isBootstrapping: Boolean = true,
) {
    val isAllSelected: Boolean get() = selectedFeedId == FeedSelection.ALL_FEED_ID

    /** 未读总数直接从 feeds 的统计字段求和，不用再发一次 COUNT 查询 */
    val totalUnreadCount: Int get() = feeds.sumOf { it.unreadCount }
}

/** 一次性提示事件，用 Channel 而不是 StateFlow，避免旋转屏幕后重放 */
sealed interface FeedEvent {
    data class Message(val text: String) : FeedEvent
}

/**
 * 资讯流 ViewModel。
 *
 * 设计要点：
 * 1. 切源只是切换本地查询范围，**不会触发任何网络请求**；
 * 2. 唯一的网络入口是 [refresh]，由刷新按钮和下拉手势显式调用；
 * 3. 列表数据全部来自 Room 的 Flow，写入后自动推送，不需要手工 reload；
 * 4. 聚合视图与单源视图共用一套状态，差别只在于 [selectedFeedId] 的取值。
 */
class FeedViewModel(
    private val feedRepository: FeedRepository,
    private val articleRepository: ArticleRepository,
) : ViewModel() {

    /** 选中源放在进程级单例里，源管理页可以直接把用户切过去 */
    private val _selectedFeedId = FeedSelection.selectedFeedId
    private val _isRefreshing = MutableStateFlow(false)
    private val _isBootstrapping = MutableStateFlow(true)

    private val _events = Channel<FeedEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val feeds: StateFlow<List<Feed>> = feedRepository.observeFeeds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val articles: StateFlow<List<Article>> = _selectedFeedId
        .flatMapLatest { feedId ->
            if (feedId == FeedSelection.ALL_FEED_ID) {
                articleRepository.observeAll()
            } else {
                articleRepository.observeByFeed(feedId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<FeedUiState> = combine(
        feeds,
        articles,
        _selectedFeedId,
        _isRefreshing,
        _isBootstrapping,
    ) { feedList, articleList, selectedId, refreshing, bootstrapping ->
        FeedUiState(
            feeds = feedList,
            selectedFeedId = selectedId,
            selectedFeed = feedList.firstOrNull { it.id == selectedId },
            articles = articleList,
            isRefreshing = refreshing,
            isBootstrapping = bootstrapping,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(),
    )

    init {
        // 选中项跟随源列表变化：聚合视图永远有效；单源被删则回落到聚合视图
        viewModelScope.launch {
            feeds.collect { list ->
                val current = _selectedFeedId.value
                val stillValid = current == FeedSelection.ALL_FEED_ID ||
                    list.any { it.id == current }
                if (!stillValid) {
                    _selectedFeedId.value = FeedSelection.ALL_FEED_ID
                }
            }
        }

        // 等播种完成（或 800ms 兜底），再撤掉启动占位
        viewModelScope.launch {
            withTimeoutOrNull(800L) { feeds.first { it.isNotEmpty() } }
            _isBootstrapping.value = false
        }
    }

    fun selectFeed(feedId: Long) {
        if (_selectedFeedId.value == feedId) return
        _selectedFeedId.value = feedId
    }

    /**
     * 手动刷新。
     *
     * 这是本 App 唯一发起网络请求的地方，由用户点击刷新按钮或下拉手势触发。
     * 聚合视图下刷新全部源，单源视图下只刷新当前源——语义与用户看到的一致。
     */
    fun refresh() {
        if (_isRefreshing.value) return
        val feedId = _selectedFeedId.value

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (feedId == FeedSelection.ALL_FEED_ID) {
                    _events.send(FeedEvent.Message(describe(feedRepository.refreshAll())))
                } else {
                    _events.send(FeedEvent.Message(describe(feedRepository.refresh(feedId))))
                }
            } finally {
                // 放在 finally 里：即使上面抛异常，转圈动画也必须停下来
                _isRefreshing.value = false
            }
        }
    }

    fun markAllRead() {
        val feedId = _selectedFeedId.value
        viewModelScope.launch {
            if (feedId == FeedSelection.ALL_FEED_ID) {
                articleRepository.markAllReadGlobally()
                _events.send(FeedEvent.Message("已把所有资讯标为已读"))
            } else {
                feedRepository.markAllRead(feedId)
                _events.send(FeedEvent.Message("已将该源全部标为已读"))
            }
        }
    }

    /** 打开详情时立即落库，返回列表后未读点会消失 */
    fun markRead(articleId: Long) {
        viewModelScope.launch { articleRepository.markRead(articleId) }
    }

    private fun describe(result: RefreshResult): String = when (result) {
        is RefreshResult.Success ->
            if (result.newItemCount > 0) {
                "已更新 ${result.newItemCount} 条新资讯"
            } else {
                "已是最新，没有新内容"
            }

        is RefreshResult.Failure -> "刷新失败：${result.message}"
    }

    private fun describe(result: RefreshAllResult): String = when {
        result.feedCount == 0 -> "请先在「源管理」添加一个 RSS 源"
        // 全部失败：直接暴露第一个错误原因，比「0 个成功」有用得多
        result.successCount == 0 ->
            "刷新失败：${result.firstErrorMessage ?: "请检查网络后重试"}"

        result.failedCount == 0 ->
            if (result.newItemCount > 0) {
                "已更新 ${result.newItemCount} 条新资讯"
            } else {
                "全部源已是最新"
            }

        // 部分成功：先说拿到多少，再说挂了几个，用户能判断要不要重试
        else -> "已更新 ${result.newItemCount} 条，${result.failedCount} 个源刷新失败"
    }
}
