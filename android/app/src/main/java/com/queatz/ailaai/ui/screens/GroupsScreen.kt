package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.exploreGroups
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.inDp
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.sortedDistinct
import com.queatz.ailaai.helpers.LocationSelector
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.GroupInfo
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.components.SearchResult
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Geo
import com.queatz.db.GroupExtended
import kotlinx.coroutines.CancellationException
import kotlin.random.Random.Default.nextInt

private var groupsCache = emptyList<GroupExtended>()

@Composable
fun GroupsScreen(
    geo: Geo?,
    state: LazyListState = rememberLazyListState(),
    locationSelector: LocationSelector,
    header: LazyListScope.() -> Unit = {}
) {
    val context = LocalContext.current
    var searchText by rememberSaveable { mutableStateOf("") }
    var allGroups by remember {
        mutableStateOf(groupsCache)
    }
    val categories = remember(allGroups) {
        allGroups
            .flatMap { it.group?.categories ?: emptyList() }
            .sortedDistinct()
    }
    var isLoading by rememberStateOf(allGroups.isEmpty())
    val me = me
    var reloadKey by remember {
        mutableStateOf(nextInt())
    }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var h by rememberStateOf(80.dp.px)

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

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
    ) {
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(
            start = 1.pad,
            top = 0.dp,
            end = 1.pad,
            bottom = 3.5f.pad + h.inDp()
        ),
        verticalArrangement = Arrangement.spacedBy(1.pad),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize()
    ) {
        header()

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
        PageInput(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onPlaced {
                    h = it.size.height
                }
        ) {
            SearchContent(
                locationSelector = locationSelector,
                isLoading = isLoading,
                categories = categories,
                category = selectedCategory
            ) {
                selectedCategory = it
            }
            SearchFieldAndAction(
                value = searchText,
                valueChange = { searchText = it },
                placeholder = stringResource(R.string.search),
            )
        }
    }
}
