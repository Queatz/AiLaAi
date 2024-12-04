package com.queatz.ailaai.ui.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.ailaai.api.statusesOfPerson
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.fadingEdge
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.components.LoadMore
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person
import com.queatz.db.PersonStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun PersonStatusDialog(
    onDismissRequest: () -> Unit,
    person: Person,
    personStatus: PersonStatus?,
    onMessageClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showPhotoDialog by rememberStateOf<String?>(null)
    var statuses by rememberStateOf(personStatus.inList())
    var hasMore by rememberStateOf(true)
    val loadMore = remember {
        MutableSharedFlow<Unit>()
    }

    suspend fun loadNext() {
        api.statusesOfPerson(person.id!!, offset = statuses.size) {
            if (it.isEmpty()) {
                hasMore = false
            } else {
                statuses += it
            }
        }
    }

    LaunchedEffect(person) {
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

    DialogBase(onDismissRequest) {
        DialogLayout(
            scrollable = false,
            content = {
                val state = rememberLazyListState()
                var viewport by remember { mutableStateOf(Size(0f, 0f)) }
                val alpha by animateFloatAsState(if (state.firstVisibleItemIndex == 0) .5f else 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    GroupPhoto(
                        photos = person.contactPhoto().inList(),
                        modifier = Modifier.clickable {
                            onProfileClick()
                        }
                    )
                    Text(
                        text = person.name ?: stringResource(R.string.someone),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    LazyColumn(
                        state = state,
                        modifier = Modifier
                            .onPlaced { viewport = it.boundsInParent().size }
                            .fadingEdge(viewport, state, 6f)
                            .weight(1f, fill = false)
                    ) {
                        itemsIndexed(statuses) { index, status ->
                            PersonStatusItem(
                                status = status,
                                modifier = if (index == 0) {
                                    Modifier
                                } else {
                                    Modifier.alpha(alpha)
                                }
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
            },
            actions = {
                IconButton(onClick = onMessageClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Message,
                        contentDescription = stringResource(R.string.message),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onDismissRequest) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
