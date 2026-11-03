package com.unimend.nothingledger.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.unimend.nothingledger.design.R

public val LedgerDotoFamily: FontFamily = FontFamily(
    Font(R.font.doto),
)

private val SystemFamily: FontFamily = FontFamily.SansSerif

private val LedgerTypography: Typography = Typography(
    displayLarge = TextStyle(fontFamily = LedgerDotoFamily, fontWeight = FontWeight.Bold, fontSize = 56.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = LedgerDotoFamily, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontFamily = SystemFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = SystemFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = SystemFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = SystemFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = SystemFamily, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = SystemFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    labelLarge = TextStyle(fontFamily = LedgerDotoFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = LedgerDotoFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
)

public object LedgerTextStyles {
    public val amountLarge: TextStyle = TextStyle(
        fontFamily = LedgerDotoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 58.sp,
    )
    public val amountMedium: TextStyle = amountLarge.copy(fontSize = 44.sp, lineHeight = 46.sp)
    public val amountCompact: TextStyle = amountLarge.copy(fontSize = 32.sp, lineHeight = 36.sp)
    public val latinLabel: TextStyle = TextStyle(
        fontFamily = LedgerDotoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
}

private val LocalLedgerColors = staticCompositionLocalOf { LedgerColorTokens.Dark }

public object NothingLedgerTheme {
    public val colors: LedgerColors
        @Composable @ReadOnlyComposable get() = LocalLedgerColors.current
}

@Composable
public fun NothingLedgerTheme(
    content: @Composable () -> Unit,
) {
    val colors = LedgerColorTokens.Dark
    CompositionLocalProvider(LocalLedgerColors provides colors) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = colors.accent,
                onPrimary = colors.contentPrimary,
                background = colors.background,
                onBackground = colors.contentPrimary,
                surface = colors.surface,
                onSurface = colors.contentPrimary,
                outline = colors.border,
            ),
            typography = LedgerTypography,
            content = content,
        )
    }
}

