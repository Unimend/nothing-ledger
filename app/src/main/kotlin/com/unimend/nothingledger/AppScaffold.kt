package com.unimend.nothingledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unimend.nothingledger.design.LedgerControl
import com.unimend.nothingledger.design.LedgerMotionSpecs
import com.unimend.nothingledger.design.LedgerScreen
import com.unimend.nothingledger.design.NothingLedgerTheme
import com.unimend.nothingledger.shell.MainDestination
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun AppScaffold(
    selected: MainDestination,
    onSelect: (MainDestination) -> Unit,
    onAdd: () -> Unit,
    content: @Composable ColumnScope.(MainDestination) -> Unit,
) {
    val selectedPage = mainDestinations.indexOf(selected).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedPage) { mainDestinations.size }
    val currentOnSelect by rememberUpdatedState(onSelect)

    LaunchedEffect(selected) {
        val targetPage = mainDestinations.indexOf(selected).coerceAtLeast(0)
        if (pagerState.settledPage != targetPage) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = LedgerMotionSpecs.navigation(),
            )
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                currentOnSelect(mainDestinations[page])
            }
    }

    Box(Modifier.fillMaxSize().background(NothingLedgerTheme.colors.background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> mainDestinations[page] },
        ) { page ->
            val destination = mainDestinations[page]
            LedgerScreen {
                content(destination)
                Spacer(Modifier.height(104.dp))
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(NothingLedgerTheme.colors.background)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            mainItems.forEach { (destination, label) ->
                LedgerControl(
                    label = label,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    accent = selected == destination,
                )
            }
        }
        LedgerControl(
            label = "+",
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 70.dp)
                .height(56.dp),
            accent = true,
            accessibilityLabel = "新增记账",
        )
    }
}

private val mainItems = listOf(
    MainDestination.TIMELINE to "流水",
    MainDestination.CALENDAR to "日历",
    MainDestination.STATISTICS to "统计",
    MainDestination.ACCOUNTS to "账户",
)

private val mainDestinations = mainItems.map { it.first }
