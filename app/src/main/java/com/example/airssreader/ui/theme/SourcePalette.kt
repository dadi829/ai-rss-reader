package com.example.airssreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * 源标识色。
 *
 * 目的：聚合时间线里所有源混在一起，只靠文字区分来源太吃力。
 * 给每个源分配一个稳定的颜色，卡片元信息行和详情页的源名前面各放一个小圆点，
 * 用户扫一眼就能把「这一条是谁发的」对应起来。
 *
 * 设计约束：
 * 1. 颜色由 feedId 取模得到，不落库——省一个字段，也不会因为改配色要迁移数据；
 * 2. 浅色与深色各一套：浅色用低明度保证白底可读，深色提高明度保证暗底可见；
 * 3. 只用中低饱和度，不抢主色（深青绿）的视觉权重，也不至于像标签一样花。
 */
object SourcePalette {

    /** 浅色模式：明度压到 40% 左右，配白色卡片有足够对比 */
    private val lightColors = listOf(
        Color(0xFF2F7A6E), // 青绿
        Color(0xFF9A6B3F), // 赭石
        Color(0xFF3F6690), // 靛蓝
        Color(0xFF8C5A5A), // 陶红
        Color(0xFF5F7548), // 橄榄
        Color(0xFF6E5B84), // 紫灰
        Color(0xFF3D7A85), // 蓝绿
        Color(0xFF8A7534), // 芥黄
    )

    /** 深色模式：同色相提亮，避免在 #121413 背景上糊成一团 */
    private val darkColors = listOf(
        Color(0xFF7FC4B6),
        Color(0xFFD8AC7C),
        Color(0xFF8FB3D6),
        Color(0xFFCE9292),
        Color(0xFFA8BE8A),
        Color(0xFFB4A2C8),
        Color(0xFF88C0CA),
        Color(0xFFD2BE7E),
    )

    /**
     * 按源 id 取色。
     *
     * 用取模而不是哈希：预置源的 id 是连续自增的，取模能让相邻的源拿到
     * 相邻的色位，8 个源刚好把 8 个颜色用满、互不重复。
     */
    fun colorFor(feedId: Long, darkTheme: Boolean): Color {
        val palette = if (darkTheme) darkColors else lightColors
        val index = ((feedId % palette.size) + palette.size) % palette.size
        return palette[index.toInt()]
    }
}

/** 组合函数里直接用：`sourceColorFor(article.feedId)` */
@Composable
@ReadOnlyComposable
fun sourceColorFor(feedId: Long): Color =
    SourcePalette.colorFor(feedId = feedId, darkTheme = isSystemInDarkTheme())
