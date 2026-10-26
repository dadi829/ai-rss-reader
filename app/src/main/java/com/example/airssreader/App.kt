package com.example.airssreader

import android.app.Application
import com.example.airssreader.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口。
 *
 * 只做两件事：持有依赖容器、在首次启动时把预置源写进数据库。
 * 这里没有 WorkManager 初始化、没有推送 SDK、没有任何常驻任务。
 */
class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // 一次性播种，跑完即结束；不属于「后台刷新」，不会发起网络请求
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.feedRepository.ensureSeeded()
        }
    }
}
