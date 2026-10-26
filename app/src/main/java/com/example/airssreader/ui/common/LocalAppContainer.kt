package com.example.airssreader.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.airssreader.di.AppContainer

/**
 * 把依赖容器放进组合树。
 *
 * 页面里通过 LocalAppContainer.current 拿到仓库，
 * 避免每个 Composable 都去 (context.applicationContext as App) 强转。
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer 未初始化，请检查 MainActivity 是否提供了 AppContainer")
}
