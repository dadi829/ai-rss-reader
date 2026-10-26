package com.example.airssreader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airssreader.data.repository.ArticleRepository
import com.example.airssreader.domain.model.Article
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 详情页状态：区分「还在查库」与「确实查不到」 */
data class DetailUiState(
    val isLoading: Boolean = true,
    val article: Article? = null,
)

/**
 * 资讯详情 ViewModel。
 *
 * 只按 id 订阅单条记录，不接收整个 Article 对象：
 * 1. 导航参数里塞长文本容易超限，传 id 最稳；
 * 2. 已读状态改动会通过 Room 的 Flow 自动推回来，页面不需要手动刷新。
 *
 * 进入页面即标记已读——这是阅读器的通行做法，
 * 用户点开就已经看到了标题和摘要，继续算未读没有意义。
 */
class ArticleDetailViewModel(
    private val articleRepository: ArticleRepository,
    private val articleId: Long,
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = articleRepository.observeById(articleId)
        .map { DetailUiState(isLoading = false, article = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState(),
        )

    init {
        viewModelScope.launch { articleRepository.markRead(articleId) }
    }
}
