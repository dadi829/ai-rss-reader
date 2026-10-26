package com.example.airssreader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * 形状体系：整体偏方正。不使用 16dp 以上大圆角，避免「气泡感」。
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.RadiusXS),
    small = RoundedCornerShape(Dimens.RadiusSM),
    medium = RoundedCornerShape(Dimens.RadiusMD),
    large = RoundedCornerShape(Dimens.RadiusLG),
    extraLarge = RoundedCornerShape(Dimens.RadiusLG),
)
