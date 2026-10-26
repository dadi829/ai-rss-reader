package com.example.airssreader.di

import android.content.Context
import com.example.airssreader.data.local.AppDatabase
import com.example.airssreader.data.remote.RssService
import com.example.airssreader.data.repository.ArticleRepository
import com.example.airssreader.data.repository.FeedRepository

/**
 * 手写依赖容器。
 *
 * 课程设计体量下没必要引入 Hilt：一个懒加载单例容器已经足够，
 * 依赖关系在构造处一眼可见，答辩时也更好讲清楚。
 */
class AppContainer(context: Context) {

    private val database: AppDatabase = AppDatabase.getInstance(context)

    private val rssService: RssService by lazy { RssService() }

    val feedRepository: FeedRepository by lazy {
        FeedRepository(
            feedDao = database.feedDao(),
            articleDao = database.articleDao(),
            rssService = rssService,
        )
    }

    val articleRepository: ArticleRepository by lazy {
        ArticleRepository(articleDao = database.articleDao())
    }
}
