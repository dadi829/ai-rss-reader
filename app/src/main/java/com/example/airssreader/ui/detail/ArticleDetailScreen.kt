package com.example.airssreader.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airssreader.core.util.DateFormatter
import com.example.airssreader.domain.model.Article
import com.example.airssreader.ui.common.LocalAppContainer
import com.example.airssreader.ui.common.ViewModelFactories
import com.example.airssreader.ui.component.AppTopBar
import com.example.airssreader.ui.component.HairlineDivider
import com.example.airssreader.ui.component.StateView
import com.example.airssreader.ui.theme.Dimens
import com.example.airssreader.ui.theme.appColors
import com.example.airssreader.ui.theme.sourceColorFor

/**
 * 资讯详情页。
 *
 * 存在的理由：原来点卡片直接跳 WebView，一旦断网或站点被墙，用户除了一个白屏
 * 什么都看不到——「离线可读」就成了一句空话。这一页把标题、来源、完整摘要
 * 全部用本地缓存渲染，断网时依然能读完一条资讯，想看全文再点底部按钮。
 *
 * 版式上刻意区别于列表：更大的标题、更疏的行高、没有卡片边界，
 * 让用户一眼确认「进入阅读态了」。
 */
@Composable
fun ArticleDetailScreen(
    articleId: Long,
    onBack: () -> Unit,
    onOpenOriginal: (url: String, title: String) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: ArticleDetailViewModel = viewModel(
        factory = ViewModelFactories.articleDetail(container, articleId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val article = uiState.article

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = article?.feedTitle ?: "资讯详情",
                onBack = onBack,
            )
        },
        bottomBar = {
            if (article != null) {
                OriginalLinkBar(
                    onClick = { onOpenOriginal(article.link, article.title) },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            when {
                uiState.isLoading -> StateView(title = "正在读取本地缓存…")

                article == null -> StateView(
                    title = "这条资讯已不存在",
                    description = "它可能随所属订阅源一起被删除了。",
                    actionText = "返回列表",
                    onAction = onBack,
                )

                else -> DetailContent(article = article)
            }
        }
    }
}

@Composable
private fun DetailContent(article: Article) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.PageHorizontal),
    ) {
        Spacer(Modifier.height(Dimens.SpaceLG))

        // 来源行：色点 + 源名 + 发布时间，与列表卡片的元信息保持同一套视觉语言
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                Modifier
                    .size(Dimens.SourceDot)
                    .background(sourceColorFor(article.feedId), CircleShape),
            )
            Spacer(Modifier.width(Dimens.SpaceSM))
            Text(
                text = article.feedTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.appColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(Dimens.SpaceSM))
            Text(
                text = DateFormatter.formatFullDateTime(article.publishedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.appColors.textTertiary,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(Dimens.SpaceMD))

        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.appColors.textPrimary,
            lineHeight = 32.sp,
        )

        Spacer(Modifier.height(Dimens.SpaceLG))
        HairlineDivider()
        Spacer(Modifier.height(Dimens.SpaceLG))

        Text(
            text = article.summary.ifBlank {
                "该源没有提供摘要。点下方按钮可以阅读原文全文。"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.appColors.textSecondary,
            lineHeight = Dimens.ReadingLineHeight,
        )

        Spacer(Modifier.height(Dimens.SpaceXL))

        Text(
            text = "以上内容已缓存到本地，断网时依然可以阅读。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.appColors.textTertiary,
        )

        Spacer(Modifier.height(Dimens.SpaceXXXL))
    }
}

/** 底部固定操作条：打开原文是详情页唯一的对外出口，不该被滚动埋没 */
@Composable
private fun OriginalLinkBar(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HairlineDivider()
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(Dimens.RadiusMD),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.PageHorizontal, vertical = Dimens.SpaceMD)
                .height(Dimens.ButtonHeight),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconSM),
                )
                Spacer(Modifier.width(Dimens.SpaceSM))
                Text(text = "阅读原文", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
