package com.example.airssreader.core.state

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 当前选中的源。
 *
 * 单独抽成进程内单例的原因：源管理页点某个源可以直接跳到资讯流，
 * 而资讯流的 ViewModel 归 FeedScreen 作用域，两页之间没有共同的父级状态。
 * 放在这里比把整个导航图提升成共享 ViewModel 更轻。
 *
 * 注意：它只保存一个 Long，不持有任何数据，进程结束即丢失，属于纯 UI 状态。
 */
object FeedSelection {
    val selectedFeedId = MutableStateFlow<Long?>(null)
}
