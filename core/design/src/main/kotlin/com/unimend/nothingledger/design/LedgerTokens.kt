package com.unimend.nothingledger.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class LedgerColors(
    public val background: Color,
    public val surface: Color,
    public val surfaceRaised: Color,
    public val contentPrimary: Color,
    public val contentSecondary: Color,
    public val contentMuted: Color,
    public val border: Color,
    public val accent: Color,
    public val positive: Color,
)

public object LedgerColorTokens {
    public val Dark: LedgerColors = LedgerColors(
        background = Color(0xFF000000),
        surface = Color(0xFF0A0A0A),
        surfaceRaised = Color(0xFF141414),
        contentPrimary = Color(0xFFF5F5F5),
        contentSecondary = Color(0xFFB8B8B8),
        contentMuted = Color(0xFF656565),
        border = Color(0xFF303030),
        accent = Color(0xFFFF2D2D),
        positive = Color(0xFFF5F5F5),
    )
}

public object LedgerSpacing {
    public val xxs: Dp = 4.dp
    public val xs: Dp = 8.dp
    public val sm: Dp = 12.dp
    public val md: Dp = 16.dp
    public val lg: Dp = 24.dp
    public val xl: Dp = 32.dp
    public val xxl: Dp = 48.dp
}

public object LedgerSize {
    /** Exceeds the 44dp accessibility floor while matching Android guidance. */
    public val minimumTouchTarget: Dp = 48.dp
    public val compactScreenPadding: Dp = 16.dp
    public val regularScreenPadding: Dp = 24.dp
    public val minimumContentBottomSpace: Dp = 24.dp
}

public object LedgerRadius {
    public val control: Dp = 8.dp
    public val surface: Dp = 16.dp
    public val prominent: Dp = 24.dp
}

public object LedgerMotion {
    public const val InstantMillis: Int = 90
    public const val FeedbackMillis: Int = 180
    public const val TransitionMillis: Int = 280
}

