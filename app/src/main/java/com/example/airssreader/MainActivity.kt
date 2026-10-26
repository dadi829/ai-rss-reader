package com.example.airssreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.example.airssreader.ui.common.LocalAppContainer
import com.example.airssreader.ui.navigation.AppNavHost
import com.example.airssreader.ui.theme.AiRssTheme

/**
 * 唯一的 Activity。
 *
 * 单 Activity + Navigation Compose：页面切换不涉及 Activity 生命周期，
 * 状态统一由 ViewModel 持有，返回栈交给 Navigation 管理。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as App).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                AiRssTheme {
                    AppNavHost()
                }
            }
        }
    }
}
