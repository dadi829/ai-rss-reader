package com.example.airssreader.ui.reader

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.airssreader.ui.component.AppTopBar
import com.example.airssreader.ui.component.StateView
import com.example.airssreader.ui.component.TopBarAction

/**
 * 原文阅读页：内置 WebView 打开资讯链接。
 *
 * 行为约定：
 * 1. 站内跳转留在 WebView 内，不跳到外部浏览器；
 * 2. 系统返回键优先回退网页历史，退无可退才关页面；
 * 3. 页面销毁时显式 destroy，避免 WebView 持有 Activity 造成泄漏。
 */
@Composable
fun ReaderScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    // 用普通持有类而不是 mutableStateOf：WebView 引用变化不该触发重组
    val holder = remember { WebViewHolder() }
    var loadFailed by remember { mutableStateOf(false) }

    BackHandler {
        val webView = holder.webView
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            onBack()
        }
    }

    // 离开页面时释放 WebView，否则它会一直持有 Activity 的 Context。
    // 先把它从父容器摘掉再 destroy，避免 Compose 仍在引用一个已销毁的 View。
    DisposableEffect(Unit) {
        onDispose {
            holder.webView?.let { view ->
                runCatching {
                    view.stopLoading()
                    (view.parent as? ViewGroup)?.removeView(view)
                    view.destroy()
                }
            }
            holder.webView = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    TopBarAction(
                        icon = Icons.Outlined.OpenInBrowser,
                        contentDescription = "用浏览器打开",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                )
                            }
                        },
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
            Box(modifier = Modifier.fillMaxSize()) {
                if (loadFailed) {
                    StateView(
                        title = "页面加载失败",
                        description = "可能是网络问题，或者该站点拒绝在 WebView 中打开。" +
                            "可以点右上角用系统浏览器打开原文。",
                    )
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            createWebView(
                                context = ctx,
                                url = url,
                                onCreated = { holder.webView = it },
                                onPageError = { loadFailed = true },
                                onPageStart = { loadFailed = false },
                            )
                        },
                    )
                }
            }
        }
    }
}

private class WebViewHolder {
    var webView: WebView? = null
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: android.content.Context,
    url: String,
    onCreated: (WebView) -> Unit,
    onPageError: () -> Unit,
    onPageStart: () -> Unit,
): WebView = WebView(context).apply {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        // 让页面按屏幕宽度排版，而不是缩成桌面视图
        useWideViewPort = true
        loadWithOverviewMode = true
        builtInZoomControls = false
        displayZoomControls = false
        // 部分中文站点靠 UA 判断是否返回移动版页面
        userAgentString = "$userAgentString AiRssReader/1.0"
    }

    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            onPageStart()
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
        ) {
            // 只关心主文档失败，子资源（图片、统计脚本）失败不打断阅读
            if (request?.isForMainFrame == true) onPageError()
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean = false
    }

    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    loadUrl(url)
    onCreated(this)
}
