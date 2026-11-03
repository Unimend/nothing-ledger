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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
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

internal data class LedgerDotRippleRequest(val id: Int, val origin: Offset, val colorIndex: Int)

public class LedgerFeedbackController internal constructor() {
    private var generation: Int = 0
    internal val dotRippleRequests = mutableStateListOf<LedgerDotRippleRequest>()

    public fun showDotRipple(origin: Offset) {
        val id = ++generation
        dotRippleRequests += LedgerDotRippleRequest(id = id, origin = origin, colorIndex = id - 1)
        if (dotRippleRequests.size > MAX_SIMULTANEOUS_RIPPLES) dotRippleRequests.removeFirst()
    }

    internal fun finishDotRipple(id: Int) {
        dotRippleRequests.removeAll { it.id == id }
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
    val colors = NothingLedgerTheme.colors
    val palette = remember(colors) {
        listOf(colors.accent, colors.income, colors.asset, colors.warning, colors.transfer, colors.categoryC)
    }

    CompositionLocalProvider(LocalLedgerFeedbackController provides controller) {
        Box(modifier.fillMaxSize()) {
            content()
            controller.dotRippleRequests.forEach { request ->
                key(request.id) {
                    LedgerDotRippleAnimation(
                        request = request,
                        color = palette[request.colorIndex % palette.size],
                        onFinished = { controller.finishDotRipple(request.id) },
                    )
                }
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
private fun LedgerDotRippleAnimation(
    request: LedgerDotRippleRequest,
    color: Color,
    onFinished: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, LedgerMotionSpecs.dotRipple())
        onFinished()
    }
    Canvas(Modifier.fillMaxSize()) {
        val dotSize = LedgerRippleTokens.DotSize.toPx()
        val start = LedgerRippleTokens.StartRadius.toPx()
        val end = LedgerRippleTokens.EndRadius.toPx()
        val radius = start + (end - start) * progress.value
        val fade = sin(PI * progress.value).toFloat().coerceIn(0f, 1f)
        repeat(LedgerRippleTokens.DotCount) { index ->
            val phase = (request.id % 4) * 0.09f
            val angle = (2.0 * PI * index / LedgerRippleTokens.DotCount) + phase
            val stagger = 0.88f + (index % 3) * 0.06f
            val center = Offset(
                x = request.origin.x + cos(angle).toFloat() * radius * stagger,
                y = request.origin.y + sin(angle).toFloat() * radius * stagger,
            )
            drawRect(
                color = color.copy(alpha = 0.78f * fade),
                topLeft = center - Offset(dotSize / 2f, dotSize / 2f),
                size = Size(dotSize, dotSize),
            )
        }
    }
}

private const val MAX_SIMULTANEOUS_RIPPLES: Int = 16
