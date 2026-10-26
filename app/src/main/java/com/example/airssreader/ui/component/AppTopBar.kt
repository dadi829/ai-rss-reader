package com.example.airssreader.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.airssreader.ui.theme.Dimens
import com.example.airssreader.ui.theme.appColors

/**
 * 统一顶栏。
 *
 * 不用 Material3 的 TopAppBar：它的默认高度与内边距偏大，
 * 这里用 Row 自行拼装，高度、字重、分割线都能精确控制。
 *
 * [statusBarsPadding] 加在 Column 上而不是 Row 上，
 * 这样状态栏高度不会挤压 52dp 的内容区。
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.TopBarHeight)
                .padding(
                    start = if (onBack == null) Dimens.PageHorizontal else Dimens.SpaceXS,
                    end = Dimens.SpaceXS,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.appColors.textPrimary,
                        modifier = Modifier.width(Dimens.IconLG),
                    )
                }
                Spacer(Modifier.width(Dimens.SpaceXS))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.appColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            actions()
        }

        HairlineDivider()
    }
}

/** 顶栏动作按钮的统一外观，保证三个图标的点击区与色调一致 */
@Composable
fun TopBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) {
                MaterialTheme.appColors.textSecondary
            } else {
                MaterialTheme.appColors.textTertiary
            },
            modifier = Modifier.width(Dimens.IconLG),
        )
    }
}

/** 0.5dp 发丝分割线，区块之间优先用它而不是加粗描边 */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.Hairline)
            .background(MaterialTheme.appColors.divider),
    )
}
