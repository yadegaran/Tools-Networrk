package com.tools.net.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.tools.net.ui.theme.BrandBlue
import com.tools.net.ui.theme.BrandCyan
import com.tools.net.ui.theme.BrandPurple
import com.tools.net.ui.theme.GlassBorderDark
import com.tools.net.ui.theme.GlassBorderLight
import com.tools.net.ui.theme.GlassDarkTint
import com.tools.net.ui.theme.GlassWhiteTint

/** آیا تم فعلی (بر اساس انتخاب واقعی کاربر، نه فقط سیستم) تاریک است. */
@Composable
private fun isCurrentThemeDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * پس‌زمینه گرادینت محو برای کل صفحه، که زیر کارت‌های شیشه‌ای حس عمق ایجاد می‌کند.
 */
@Composable
fun GlassAppBackground(content: @Composable () -> Unit) {
    val isDark = isCurrentThemeDark()
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(colors.background, Color(0xFF131A2A), colors.background)
                    } else {
                        listOf(Color(0xFFE8F0FE), colors.background, Color(0xFFEFF6FF))
                    }
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandBlue.copy(alpha = if (isDark) 0.10f else 0.08f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
        )
        content()
    }
}

/** کارت/سطح شیشه‌ای نیمه‌شفاف با حاشیه‌ی نوری ملایم؛ روی پس‌زمینه گرادینت اپ حس عمق می‌دهد. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isDark = isCurrentThemeDark()
    val tint = if (isDark) GlassDarkTint else GlassWhiteTint
    val border = if (isDark) GlassBorderDark else GlassBorderLight

    var glassModifier = modifier
        .clip(shape)
        .background(tint, shape)
        .border(1.dp, border, shape)

    if (onClick != null) {
        glassModifier = glassModifier.clickable(onClick = onClick)
    }

    Box(modifier = glassModifier) {
        content()
    }
}

/** برند گرادینت برای آیکون‌ها / هدرها (آبی -> فیروزه‌ای -> بنفش). */
val GlassBrandGradient = Brush.horizontalGradient(listOf(BrandBlue, BrandCyan, BrandPurple))
