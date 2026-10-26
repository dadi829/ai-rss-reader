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

    /**
     * 「全部」聚合视图的虚拟 id。
     *
     * 用 -1 而不是 null：null 在原来的实现里表示「还没有选中任何源」，
     * 与「用户主动选了全部」是两种不同语义，混用会让空态判断变得含糊。
     * 真实源 id 由 Room 自增，从 1 开始，不会与 -1 冲突。
     */
    const val ALL_FEED_ID = -1L

    /** 默认停在聚合视图：一次看到所有源的最新内容，比默认进某个单源更有用 */
    val selectedFeedId = MutableStateFlow(ALL_FEED_ID)
}
