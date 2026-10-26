package com.example.airssreader.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.airssreader.ui.feed.FeedScreen
import com.example.airssreader.ui.reader.ReaderScreen
import com.example.airssreader.ui.search.SearchScreen
import com.example.airssreader.ui.sources.SourcesScreen

/**
 * 路由表。
 *
 * 页面只有四个：资讯流（首页）、源管理、全局搜索、原文阅读。
 * 没有底部导航——阅读器的核心动作就一个「看资讯」，
 * 搜索和源管理都是低频入口，放顶栏更合适。
 */
object Routes {
    const val FEED = "feed"
    const val SOURCES = "sources"
    const val SEARCH = "search"

    /** URL 与标题都走 query 参数，避免链接里的斜杠破坏路径匹配 */
    const val READER = "reader?url={url}&title={title}"

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
                    navController.navigate(Routes.reader(article.link, article.title))
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
                    navController.navigate(Routes.reader(article.link, article.title))
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
