package com.unimend.nothingledger.design

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class LedgerDotRippleRequest(val id: Int, val origin: Offset)

public class LedgerFeedbackController internal constructor() {
    private var generation: Int = 0
    internal var dotRippleRequest: LedgerDotRippleRequest? by mutableStateOf(null)
        private set

    public fun showDotRipple(origin: Offset) {
        dotRippleRequest = LedgerDotRippleRequest(id = ++generation, origin = origin)
    }
}

private val LocalLedgerFeedbackController = staticCompositionLocalOf<LedgerFeedbackController?> { null }

/** App-level feedback layer that survives page navigation. */
@Composable
public fun LedgerFeedbackHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val controller = remember { LedgerFeedbackController() }
    val progress = remember { Animatable(1f) }
    val request = controller.dotRippleRequest

    LaunchedEffect(request?.id) {
        if (request == null) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, LedgerMotionSpecs.dotRipple())
    }

    CompositionLocalProvider(LocalLedgerFeedbackController provides controller) {
        Box(modifier.fillMaxSize()) {
            content()
            if (request != null && progress.value < 1f) {
                LedgerDotRippleOverlay(
                    origin = request.origin,
                    generation = request.id,
                    progress = progress.value,
                    color = NothingLedgerTheme.colors.accent,
                )
            }
        }
    }
}

/** Rounded clickable surface that reports its touch point to [LedgerFeedbackHost]. */
@Composable
public fun LedgerDotRippleSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = NothingLedgerTheme.colors
    val shape = RoundedCornerShape(LedgerRadius.surface)
    val feedback = LocalLedgerFeedbackController.current
    var coordinates: LayoutCoordinates? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .onGloballyPositioned { coordinates = it }
            .semantics {
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            }
            .pointerInput(onClick, feedback) {
                detectTapGestures { localPosition ->
                    coordinates?.let { feedback?.showDotRipple(it.localToRoot(localPosition)) }
                    onClick()
                }
            },
        content = content,
    )
}

@Composable
private fun LedgerDotRippleOverlay(
    origin: Offset,
    generation: Int,
    progress: Float,
    color: Color,
) {
    Canvas(Modifier.fillMaxSize()) {
        val dotSize = LedgerRippleTokens.DotSize.toPx()
        val start = LedgerRippleTokens.StartRadius.toPx()
        val end = LedgerRippleTokens.EndRadius.toPx()
        val radius = start + (end - start) * progress
        val fade = sin(PI * progress).toFloat().coerceIn(0f, 1f)
        repeat(LedgerRippleTokens.DotCount) { index ->
            val phase = (generation % 4) * 0.09f
            val angle = (2.0 * PI * index / LedgerRippleTokens.DotCount) + phase
            val stagger = 0.88f + (index % 3) * 0.06f
            val center = Offset(
                x = origin.x + cos(angle).toFloat() * radius * stagger,
                y = origin.y + sin(angle).toFloat() * radius * stagger,
            )
            drawRect(
                color = color.copy(alpha = 0.78f * fade),
                topLeft = center - Offset(dotSize / 2f, dotSize / 2f),
                size = Size(dotSize, dotSize),
            )
        }
    }
}
