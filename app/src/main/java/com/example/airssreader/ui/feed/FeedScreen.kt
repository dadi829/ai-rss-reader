package com.example.airssreader.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airssreader.core.util.DateBucket
import com.example.airssreader.core.util.DateFormatter
import com.example.airssreader.core.util.DateGrouper
import com.example.airssreader.domain.model.Article
import com.example.airssreader.ui.common.LocalAppContainer
import com.example.airssreader.ui.common.ViewModelFactories
import com.example.airssreader.ui.component.AppTopBar
import com.example.airssreader.ui.component.ArticleCard
import com.example.airssreader.ui.component.HairlineDivider
import com.example.airssreader.ui.component.ListBottomSpacer
import com.example.airssreader.ui.component.SourceTabRow
import com.example.airssreader.ui.component.StateView
import com.example.airssreader.ui.component.TopBarAction
import com.example.airssreader.ui.theme.Dimens
import com.example.airssreader.ui.theme.appColors

/**
 * 资讯流主页面。
 *
 * 布局自上而下：顶栏 → 源切换条（首位是「全部」）→ 列表。
 * 列表区域包在 PullToRefreshBox 里，下拉手势与顶栏刷新按钮走同一条
 * [FeedViewModel.refresh] 路径，都是用户主动触发。
 *
 * 两种列表形态共用同一套卡片，差别只在数据来源与分组：
 * - 聚合视图：跨源混排 + 日期分组 + 卡片显示来源；
 * - 单源视图：单一源按时间倒序，不需要分组也不需要来源名。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onOpenSearch: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenArticle: (Article) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: FeedViewModel = viewModel(factory = ViewModelFactories.feed(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FeedEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "AI 资讯",
                actions = {
                    if (uiState.isRefreshing) {
                        Box(
                            modifier = Modifier.size(Dimens.TouchTarget),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Dimens.IconMD),
                                strokeWidth = Dimens.StrokeThick,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        TopBarAction(
                            icon = Icons.Outlined.Refresh,
                            contentDescription = if (uiState.isAllSelected) "刷新全部源" else "刷新当前源",
                            onClick = viewModel::refresh,
                            enabled = uiState.feeds.isNotEmpty(),
                        )
                    }
                    TopBarAction(
                        icon = Icons.Outlined.Search,
                        contentDescription = "全局搜索",
                        onClick = onOpenSearch,
                    )
                    TopBarAction(
                        icon = Icons.Outlined.RssFeed,
                        contentDescription = "源管理",
                        onClick = onOpenSources,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (uiState.feeds.isNotEmpty()) {
                SourceTabRow(
                    feeds = uiState.feeds,
                    selectedFeedId = uiState.selectedFeedId,
                    totalUnreadCount = uiState.totalUnreadCount,
                    onSelect = viewModel::selectFeed,
                    modifier = Modifier.padding(vertical = Dimens.SpaceXS),
                )
                HairlineDivider()
            }

            when {
                uiState.isBootstrapping -> {
                    StateView(title = "正在读取本地缓存…", modifier = Modifier.weight(1f))
                }

                uiState.feeds.isEmpty() -> {
                    StateView(
                        title = "还没有订阅源",
                        description = "去「源管理」添加一个 AI 方向的 RSS 地址，或者恢复默认的 8 个预置源。",
                        actionText = "添加 RSS 源",
                        onAction = onOpenSources,
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.weight(1f),
                    ) {
                        ArticleList(
                            uiState = uiState,
                            onArticleClick = { article ->
                                viewModel.markRead(article.id)
                                onOpenArticle(article)
                            },
                            onMarkAllRead = viewModel::markAllRead,
                            onRefresh = viewModel::refresh,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleList(
    uiState: FeedUiState,
    onArticleClick: (Article) -> Unit,
    onMarkAllRead: () -> Unit,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.PageHorizontal,
            end = Dimens.PageHorizontal,
            top = Dimens.SpaceSM,
            bottom = Dimens.SpaceSM,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
    ) {
        if (uiState.articles.isEmpty()) {
            item(key = "empty") {
                Box(modifier = Modifier.fillParentMaxSize()) {
                    StateView(
                        title = if (uiState.isAllSelected) "还没有任何缓存内容" else "这个源还没有缓存内容",
                        description = if (uiState.isAllSelected) {
                            "点右上角刷新按钮，会一次性拉取全部 ${uiState.feeds.size} 个源的最新内容。"
                        } else {
                            "点右上角刷新按钮，或者下拉列表，才会去网络拉取。"
                        },
                        actionText = "立即刷新",
                        onAction = onRefresh,
                    )
                }
            }
        } else {
            item(key = "meta") {
                FeedMetaRow(
                    uiState = uiState,
                    onMarkAllRead = onMarkAllRead,
                )
            }

            if (uiState.isAllSelected) {
                groupByDate(uiState.articles).forEach { section ->
                    item(key = "header-${section.bucket.name}") {
                        DateSectionHeader(
                            label = section.bucket.label,
                            count = section.articles.size,
                        )
                    }
                    items(section.articles, key = { it.id }) { article ->
                        ArticleCard(
                            article = article,
                            showFeedName = true,
                            onClick = { onArticleClick(article) },
                        )
                    }
                }
            } else {
                items(uiState.articles, key = { it.id }) { article ->
                    ArticleCard(
                        article = article,
                        onClick = { onArticleClick(article) },
                    )
                }
            }

            item(key = "bottom") { ListBottomSpacer() }
        }
    }
}

/** 列表头部的信息条：源数、条数、未读数、缓存时间，右侧是「全部已读」 */
@Composable
private fun FeedMetaRow(
    uiState: FeedUiState,
    onMarkAllRead: () -> Unit,
) {
    val feed = uiState.selectedFeed
    val unreadCount = if (uiState.isAllSelected) uiState.totalUnreadCount else feed?.unreadCount ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceXXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildString {
                if (uiState.isAllSelected) {
                    append("${uiState.feeds.size} 个源 · ")
                }
                append("共 ${uiState.articles.size} 条")
                if (unreadCount > 0) {
                    append(" · $unreadCount 条未读")
                }
                // 聚合视图下每个源更新时间不同，报「上次更新」会误导，所以只在单源视图显示
                if (!uiState.isAllSelected && feed != null && feed.hasFetched) {
                    append(" · ")
                    append(DateFormatter.formatLastFetched(feed.lastFetchedAt))
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.appColors.textTertiary,
            modifier = Modifier.weight(1f),
        )

        if (unreadCount > 0) {
            Text(
                text = "全部已读",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusSM))
                    .clickable(onClick = onMarkAllRead)
                    .padding(horizontal = Dimens.SpaceSM, vertical = Dimens.SpaceXS),
            )
        }
    }
}

/** 日期分组标题：文字 + 条数 + 一条延伸到底的细线，弱化但不失结构感 */
@Composable
private fun DateSectionHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.SpaceMD, bottom = Dimens.SpaceXXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.appColors.textSecondary,
        )
        Spacer(Modifier.width(Dimens.SpaceSM))
        Text(
            text = "$count 条",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.appColors.textTertiary,
        )
        Spacer(Modifier.width(Dimens.SpaceSM))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(Dimens.Hairline)
                .background(MaterialTheme.appColors.divider),
        )
    }
}

/** 聚合视图里的一段：同属一个日期分组的条目 */
private data class ArticleSection(
    val bucket: DateBucket,
    val articles: List<Article>,
)

/**
 * 按日期分组。
 *
 * 输入已经按 publishedAt 倒序，groupBy 返回的 LinkedHashMap 保留首次出现顺序，
 * 再按 ordinal 排序只是兜底：即使未来查询排序变了，分组顺序也不会乱。
 */
private fun groupByDate(articles: List<Article>): List<ArticleSection> =
    articles.groupBy { DateGrouper.bucketOf(it.publishedAt) }
        .map { (bucket, list) -> ArticleSection(bucket, list) }
        .sortedBy { it.bucket.ordinal }
