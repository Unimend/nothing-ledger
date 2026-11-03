package com.unimend.nothingledger.design

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The only source of animation timing. Components choose a semantic spec, never a raw duration. */
public object LedgerMotionTokens {
    public const val ImmediateMillis: Int = 90
    public const val FeedbackMillis: Int = 180
    public const val DotRippleMillis: Int = 420
    public const val NavigationMillis: Int = 280
}

/** Reusable animation specs. Adding motion does not belong in feature screens. */
public object LedgerMotionSpecs {
    public fun navigation(): FiniteAnimationSpec<Float> = tween(
        durationMillis = LedgerMotionTokens.NavigationMillis,
        easing = FastOutSlowInEasing,
    )

    public fun feedback(): FiniteAnimationSpec<Float> = tween(
        durationMillis = LedgerMotionTokens.FeedbackMillis,
        easing = FastOutSlowInEasing,
    )

    public fun dotRipple(): FiniteAnimationSpec<Float> = tween(
        durationMillis = LedgerMotionTokens.DotRippleMillis,
        easing = FastOutSlowInEasing,
    )
}

/** The only source of gesture thresholds shared by feature and product surfaces. */
public object LedgerGestureTokens {
    public val LocalSwipeActionDistance: Dp = 64.dp
    public const val HorizontalDominanceRatio: Float = 1.35f
}

public object LedgerRippleTokens {
    public val DotSize: Dp = 3.dp
    public val StartRadius: Dp = 8.dp
    public val EndRadius: Dp = 88.dp
    public const val DotCount: Int = 24
}

/** Platform-aware local swipe configuration used by month and date surfaces. */
@Composable
public fun rememberLedgerLocalSwipeConfig(): HorizontalSwipeConfig {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val actionDistance = with(LocalDensity.current) { LedgerGestureTokens.LocalSwipeActionDistance.toPx() }
    return HorizontalSwipeConfig(
        directionLockDistance = touchSlop,
        actionDistance = actionDistance,
        horizontalDominanceRatio = LedgerGestureTokens.HorizontalDominanceRatio,
    )
}
