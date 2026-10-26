package com.example.airssreader.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.airssreader.di.AppContainer
import com.example.airssreader.ui.feed.FeedViewModel
import com.example.airssreader.ui.search.SearchViewModel
import com.example.airssreader.ui.sources.SourcesViewModel

/**
 * ViewModel 工厂集合。
 *
 * 用 lifecycle 提供的 viewModelFactory DSL，比手写 Factory 子类少一层样板代码，
 * 也不需要引入 DI 框架。
 */
object ViewModelFactories {

    fun feed(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            FeedViewModel(
                feedRepository = container.feedRepository,
                articleRepository = container.articleRepository,
            )
        }
    }

    fun sources(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            SourcesViewModel(feedRepository = container.feedRepository)
        }
    }

    fun search(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            SearchViewModel(articleRepository = container.articleRepository)
        }
    }
}
