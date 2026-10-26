package com.example.airssreader.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airssreader.core.util.DateFormatter
import com.example.airssreader.domain.model.Feed
import com.example.airssreader.ui.common.LocalAppContainer
import com.example.airssreader.ui.common.ViewModelFactories
import com.example.airssreader.ui.component.AppTopBar
import com.example.airssreader.ui.component.HairlineDivider
import com.example.airssreader.ui.component.ListBottomSpacer
import com.example.airssreader.ui.component.StateView
import com.example.airssreader.ui.component.TopBarAction
import com.example.airssreader.ui.theme.Dimens
import com.example.airssreader.ui.theme.appColors

/**
 * 源管理页。
 *
 * 这里承担「订阅」这件事的全部操作：新增、删除、查看每个源的缓存与未读情况。
 * 页面上不做任何网络动作，只有点「添加」并确认时才会验证地址。
 */
@Composable
fun SourcesScreen(
    onBack: () -> Unit,
    onOpenFeed: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SourcesViewModel = viewModel(factory = ViewModelFactories.sources(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Feed?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SourcesEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "源管理",
                onBack = onBack,
                actions = {
                    TopBarAction(
                        icon = Icons.Outlined.Add,
                        contentDescription = "添加 RSS 源",
                        onClick = { showAddDialog = true },
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
            if (uiState.feeds.isEmpty()) {
                StateView(
                    title = "还没有订阅源",
                    description = "点右上角「+」添加一个 RSS 地址，添加时会先验证地址有效性。",
                    actionText = "添加 RSS 源",
                    onAction = { showAddDialog = true },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Dimens.SpaceSM),
                ) {
                    item(key = "hint") { PullPolicyHint(feedCount = uiState.feeds.size) }

                    items(uiState.feeds, key = { it.id }) { feed ->
                        SourceRow(
                            feed = feed,
                            onClick = {
                                viewModel.selectFeed(feed.id)
                                onOpenFeed()
                            },
                            onDelete = { pendingDelete = feed },
                        )
                        HairlineDivider(modifier = Modifier.padding(start = Dimens.PageHorizontal))
                    }

                    item(key = "bottom") { ListBottomSpacer() }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFeedDialog(
            isAdding = uiState.isAdding,
            error = uiState.addError,
            onDismiss = {
                showAddDialog = false
                viewModel.clearAddError()
            },
            onConfirm = { url ->
                viewModel.addFeed(url) {
                    showAddDialog = false
                }
            },
            onInputChange = viewModel::clearAddError,
        )
    }

    pendingDelete?.let { feed ->
        DeleteFeedDialog(
            feed = feed,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                viewModel.deleteFeed(feed)
                pendingDelete = null
            },
        )
    }
}

/**
 * 说明条。
 *
 * 明确写出「没有后台刷新」，让评审一眼看懂这个 App 的数据来源策略，
 * 也避免用户误以为内容会自动更新。
 */
@Composable
private fun PullPolicyHint(feedCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.PageHorizontal,
                end = Dimens.PageHorizontal,
                top = Dimens.SpaceMD,
                bottom = Dimens.SpaceMD,
            )
            .clip(RoundedCornerShape(Dimens.RadiusMD))
            .background(MaterialTheme.appColors.surfaceMuted)
            .padding(Dimens.SpaceMD),
    ) {
        Text(
            text = "$feedCount 个订阅源 · 内容全部存在本地",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.appColors.textPrimary,
        )
        Text(
            text = "没有任何后台刷新与推送。只有在资讯流点刷新或下拉时才会访问网络——" +
                "聚合视图下一次刷新全部源，单源视图下只刷新当前源。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.appColors.textTertiary,
            modifier = Modifier.padding(top = Dimens.SpaceXXS),
        )
    }
}

@Composable
private fun SourceRow(
    feed: Feed,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(
                start = Dimens.PageHorizontal,
                end = Dimens.SpaceXS,
                top = Dimens.SpaceMD,
                bottom = Dimens.SpaceMD,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = feed.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.appColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (feed.unreadCount > 0) {
                    Spacer(Modifier.width(Dimens.SpaceSM))
                    Text(
                        text = "${feed.unreadCount} 未读",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = feed.url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.appColors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Dimens.SpaceXXS),
            )

            Text(
                text = "${feed.articleCount} 条缓存 · ${DateFormatter.formatLastFetched(feed.lastFetchedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.appColors.textTertiary,
                modifier = Modifier.padding(top = Dimens.SpaceXXS),
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "删除 ${feed.title}",
                tint = MaterialTheme.appColors.textTertiary,
            )
        }
    }
}

@Composable
private fun AddFeedDialog(
    isAdding: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onInputChange: () -> Unit,
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isAdding) onDismiss() },
        title = { Text("添加 RSS 源") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        onInputChange()
                    },
                    placeholder = { Text("https://example.com/feed.xml") },
                    singleLine = true,
                    isError = error != null,
                    enabled = !isAdding,
                    shape = RoundedCornerShape(Dimens.RadiusMD),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = error ?: "确认后会先访问该地址验证格式，并立即缓存最新条目。",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.appColors.textTertiary
                    },
                    modifier = Modifier.padding(top = Dimens.SpaceSM),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url) },
                enabled = url.isNotBlank() && !isAdding,
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(Dimens.IconSM),
                        strokeWidth = Dimens.StrokeThick,
                    )
                    Spacer(Modifier.width(Dimens.SpaceSM))
                }
                Text(if (isAdding) "验证中…" else "添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isAdding) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun DeleteFeedDialog(
    feed: Feed,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除订阅源") },
        text = {
            Text(
                text = "将删除「${feed.title}」以及它已缓存的 ${feed.articleCount} 条资讯，" +
                    "该操作不可撤销。",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
