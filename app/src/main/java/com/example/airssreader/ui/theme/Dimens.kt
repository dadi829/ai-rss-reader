package com.example.airssreader.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 全局尺寸常量。页面内不出现裸数值，一律引用此处。
 *
 * 间距遵循 4dp 栅格；圆角克制：条目 4dp / 卡片 8dp / 大容器 12dp 封顶。
 */
object Dimens {
    // 间距
    val SpaceXXS = 2.dp
    val SpaceXS = 4.dp
    val SpaceSM = 8.dp
    val SpaceMD = 12.dp
    val SpaceLG = 16.dp
    val SpaceXL = 20.dp
    val SpaceXXL = 24.dp
    val SpaceXXXL = 32.dp

    // 页面级
    val PageHorizontal = 16.dp
    val PageTop = 8.dp
    val SectionGap = 20.dp

    // 圆角
    val RadiusXS = 2.dp
    val RadiusSM = 4.dp
    val RadiusMD = 8.dp
    val RadiusLG = 12.dp

    // 线宽
    val Hairline = 0.5.dp
    val StrokeThin = 1.dp
    val StrokeThick = 2.dp

    // 控件尺寸
    val TopBarHeight = 52.dp
    val ButtonHeight = 44.dp
    val TouchTarget = 48.dp
    val SearchBarHeight = 40.dp
    val SourceTabHeight = 36.dp
    val IconSM = 16.dp
    val IconMD = 20.dp
    val IconLG = 24.dp
    val UnreadDot = 6.dp
    val TabIndicatorWidth = 20.dp
    val ListItemMinHeight = 56.dp

    // 内容约束
    val ContentMaxWidth = 280.dp
    val SummaryMaxLines = 3
    val TitleMaxLines = 3
}
