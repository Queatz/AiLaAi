package com.queatz.ailaai.ui.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import app.ailaai.api.deleteStatus
import app.ailaai.api.statusesOfPerson
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.fadingEdge
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.LoadMore
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.PersonStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Statuses(
    personId: String,
    modifier: Modifier = Modifier,
    initialValue: List<PersonStatus> = emptyList(),
    onStatus: (PersonStatus) -> Unit
) {
    val me = me
    val scope = rememberCoroutineScope()
    var showPhotoDialog by rememberStateOf<String?>(null)
    var statuses by rememberStateOf(initialValue)
    var hasMore by rememberStateOf(true)
    val loadMore = remember {
        MutableSharedFlow<Unit>()
    }
    val state = rememberLazyListState()
    var viewport by remember { mutableStateOf(Size(0f, 0f)) }
    val alpha by animateFloatAsState(if (state.firstVisibleItemIndex == 0) .5f else 1f)

    suspend fun loadNext() {
        api.statusesOfPerson(personId, offset = statuses.size) {
            if (it.isEmpty()) {
                hasMore = false
            } else {
                statuses += it
            }
        }
    }

    LaunchedEffect(personId) {
        loadNext()
        loadMore.collect {
            loadNext()
        }
    }

    if (showPhotoDialog != null) {
        val media = Media.Photo(showPhotoDialog!!)
        PhotoDialog(
            onDismissRequest = {
                showPhotoDialog = null
            },
            initialMedia = media,
            medias = media.inList()
        )
    }

    LazyColumn(
        state = state,
        modifier = modifier
            .onPlaced { viewport = it.boundsInParent().size }
            .fadingEdge(viewport, state, 6f)
    ) {
        itemsIndexed(statuses) { index, status ->
            var showMyStatusMenu by rememberStateOf(false)
            var showDeleteDialog by rememberStateOf(false)

            if (showDeleteDialog) {
                Alert(
                    onDismissRequest = {
                        showDeleteDialog = false
                    },
                    title = stringResource(R.string.delete_this_status),
                    text = null,
                    dismissButton = stringResource(R.string.cancel),
                    confirmButton = stringResource(R.string.yes_delete),
                    confirmColor = MaterialTheme.colorScheme.error
                ) {
                    scope.launch {
                        api.deleteStatus(status.id!!) {
                            statuses = emptyList()
                            loadMore.emit(Unit)
                        }
                    }
                }
            }

            if (showMyStatusMenu) {
                Menu(
                    {
                        showMyStatusMenu = false
                    }
                ) {
                    menuItem(stringResource(R.string.delete)) {
                        showDeleteDialog = true
                        showMyStatusMenu = false
                    }
                }
            }

            PersonStatusItem(
                status = status,
                modifier = if (index == 0) {
                    Modifier
                } else {
                    Modifier.alpha(alpha)
                }
                    .clip(MaterialTheme.shapes.large)
                    .combinedClickable(
                        onLongClick = {
                            if (personId == me?.id) {
                                showMyStatusMenu = true
                            }
                        },
                        onClick = {
                            onStatus(status)
                        }
                    )
            ) {
                showPhotoDialog = status.photo!!
            }
            if (index != statuses.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.pad).alpha(alpha)
                )
            }
        }

        item {
            LoadMore(hasMore = hasMore, contentPadding = 1.pad) {
                scope.launch {
                    loadMore.emit(Unit)
                }
            }
        }
    }
}
