package com.example.airssreader.ui.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airssreader.domain.model.Article
import com.example.airssreader.ui.common.LocalAppContainer
import com.example.airssreader.ui.common.ViewModelFactories
import com.example.airssreader.ui.component.AppSearchField
import com.example.airssreader.ui.component.ArticleCard
import com.example.airssreader.ui.component.HairlineDivider
import com.example.airssreader.ui.component.ListBottomSpacer
import com.example.airssreader.ui.component.StateView
import com.example.airssreader.ui.theme.Dimens
import com.example.airssreader.ui.theme.appColors

/**
 * 全局搜索页。
 *
 * 这是本 App 与「浏览器书签」的核心差异：书签只能按标题找收藏，
 * 这里可以跨全部订阅源、在标题和摘要里一起检索已经缓存下来的内容，
 * 而且完全离线——断网也能搜出结果。
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenArticle: (Article) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SearchViewModel = viewModel(factory = ViewModelFactories.search(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SearchTopBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            when {
                uiState.query.isBlank() -> {
                    StateView(
                        title = "跨源搜索已缓存的资讯",
                        description = "当前共缓存 ${uiState.totalCached} 条，搜索会同时匹配标题与摘要，" +
                            "覆盖所有订阅源，离线也能用。",
                    )
                }

                uiState.results.isEmpty() -> {
                    StateView(
                        title = "没有匹配「${uiState.query.trim()}」的资讯",
                        description = "搜索只覆盖已经缓存的内容。如果这个源还没刷新过，" +
                            "先回资讯流点一次刷新再来搜。",
                    )
                }

                else -> {
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
                        item(key = "count") {
                            Text(
                                text = "找到 ${uiState.results.size} 条",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.appColors.textTertiary,
                                modifier = Modifier.padding(vertical = Dimens.SpaceXXS),
                            )
                        }

                        items(uiState.results, key = { it.id }) { article ->
                            ArticleCard(
                                article = article,
                                onClick = {
                                    viewModel.markRead(article.id)
                                    onOpenArticle(article)
                                },
                                showFeedName = true,
                                highlightKeyword = uiState.query.trim(),
                            )
                        }

                        item(key = "bottom") { ListBottomSpacer() }
                    }
                }
            }
        }
    }
}

/** 顶栏里直接嵌搜索框，进页面即可输入，少一次点击 */
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.TopBarHeight)
                .padding(end = Dimens.PageHorizontal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.appColors.textPrimary,
                )
            }

            AppSearchField(
                value = query,
                onValueChange = onQueryChange,
                autoFocus = true,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(Dimens.SpaceSM))
        }

        HairlineDivider()
    }
}
