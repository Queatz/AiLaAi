package com.queatz.ailaai.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlin.math.abs

enum class MainTab {
    Friends,
    Local,
    Saved
}

private val MainTab.stringResource
    get() = when (this) {
        MainTab.Friends -> R.string.friends
        MainTab.Local -> R.string.local
        MainTab.Saved -> R.string.saved
    }

fun Modifier.swipeMainTabs(onSwipe: (Int) -> Unit): Modifier = composed {
    val slop = LocalViewConfiguration.current.touchSlop
    this.draggable(
        state = rememberDraggableState {  },
        orientation = Orientation.Horizontal,
        onDragStopped = {
            if (abs(it) > slop) {
                onSwipe(if (it < 0.0) 1 else -1)
            }
        }
    )
}

@Composable
fun MainTabs(tab: MainTab, onTab: (MainTab) -> Unit, tabs: List<MainTab>? = null) {
    TabRow(
        selectedTabIndex = tab.ordinal,
        divider = {},
        indicator = {
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(it[tab.ordinal]).clip(MaterialTheme.shapes.small)
            )
        },
        modifier = Modifier
            .padding(start = PaddingDefault, end = PaddingDefault, bottom = PaddingDefault / 2)
    ) {
        (tabs?.sortedBy { it.ordinal } ?: MainTab.entries).forEachIndexed { index, it ->
            Tab(
                tab.ordinal == index,
                onClick = { onTab(it) },
                selectedContentColor = MaterialTheme.colorScheme.onBackground,
                unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = .5f
                ),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
            ) {
                Text(
                    stringResource(it.stringResource),
                    fontWeight = if (tab.ordinal == index) FontWeight.Black else FontWeight.Normal,
                    modifier = Modifier
                        .padding(PaddingDefault)
                )
            }
        }
    }
}
