package com.example.airssreader.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.airssreader.core.state.FeedSelection
import com.example.airssreader.domain.model.Feed
import com.example.airssreader.ui.theme.Dimens
import com.example.airssreader.ui.theme.appColors
import com.example.airssreader.ui.theme.sourceColorFor

/**
 * 源切换条。
 *
 * 第一项固定是「全部」聚合视图，后面才是各个源。
 * 这个顺序是有意的：默认选中项在最左侧，用户打开就能看到所有源的最新内容，
 * 想只看某一个源再往右滑，不需要先理解「这里能切换」。
 *
 * 用横向滚动的文字 tab 而不是下拉菜单：
 * 源数量有限（默认 8 个），左右滑比多一层弹窗快。
 *
 * 选中态用「主色文字 + 底部 2dp 指示线」，不用胶囊背景，
 * 避免整条看起来像一排按钮。
 */
@Composable
fun SourceTabRow(
    feeds: List<Feed>,
    selectedFeedId: Long,
    totalUnreadCount: Int,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Dimens.PageHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
    ) {
        item(key = "all-feeds") {
            SourceTab(
                title = "全部",
                unreadCount = totalUnreadCount,
                selected = selectedFeedId == FeedSelection.ALL_FEED_ID,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = { onSelect(FeedSelection.ALL_FEED_ID) },
            )
        }

        items(feeds, key = { it.id }) { feed ->
            SourceTab(
                title = feed.title,
                unreadCount = feed.unreadCount,
                selected = feed.id == selectedFeedId,
                accentColor = sourceColorFor(feed.id),
                onClick = { onSelect(feed.id) },
            )
        }
    }
}

@Composable
private fun SourceTab(
    title: String,
    unreadCount: Int,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.appColors.textSecondary
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusSM))
            .clickable(onClick = onClick)
            .padding(
                horizontal = Dimens.SpaceMD,
                vertical = Dimens.SpaceSM,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 未选中时用小色点标记「这是哪个源」，与列表卡片的源标识色对应
            if (!selected) {
                Spacer(
                    Modifier
                        .width(Dimens.SourceDot)
                        .height(Dimens.SourceDot)
                        .clip(RoundedCornerShape(Dimens.SourceDot))
                        .background(accentColor),
                )
                Spacer(Modifier.width(Dimens.SpaceXS))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
            )

            if (unreadCount > 0) {
                Spacer(Modifier.width(Dimens.SpaceXS))
                UnreadBadge(count = unreadCount, selected = selected)
            }
        }

        // 指示线：只在选中态绘制，宽度与文字区一致
        Spacer(
            Modifier
                .padding(top = Dimens.SpaceXS)
                .width(Dimens.TabIndicatorWidth)
                .height(Dimens.StrokeThick)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
    }
}

@Composable
private fun UnreadBadge(count: Int, selected: Boolean) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.appColors.dividerStrong
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.appColors.textSecondary
    }

    Text(
        text = if (count > 99) "99+" else count.toString(),
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusXS))
            .background(bg)
            .padding(horizontal = Dimens.SpaceXS, vertical = Dimens.SpaceXXS),
    )
}
