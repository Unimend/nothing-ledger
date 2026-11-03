package com.unimend.nothingledger.design

import kotlin.math.abs

/** The action produced when a local horizontal swipe finishes. */
public enum class HorizontalSwipeAction {
    Previous,
    Next,
    None,
}

/** The axis owned by a gesture after its initial movement becomes unambiguous. */
public enum class GestureAxisLock {
    Undecided,
    Horizontal,
    Vertical,
}

/** Shared thresholds for local month and date swipes. Values are supplied in pixels. */
public data class HorizontalSwipeConfig(
    public val directionLockDistance: Float,
    public val actionDistance: Float,
    public val horizontalDominanceRatio: Float = 1.35f,
) {
    init {
        require(directionLockDistance > 0f)
        require(actionDistance >= directionLockDistance)
        require(horizontalDominanceRatio > 1f)
    }
}

/**
 * Pure gesture state used by Compose surfaces without embedding navigation policy in the UI.
 *
 * Once horizontal or vertical movement wins, the lock never changes for the lifetime of the
 * gesture. This prevents a vertical scroll with a curved finger path from becoming a month swipe.
 */
public data class HorizontalSwipeState(
    public val totalX: Float = 0f,
    public val totalY: Float = 0f,
    public val axisLock: GestureAxisLock = GestureAxisLock.Undecided,
) {
    public fun move(deltaX: Float, deltaY: Float, config: HorizontalSwipeConfig): HorizontalSwipeState {
        val nextX = totalX + deltaX
        val nextY = totalY + deltaY
        if (axisLock != GestureAxisLock.Undecided) {
            return copy(totalX = nextX, totalY = nextY)
        }

        val absoluteX = abs(nextX)
        val absoluteY = abs(nextY)
        val travelled = maxOf(absoluteX, absoluteY)
        if (travelled < config.directionLockDistance) {
            return copy(totalX = nextX, totalY = nextY)
        }

        val nextLock = when {
            absoluteX > absoluteY * config.horizontalDominanceRatio -> GestureAxisLock.Horizontal
            absoluteY > absoluteX * config.horizontalDominanceRatio -> GestureAxisLock.Vertical
            else -> GestureAxisLock.Undecided
        }
        return copy(totalX = nextX, totalY = nextY, axisLock = nextLock)
    }

    public fun finish(config: HorizontalSwipeConfig): HorizontalSwipeAction {
        if (axisLock != GestureAxisLock.Horizontal || abs(totalX) < config.actionDistance) {
            return HorizontalSwipeAction.None
        }
        return if (totalX > 0f) HorizontalSwipeAction.Previous else HorizontalSwipeAction.Next
    }
}
