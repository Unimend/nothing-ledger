package com.unimend.nothingledger.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
public fun Modifier.ledgerClickable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        role = role,
        onClick = onClick,
    )
}

@Composable
public fun LedgerScreen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NothingLedgerTheme.colors.background)
            .padding(WindowInsets.safeDrawing.asPaddingValues()),
    ) {
        val horizontalPadding = if (maxWidth < 360.dp) {
            LedgerSize.compactScreenPadding
        } else {
            LedgerSize.regularScreenPadding
        }
        val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(
            modifier = scrollModifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(bottom = LedgerSize.minimumContentBottomSpace),
            content = content,
        )
    }
}

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
    color: Color? = null,
) {
    val style = when {
        amount.length <= 9 -> LedgerTextStyles.amountLarge
        amount.length <= 13 -> LedgerTextStyles.amountMedium
        else -> LedgerTextStyles.amountCompact
    }
    Text(
        text = amount,
        modifier = modifier,
        color = color ?: if (accent) NothingLedgerTheme.colors.accent else NothingLedgerTheme.colors.contentPrimary,
        style = style,
        textAlign = textAlign,
        maxLines = 1,
    )
}

@Composable
public fun LedgerControl(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
    accessibilityLabel: String = label,
    tone: LedgerControlTone? = null,
) {
    val colors = NothingLedgerTheme.colors
    val shape = RoundedCornerShape(LedgerRadius.control)
    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = LedgerSize.minimumTouchTarget,
                minHeight = LedgerSize.minimumTouchTarget,
            )
            .clip(shape)
            .background(
                when {
                    !enabled -> colors.surface
                    tone != null -> tone.color(colors)
                    accent -> colors.accent
                    else -> colors.surfaceRaised
                },
            )
            .semantics { contentDescription = accessibilityLabel }
            .ledgerClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) colors.contentPrimary else colors.contentMuted,
            style = LedgerTextStyles.latinLabel,
            textAlign = TextAlign.Center,
        )
    }
}

public enum class LedgerControlTone {
    ACCENT,
    INCOME,
    EXPENSE,
    ASSET,
    TRANSFER,
    WARNING,
    CATEGORY_A,
    CATEGORY_B,
    CATEGORY_C,
}

private fun LedgerControlTone.color(colors: LedgerColors): Color = when (this) {
    LedgerControlTone.ACCENT -> colors.accent
    LedgerControlTone.INCOME -> colors.income
    LedgerControlTone.EXPENSE -> colors.expense
    LedgerControlTone.ASSET -> colors.asset
    LedgerControlTone.TRANSFER -> colors.transfer
    LedgerControlTone.WARNING -> colors.warning
    LedgerControlTone.CATEGORY_A -> colors.categoryA
    LedgerControlTone.CATEGORY_B -> colors.categoryB
    LedgerControlTone.CATEGORY_C -> colors.categoryC
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

