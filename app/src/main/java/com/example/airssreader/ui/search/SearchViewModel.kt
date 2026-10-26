package com.example.airssreader.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airssreader.data.repository.ArticleRepository
import com.example.airssreader.domain.model.Article
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 搜索页状态 */
data class SearchUiState(
    /** 输入框里的原始文本，用于高亮和展示 */
    val query: String = "",
    val results: List<Article> = emptyList(),
    /** 已缓存的条目总数，空态时告诉用户「搜的是哪些数据」 */
    val totalCached: Int = 0,
)

/**
 * 全局搜索 ViewModel。
 *
 * 搜索范围是**所有源已经缓存到 Room 的条目**，不是源名称，也不是在线检索。
 * 输入做 200ms 防抖：连续输入时只查最后一次，避免每敲一个字都跑一次 SQL。
 */
class SearchViewModel(
    private val articleRepository: ArticleRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val results: StateFlow<List<Article>> = _query
        .debounce(200L)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { keyword ->
            if (keyword.isEmpty()) {
                flowOf(emptyList())
            } else {
                articleRepository.search(keyword)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        results,
        articleRepository.observeTotalCachedCount(),
    ) { query, resultList, total ->
        SearchUiState(
            query = query,
            results = resultList,
            totalCached = total,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun markRead(articleId: Long) {
        viewModelScope.launch { articleRepository.markRead(articleId) }
    }
}
