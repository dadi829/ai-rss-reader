package com.example.airssreader.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airssreader.core.state.FeedSelection
import com.example.airssreader.data.repository.AddFeedResult
import com.example.airssreader.data.repository.FeedRepository
import com.example.airssreader.domain.model.Feed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 源管理页面状态 */
data class SourcesUiState(
    val feeds: List<Feed> = emptyList(),
    /** 正在验证并添加，按钮需要禁用防重复提交 */
    val isAdding: Boolean = false,
    /** 添加失败的原因，展示在输入框下方 */
    val addError: String? = null,
)

sealed interface SourcesEvent {
    data class Message(val text: String) : SourcesEvent
}

/**
 * 源管理 ViewModel。
 *
 * 添加源会真实访问一次网络（验证地址 + 抓标题 + 首次缓存），
 * 这同样是用户点击「添加」主动触发的，不存在后台自动执行。
 */
class SourcesViewModel(
    private val feedRepository: FeedRepository,
) : ViewModel() {

    private val _isAdding = MutableStateFlow(false)
    private val _addError = MutableStateFlow<String?>(null)

    private val _events = Channel<SourcesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<SourcesUiState> = combine(
        feedRepository.observeFeeds(),
        _isAdding,
        _addError,
    ) { feeds, adding, error ->
        SourcesUiState(feeds = feeds, isAdding = adding, addError = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SourcesUiState(),
    )

    fun clearAddError() {
        if (_addError.value != null) _addError.value = null
    }

    /**
     * 添加源。
     *
     * [onSuccess] 在验证通过后回调，供 UI 关闭对话框并清空输入。
     */
    fun addFeed(rawUrl: String, onSuccess: () -> Unit) {
        if (_isAdding.value) return

        viewModelScope.launch {
            _isAdding.value = true
            _addError.value = null

            when (val result = feedRepository.addFeed(rawUrl)) {
                is AddFeedResult.Success -> {
                    _isAdding.value = false
                    _events.send(SourcesEvent.Message("已添加「${result.feedTitle}」并缓存最新内容"))
                    onSuccess()
                }

                AddFeedResult.Duplicate -> {
                    _isAdding.value = false
                    _addError.value = "这个地址已经订阅过了"
                }

                is AddFeedResult.Failure -> {
                    _isAdding.value = false
                    _addError.value = result.message
                }
            }
        }
    }

    /** 删除源会连带清掉该源已缓存的条目，所以 UI 侧必须二次确认 */
    fun deleteFeed(feed: Feed) {
        viewModelScope.launch {
            feedRepository.deleteFeed(feed.id)
            if (FeedSelection.selectedFeedId.value == feed.id) {
                FeedSelection.selectedFeedId.value = null
            }
            _events.send(SourcesEvent.Message("已删除「${feed.title}」及其缓存"))
        }
    }

    /** 从源管理跳到资讯流：只切选中项，不发起任何请求 */
    fun selectFeed(feedId: Long) {
        FeedSelection.selectedFeedId.value = feedId
    }
}
