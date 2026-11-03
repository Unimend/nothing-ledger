package com.unimend.nothingledger.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
public fun LedgerSurface(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = NothingLedgerTheme.colors
    val shape = RoundedCornerShape(LedgerRadius.surface)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (elevated) colors.surfaceRaised else colors.surface)
            .border(1.dp, colors.border, shape),
        content = content,
    )
}

@Composable
public fun LedgerAmountText(
    amount: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    textAlign: TextAlign = TextAlign.End,
) {
    Text(
        text = amount,
        modifier = modifier,
        color = if (accent) NothingLedgerTheme.colors.accent else NothingLedgerTheme.colors.contentPrimary,
        style = MaterialTheme.typography.displayMedium,
        textAlign = textAlign,
        maxLines = 1,
    )
}

public enum class LedgerStatus { ACTIVE, MUTED, ALERT }

@Composable
public fun LedgerStatusMarker(
    label: String,
    status: LedgerStatus,
    modifier: Modifier = Modifier,
) {
    val colors = NothingLedgerTheme.colors
    val markerColor = when (status) {
        LedgerStatus.ACTIVE -> colors.contentPrimary
        LedgerStatus.MUTED -> colors.contentMuted
        LedgerStatus.ALERT -> colors.accent
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(8.dp)) { drawRect(markerColor) }
        Spacer(Modifier.size(LedgerSpacing.xs))
        Text(label, color = markerColor, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
public fun DitherField(
    modifier: Modifier = Modifier,
    cellSize: Dp = 6.dp,
    color: Color = NothingLedgerTheme.colors.contentMuted,
    density: Int = 3,
) {
    require(density > 0) { "Dither density must be positive" }
    Canvas(modifier) {
        val cellPx = cellSize.toPx()
        val columns = (size.width / cellPx).toInt() + 1
        val rows = (size.height / cellPx).toInt() + 1
        repeat(columns) { x ->
            repeat(rows) { y ->
                if ((x * 2 + y * 3) % density == 0) {
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(x * cellPx, y * cellPx),
                        size = androidx.compose.ui.geometry.Size(cellPx * 0.62f, cellPx * 0.62f),
                    )
                }
            }
        }
    }
}

