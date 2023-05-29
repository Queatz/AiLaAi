package com.queatz.ailaai.ui.story

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import org.burnoutcrew.reorderable.*

@Composable
fun <T> ReorderDialog(
    onDismissRequest: () -> Unit,
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: ItemPosition, to: ItemPosition) -> Unit,
    list: Boolean = false,
    draggable: (T) -> Boolean = { true },
    item: @Composable (item: T, elevation: Dp) -> Unit,
) {
    ReorderDialogBase(
        onDismissRequest
    ) {
        if (list) {
            val reorderState = rememberReorderableLazyListState(
                onMove = onMove,
                canDragOver = { draggedOver, dragging -> draggable(items[draggedOver.index]) }
            )
            LazyColumn(
                state = reorderState.listState,
                contentPadding = PaddingValues(PaddingDefault * 2),
                verticalArrangement = Arrangement.spacedBy(PaddingDefault * 2),
                modifier = Modifier
                    .reorderable(reorderState)
                    .detectReorder(reorderState)
                    .detectReorderAfterLongPress(reorderState)
            ) {
                items(items, key = key) {
                    if (draggable(it)) {
                        ReorderableItem(reorderState, key = key(it)) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) ElevationDefault * 2 else 0.dp)
                            item(it, elevation)
                        }
                    } else {
                        item(it, 0.dp)
                    }
                }
            }
        } else {
            val reorderState = rememberReorderableLazyGridState(onMove = onMove)
            LazyVerticalGrid(
                state = reorderState.gridState,
                columns = GridCells.Adaptive(80.dp),
                contentPadding = PaddingValues(PaddingDefault * 2),
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault * 2),
                verticalArrangement = Arrangement.spacedBy(PaddingDefault * 2),
                modifier = Modifier
                    .reorderable(reorderState)
                    .detectReorder(reorderState)
                    .detectReorderAfterLongPress(reorderState)
            ) {
                items(items, key = key) {
                    if (draggable(it)) {
                        ReorderableItem(reorderState, key(it)) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) ElevationDefault * 2 else 0.dp)
                            item(it, elevation)
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
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingDefault * 2)
            ) {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}
