package com.example.airssreader.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.airssreader.ui.theme.Dimens
import com.example.airssreader.ui.theme.appColors

/**
 * 统一状态页：加载中 / 空 / 错误 / 未选择 共用一套版式。
 *
 * 好处是任何页面的「没内容」状态长得一样，不会出现有的居中有的靠上。
 */
@Composable
fun StateView(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.SpaceXXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.appColors.textSecondary,
            textAlign = TextAlign.Center,
        )

        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.appColors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = Dimens.SpaceSM)
                    .widthIn(max = Dimens.ContentMaxWidth),
            )
        }

        if (actionText != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier
                    .padding(top = Dimens.SpaceMD)
                    .height(Dimens.ButtonHeight),
            ) {
                Text(text = actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
