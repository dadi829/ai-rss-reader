package com.example.airssreader.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.airssreader.core.util.DateFormatter
import com.example.airssreader.domain.model.Article
import com.example.airssreader.ui.theme.Dimens
import com.example.airssreader.ui.theme.appColors

/**
 * 资讯条目。
 *
 * 版式说明：
 * 1. 未读用左侧 6dp 实心点标记，已读去掉点并把标题降为次级色——
 *    比整行加底色更克制，也不会让列表显得花；
 * 2. 标题最多 3 行、摘要最多 3 行，超出省略，保证单条高度可控；
 * 3. 卡片无阴影无描边，靠 8dp 圆角 + 灰白背景上的白色块形成分区。
 */
@Composable
fun ArticleCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFeedName: Boolean = false,
    highlightKeyword: String = "",
) {
    val highlightStyle = androidx.compose.ui.text.SpanStyle(
        background = MaterialTheme.appColors.searchHighlight,
        color = MaterialTheme.appColors.textPrimary,
        fontWeight = FontWeight.Medium,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMD))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(
                horizontal = Dimens.SpaceLG,
                vertical = Dimens.SpaceMD,
            ),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (!article.isRead) {
                // 圆点与标题首行视觉居中对齐
                Spacer(
                    Modifier
                        .padding(top = Dimens.SpaceSM)
                        .size(Dimens.UnreadDot)
                        .background(
                            color = MaterialTheme.appColors.unreadDot,
                            shape = RoundedCornerShape(Dimens.UnreadDot),
                        ),
                )
                Spacer(Modifier.width(Dimens.SpaceSM))
            }

            Text(
                text = buildHighlightedText(article.title, highlightKeyword, highlightStyle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (article.isRead) FontWeight.Normal else FontWeight.Medium,
                color = if (article.isRead) {
                    MaterialTheme.appColors.textSecondary
                } else {
                    MaterialTheme.appColors.textPrimary
                },
                maxLines = Dimens.TitleMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        if (article.summary.isNotBlank()) {
            Text(
                text = buildHighlightedText(article.summary, highlightKeyword, highlightStyle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.appColors.textTertiary,
                maxLines = Dimens.SummaryMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Dimens.SpaceXS),
            )
        }

        Row(
            modifier = Modifier.padding(top = Dimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            Text(
                text = DateFormatter.formatRelative(article.publishedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.appColors.textTertiary,
            )
            if (showFeedName) {
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.appColors.textTertiary,
                )
                Text(
                    text = article.feedTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.appColors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

/** 列表底部留白，避免最后一张卡片贴着导航栏 */
@Composable
fun ListBottomSpacer() {
    Spacer(Modifier.size(Dimens.SpaceXXL))
}
