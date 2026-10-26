package com.example.airssreader.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 配色说明：
 * 1. 主色为低饱和深青绿，只用于选中态、未读标记、主按钮，不做大面积铺色；
 * 2. 中性灰阶承担 90% 的界面（背景 / 分割线 / 三级文字），层级靠明度与字重区分；
 * 3. 暖棕作为辅助强调，红色仅用于错误与删除等危险操作；
 * 4. 深色模式沿用同一色相的暗部色阶，不使用荧光色。
 */
internal object Palette {
    // 主色（浅色）
    val Primary = Color(0xFF17695E)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFD5E8E3)
    val OnPrimaryContainer = Color(0xFF04302A)
    val InversePrimary = Color(0xFF8FD3C5)

    // 主色（深色）
    val PrimaryDark = Color(0xFF8FCFC2)
    val OnPrimaryDark = Color(0xFF00382F)
    val PrimaryContainerDark = Color(0xFF1E4E46)
    val OnPrimaryContainerDark = Color(0xFFC9E9E2)

    // 中性（浅色）
    val BackgroundLight = Color(0xFFF7F8F7)
    val SurfaceLight = Color(0xFFFFFFFF)
    val InkLight = Color(0xFF1A1C1B)
    val InkSecondaryLight = Color(0xFF5B625F)
    val InkTertiaryLight = Color(0xFF8D948F)
    val DividerLight = Color(0xFFE6E8E7)
    val DividerStrongLight = Color(0xFFD3D7D5)
    val MutedSurfaceLight = Color(0xFFF1F3F2)

    // 中性（深色）
    val BackgroundDark = Color(0xFF121413)
    val SurfaceDark = Color(0xFF181B1A)
    val InkDark = Color(0xFFE2E5E3)
    val InkSecondaryDark = Color(0xFFA7AEAB)
    val InkTertiaryDark = Color(0xFF767D7A)
    val DividerDark = Color(0xFF2A2F2D)
    val DividerStrongDark = Color(0xFF3A403E)
    val MutedSurfaceDark = Color(0xFF1C1F1E)

    // 语义色
    val ErrorLight = Color(0xFFB3261E)
    val ErrorDark = Color(0xFFF2B8B5)
    val SuccessLight = Color(0xFF2E7D5B)
    val SuccessDark = Color(0xFF7FC7A3)
    val WarningLight = Color(0xFF9A6B1F)
    val WarningDark = Color(0xFFD9B678)
    val AccentAmberLight = Color(0xFFB4763A)
    val AccentAmberDark = Color(0xFFD8AC7C)
}
