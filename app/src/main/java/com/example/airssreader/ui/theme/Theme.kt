package com.example.airssreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material3 ColorScheme 之外的语义色（分割线、弱化背景、三级文字等）。
 * 页面里统一通过 MaterialTheme.appColors 取用，避免散落硬编码。
 */
@Immutable
data class AppExtraColors(
    val divider: Color,
    val dividerStrong: Color,
    val surfaceMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val unreadDot: Color,
    val success: Color,
    val warning: Color,
    val accentAmber: Color,
    val searchHighlight: Color,
)

private val LightExtraColors = AppExtraColors(
    divider = Palette.DividerLight,
    dividerStrong = Palette.DividerStrongLight,
    surfaceMuted = Palette.MutedSurfaceLight,
    textPrimary = Palette.InkLight,
    textSecondary = Palette.InkSecondaryLight,
    textTertiary = Palette.InkTertiaryLight,
    unreadDot = Palette.Primary,
    success = Palette.SuccessLight,
    warning = Palette.WarningLight,
    accentAmber = Palette.AccentAmberLight,
    searchHighlight = Palette.PrimaryContainer,
)

private val DarkExtraColors = AppExtraColors(
    divider = Palette.DividerDark,
    dividerStrong = Palette.DividerStrongDark,
    surfaceMuted = Palette.MutedSurfaceDark,
    textPrimary = Palette.InkDark,
    textSecondary = Palette.InkSecondaryDark,
    textTertiary = Palette.InkTertiaryDark,
    unreadDot = Palette.PrimaryDark,
    success = Palette.SuccessDark,
    warning = Palette.WarningDark,
    accentAmber = Palette.AccentAmberDark,
    searchHighlight = Palette.PrimaryContainerDark,
)

private val LocalAppColors = staticCompositionLocalOf { LightExtraColors }

/** 扩展属性：MaterialTheme.appColors.divider 等 */
val MaterialTheme.appColors: AppExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

private val LightColors = lightColorScheme(
    primary = Palette.Primary,
    onPrimary = Palette.OnPrimary,
    primaryContainer = Palette.PrimaryContainer,
    onPrimaryContainer = Palette.OnPrimaryContainer,
    inversePrimary = Palette.InversePrimary,
    secondary = Palette.AccentAmberLight,
    onSecondary = Palette.OnPrimary,
    background = Palette.BackgroundLight,
    onBackground = Palette.InkLight,
    surface = Palette.SurfaceLight,
    onSurface = Palette.InkLight,
    surfaceVariant = Palette.MutedSurfaceLight,
    onSurfaceVariant = Palette.InkSecondaryLight,
    outline = Palette.DividerStrongLight,
    outlineVariant = Palette.DividerLight,
    error = Palette.ErrorLight,
    onError = Palette.OnPrimary,
)

private val DarkColors = darkColorScheme(
    primary = Palette.PrimaryDark,
    onPrimary = Palette.OnPrimaryDark,
    primaryContainer = Palette.PrimaryContainerDark,
    onPrimaryContainer = Palette.OnPrimaryContainerDark,
    inversePrimary = Palette.Primary,
    secondary = Palette.AccentAmberDark,
    onSecondary = Palette.OnPrimaryDark,
    background = Palette.BackgroundDark,
    onBackground = Palette.InkDark,
    surface = Palette.SurfaceDark,
    onSurface = Palette.InkDark,
    surfaceVariant = Palette.MutedSurfaceDark,
    onSurfaceVariant = Palette.InkSecondaryDark,
    outline = Palette.DividerStrongDark,
    outlineVariant = Palette.DividerDark,
    error = Palette.ErrorDark,
    onError = Palette.OnPrimaryDark,
)

@Composable
fun AiRssTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extra = if (darkTheme) DarkExtraColors else LightExtraColors
    CompositionLocalProvider(LocalAppColors provides extra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
