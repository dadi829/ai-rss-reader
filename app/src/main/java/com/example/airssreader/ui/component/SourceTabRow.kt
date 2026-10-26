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
import com.example.airssreader.domain.model.Feed
import com.example.airssreader.ui.theme.Dimens
import com.example.airssreader.ui.theme.appColors

/**
 * 源切换条。
 *
 * 用横向滚动的文字 tab 而不是下拉菜单：
 * 源数量有限（默认 6 个），一屏内左右滑即可切换，比多一层弹窗快。
 *
 * 选中态用「主色文字 + 底部 2dp 指示线」，不用胶囊背景，
 * 避免整条看起来像一排按钮。
 */
@Composable
fun SourceTabRow(
    feeds: List<Feed>,
    selectedFeedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Dimens.PageHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
    ) {
        items(feeds, key = { it.id }) { feed ->
            SourceTab(
                feed = feed,
                selected = feed.id == selectedFeedId,
                onClick = { onSelect(feed.id) },
            )
        }
    }
}

@Composable
private fun SourceTab(
    feed: Feed,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val indicatorColor = MaterialTheme.colorScheme.primary
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
            Text(
                text = feed.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
            )
            if (feed.unreadCount > 0) {
                Spacer(Modifier.width(Dimens.SpaceXS))
                UnreadBadge(count = feed.unreadCount, selected = selected)
            }
        }

        // 指示线：只在选中态绘制，宽度与文字区一致
        Spacer(
            Modifier
                .padding(top = Dimens.SpaceXS)
                .width(Dimens.TabIndicatorWidth)
                .height(Dimens.StrokeThick)
                .background(if (selected) indicatorColor else Color.Transparent),
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
