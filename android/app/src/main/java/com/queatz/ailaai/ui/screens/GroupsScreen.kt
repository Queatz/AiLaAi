package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.exploreGroups
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.GroupInfo
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.SearchResult
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.GroupExtended
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private var groupsCache = emptyList<GroupExtended>()

@Composable
fun GroupsScreen(
    geo: LatLng?,
) {
    val context = LocalContext.current
    val state = rememberLazyListState()
    var searchText by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var allGroups by remember {
        mutableStateOf(groupsCache)
    }
    var isLoading by rememberStateOf(allGroups.isEmpty())
    val me = me
    val reloadFlow = remember {
        MutableSharedFlow<Boolean>()
    }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    val results = remember(
        searchText,
        allGroups,
        selectedCategory
    ) {
        if (searchText.isBlank()) {
            allGroups
        } else {
            allGroups.filter {
                (it.group?.name?.contains(searchText, ignoreCase = true) == true) ||
                        it.members?.any {
                            it.person?.id != me?.id && it.person?.name?.contains(
                                searchText,
                                ignoreCase = true
                            ) == true
                        } == true
            }
        }
            .map { SearchResult.Group(it) }
            .let {
                if (selectedCategory == null) it else it.filter {
                    it.groupExtended.group?.categories?.contains(selectedCategory) == true
                }
            }
    }

    suspend fun reload(passive: Boolean = false) {
        if (geo != null) {
            isLoading = (!passive && groupsCache.isEmpty()) || results.isEmpty()
            api.exploreGroups(
                geo = geo.toGeo(),
                search = searchText,
                public = true,
                onError = {
                    if (!passive && it !is CancellationException) {
                        context.showDidntWork()
                    }
                }
            ) {
                allGroups = it.filter { it.group != null }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        reloadFlow.collectLatest {
            reload(it)
        }
    }

    ResumeEffect {
        reloadFlow.emit(true)
    }

    LaunchedEffect(searchText) {
        reloadFlow.emit(true)
    }

    var skipFirst by rememberStateOf(false)

    LaunchedEffect(geo) {
        if (geo == null) {
            return@LaunchedEffect
        }
        if (skipFirst) {
            skipFirst = false
            return@LaunchedEffect
        }
        // todo search server, set allGroups
        reloadFlow.emit(true)
    }

    LazyColumn(
        state = state,
        contentPadding = PaddingValues(
            1.pad
        ),
        verticalArrangement = Arrangement.spacedBy(1.pad),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize()
    ) {
        if (isLoading) {
            item {
                Loading()
            }
        } else if (results.isEmpty()) {
            item {
                EmptyText(stringResource(R.string.no_groups_nearby))
            }
        } else {
            items(
                items = results,
                key = {
                    "group:${it.groupExtended.group!!.id!!}"
                }
            ) {
                ContactItem(
                    item = it,
                    onChange = {
                        scope.launch {
                            reloadFlow.emit(true)
                        }
                    },
                    info = GroupInfo.Members,
                    coverPhoto = true
                )
            }
        }
    }
}