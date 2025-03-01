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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.exploreGroups
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.GroupInfo
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.SearchResult
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Geo
import com.queatz.db.GroupExtended
import kotlinx.coroutines.CancellationException
import kotlin.random.Random.Default.nextInt

private var groupsCache = emptyList<GroupExtended>()

@Composable
fun GroupsScreen(
    geo: Geo?
) {
    val context = LocalContext.current
    val state = rememberLazyListState()
    var searchText by rememberSaveable { mutableStateOf("") }
    var allGroups by remember {
        mutableStateOf(groupsCache)
    }
    var isLoading by rememberStateOf(allGroups.isEmpty())
    val me = me
    var reloadKey by remember {
        mutableStateOf(nextInt())
    }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadKey) {
        if (geo != null) {
            isLoading = (groupsCache.isEmpty()) || allGroups.isEmpty()
            api.exploreGroups(
                geo = geo,
                search = searchText,
                public = true,
                onError = {
                    if (it !is CancellationException) {
                        context.showDidntWork()
                    }
                }
            ) {
                allGroups = it.filter { it.group != null }
            }
            isLoading = false
        }
    }

    LaunchedEffect(geo, searchText) {
        reloadKey = nextInt()
    }

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
                if (selectedCategory == null) {
                    it
                } else {
                    it.filter {
                        it.groupExtended.group?.categories?.contains(selectedCategory) == true
                    }
                }
            }
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
        when {
            isLoading -> {
                item {
                    Loading()
                }
            }
            results.isEmpty() -> {
                item {
                    EmptyText(stringResource(R.string.no_groups_nearby))
                }
            }
            else -> {
                items(
                    items = results,
                    key = {
                        "group:${it.groupExtended.group!!.id!!}"
                    }
                ) {
                    ContactItem(
                        item = it,
                        onChange = {
                            reloadKey = nextInt()
                        },
                        info = GroupInfo.Members,
                        coverPhoto = true
                    )
                }
            }
        }
    }
}