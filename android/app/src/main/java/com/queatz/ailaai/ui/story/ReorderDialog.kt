package com.queatz.ailaai.ui.story

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun <T> ReorderDialog(
    onDismissRequest: () -> Unit,
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    list: Boolean = false,
    draggable: (T) -> Boolean = { true },
    item: @Composable (item: T, elevation: Dp) -> Unit,
) {
    ReorderDialogBase(
        onDismissRequest
    ) {
        if (list) {
            val lazyListState = rememberLazyListState()
            val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
                if (from.index in items.indices && to.index in items.indices) {
                    if (draggable(items[from.index]) && draggable(items[to.index])) {
                        onMove(from.index, to.index)
                    }
                }
            }
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(2.pad),
                verticalArrangement = Arrangement.spacedBy(2.pad),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = key) {
                    if (draggable(it)) {
                        ReorderableItem(reorderState, key = key(it)) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 2.elevation else 0.dp)
                            Box(modifier = Modifier.longPressDraggableHandle()) {
                                item(it, elevation)
                            }
                        }
                    } else {
                        item(it, 0.dp)
                    }
                }
            }
        } else {
            val lazyGridState = rememberLazyGridState()
            val reorderState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
                if (from.index in items.indices && to.index in items.indices) {
                    if (draggable(items[from.index]) && draggable(items[to.index])) {
                        onMove(from.index, to.index)
                    }
                }
            }
            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Adaptive(80.dp),
                contentPadding = PaddingValues(2.pad),
                horizontalArrangement = Arrangement.spacedBy(2.pad),
                verticalArrangement = Arrangement.spacedBy(2.pad),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = key) {
                    if (draggable(it)) {
                        ReorderableItem(reorderState, key(it)) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 2.elevation else 0.dp)
                            Box(modifier = Modifier.longPressDraggableHandle()) {
                                item(it, elevation)
                            }
                        }
                    } else {
                        item(it, 0.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderDialogBase(
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    DialogBase(onDismissRequest) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
            ) {
                content()
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.pad)
            ) {
                DialogCloseButton(onDismissRequest)
            }
        }
    }
}
