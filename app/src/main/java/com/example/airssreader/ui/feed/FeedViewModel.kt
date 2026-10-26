package com.example.airssreader.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airssreader.core.state.FeedSelection
import com.example.airssreader.data.repository.ArticleRepository
import com.example.airssreader.data.repository.FeedRepository
import com.example.airssreader.data.repository.RefreshResult
import com.example.airssreader.domain.model.Article
import com.example.airssreader.domain.model.Feed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** 资讯流页面的完整状态 */
data class FeedUiState(
    val feeds: List<Feed> = emptyList(),
    val selectedFeedId: Long? = null,
    val selectedFeed: Feed? = null,
    val articles: List<Article> = emptyList(),
    /** 正在执行用户手动触发的刷新 */
    val isRefreshing: Boolean = false,
    /** 启动初期等待数据库播种完成，避免闪一下「没有订阅源」 */
    val isBootstrapping: Boolean = true,
)

/** 一次性提示事件，用 Channel 而不是 StateFlow，避免旋转屏幕后重放 */
sealed interface FeedEvent {
    data class Message(val text: String) : FeedEvent
}

/**
 * 资讯流 ViewModel。
 *
 * 设计要点：
 * 1. 切源只是切换本地查询的 feedId，**不会触发任何网络请求**；
 * 2. 唯一的网络入口是 [refresh]，由刷新按钮和下拉手势显式调用；
 * 3. 列表数据全部来自 Room 的 Flow，写入后自动推送，不需要手工 reload。
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
            if (feedId == null) flowOf(emptyList()) else articleRepository.observeByFeed(feedId)
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
        // 选中项跟随源列表变化：默认选中第一个，源被删则回落到新的第一个
        viewModelScope.launch {
            feeds.collect { list ->
                val current = _selectedFeedId.value
                if (current == null || list.none { it.id == current }) {
                    _selectedFeedId.value = list.firstOrNull()?.id
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
     * 手动刷新当前源。
     *
     * 这是本 App 唯一发起网络请求的地方，由用户点击刷新按钮或下拉手势触发。
     */
    fun refresh() {
        val feedId = _selectedFeedId.value
        if (feedId == null) {
            viewModelScope.launch { _events.send(FeedEvent.Message("请先在「源管理」添加一个 RSS 源")) }
            return
        }
        if (_isRefreshing.value) return

        viewModelScope.launch {
            _isRefreshing.value = true
            val result = feedRepository.refresh(feedId)
            _isRefreshing.value = false

            when (result) {
                is RefreshResult.Success -> {
                    val text = if (result.newItemCount > 0) {
                        "已更新 ${result.newItemCount} 条新资讯"
                    } else {
                        "已是最新，没有新内容"
                    }
                    _events.send(FeedEvent.Message(text))
                }

                is RefreshResult.Failure -> {
                    _events.send(FeedEvent.Message("刷新失败：${result.message}"))
                }
            }
        }
    }

    fun markAllRead() {
        val feedId = _selectedFeedId.value ?: return
        viewModelScope.launch {
            feedRepository.markAllRead(feedId)
            _events.send(FeedEvent.Message("已将该源全部标为已读"))
        }
    }

    /** 打开详情时立即落库，返回列表后未读点会消失 */
    fun markRead(articleId: Long) {
        viewModelScope.launch { articleRepository.markRead(articleId) }
    }
}
