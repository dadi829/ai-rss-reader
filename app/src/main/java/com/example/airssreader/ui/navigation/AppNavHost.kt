package com.example.airssreader.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.airssreader.ui.detail.ArticleDetailScreen
import com.example.airssreader.ui.feed.FeedScreen
import com.example.airssreader.ui.reader.ReaderScreen
import com.example.airssreader.ui.search.SearchScreen
import com.example.airssreader.ui.sources.SourcesScreen

/**
 * 路由表。
 *
 * 页面五个：资讯流（首页）、源管理、全局搜索、资讯详情、原文阅读。
 * 没有底部导航——阅读器的核心动作就一个「看资讯」，
 * 搜索和源管理都是低频入口，放顶栏更合适。
 *
 * 详情页夹在列表与 WebView 之间：列表 → 详情（本地渲染）→ 原文（联网）。
 * 这样断网时链路在第二跳就停住，而不是直接把用户扔进一个白屏。
 */
object Routes {
    const val FEED = "feed"
    const val SOURCES = "sources"
    const val SEARCH = "search"

    /** 详情页只传 id，标题和摘要由 Room 取，避免导航参数里塞长文本 */
    const val ARTICLE = "article/{articleId}"

    /** URL 与标题都走 query 参数，避免链接里的斜杠破坏路径匹配 */
    const val READER = "reader?url={url}&title={title}"

    fun article(articleId: Long): String = "article/$articleId"

    fun reader(url: String, title: String): String =
        "reader?url=${Uri.encode(url)}&title=${Uri.encode(title)}"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.FEED,
    ) {
        composable(Routes.FEED) {
            FeedScreen(
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenSources = { navController.navigate(Routes.SOURCES) },
                onOpenArticle = { article ->
                    navController.navigate(Routes.article(article.id))
                },
            )
        }

        composable(Routes.SOURCES) {
            SourcesScreen(
                onBack = { navController.popBackStack() },
                // 选中源后直接回到资讯流，不再多点一次
                onOpenFeed = { navController.popBackStack() },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenArticle = { article ->
                    navController.navigate(Routes.article(article.id))
                },
            )
        }

        composable(
            route = Routes.ARTICLE,
            arguments = listOf(
                navArgument("articleId") { type = NavType.LongType },
            ),
        ) { entry ->
            ArticleDetailScreen(
                articleId = entry.arguments?.getLong("articleId") ?: 0L,
                onBack = { navController.popBackStack() },
                onOpenOriginal = { url, title ->
                    navController.navigate(Routes.reader(url, title))
                },
            )
        }

        composable(
            route = Routes.READER,
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            ReaderScreen(
                url = entry.arguments?.getString("url").orEmpty(),
                title = entry.arguments?.getString("title").orEmpty().ifBlank { "原文" },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
