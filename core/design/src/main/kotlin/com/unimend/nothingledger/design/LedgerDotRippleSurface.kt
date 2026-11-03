package com.unimend.nothingledger.design

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Shared Nothing-style surface whose only visual feedback is an expanding ring of square dots. */
@Composable
public fun LedgerDotRippleSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rippleColor: Color = NothingLedgerTheme.colors.accent,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = NothingLedgerTheme.colors
    val shape = RoundedCornerShape(LedgerRadius.surface)
    val progress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var origin by remember { mutableStateOf(Offset.Zero) }
    var generation by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .semantics {
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            }
            .pointerInput(onClick) {
                detectTapGestures { position ->
                    origin = position
                    generation += 1
                    scope.launch {
                        progress.stop()
                        progress.snapTo(0f)
                        onClick()
                        progress.animateTo(1f, LedgerMotionSpecs.dotRipple())
                    }
                }
            },
    ) {
        content()
        val animationProgress = progress.value
        if (animationProgress < 1f) {
            Canvas(Modifier.matchParentSize()) {
                val dotSize = LedgerRippleTokens.DotSize.toPx()
                val start = LedgerRippleTokens.StartRadius.toPx()
                val end = LedgerRippleTokens.EndRadius.toPx()
                val radius = start + (end - start) * animationProgress
                val fade = sin(PI * animationProgress).toFloat().coerceIn(0f, 1f)
                repeat(LedgerRippleTokens.DotCount) { index ->
                    val phase = (generation % 4) * 0.09f
                    val angle = (2.0 * PI * index / LedgerRippleTokens.DotCount) + phase
                    val stagger = 0.88f + (index % 3) * 0.06f
                    val center = Offset(
                        x = origin.x + cos(angle).toFloat() * radius * stagger,
                        y = origin.y + sin(angle).toFloat() * radius * stagger,
                    )
                    drawRect(
                        color = rippleColor.copy(alpha = 0.78f * fade),
                        topLeft = center - Offset(dotSize / 2f, dotSize / 2f),
                        size = Size(dotSize, dotSize),
                    )
                }
            }
        }
    }
}
