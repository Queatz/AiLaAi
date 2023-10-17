package com.queatz.ailaai.ui.screens

import android.app.Activity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.api.cards
import com.queatz.ailaai.api.myGeo
import com.queatz.ailaai.api.savedCards
import com.queatz.ailaai.data.Card
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.ktor.utils.io.*
import kotlinx.coroutines.launch

var exploreInitialCategory: String? = null

@Composable
fun ExploreScreen(navController: NavController, me: () -> Person?) {
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var value by rememberSaveable { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(exploreInitialCategory) }
    var categories by remember { mutableStateOf(emptyList<String>()) }
    var geo: LatLng? by remember { mutableStateOf(null) }
    var shownValue by rememberSaveable { mutableStateOf("") }
    var cards by remember { mutableStateOf(emptyList<Card>()) }
    var hasInitialCards by rememberStateOf(false)
    var isLoading by rememberStateOf(true)
    var isError by rememberStateOf(false)
    var offset by remember { mutableStateOf(0) }
    val limit = 20
    var hasMore by rememberStateOf(true)
    var shownGeo: LatLng? by remember { mutableStateOf(null) }
    val locationSelector = locationSelector(
        geo,
        { geo = it },
        navController.context as Activity
    )
    var tab by rememberSavableStateOf(MainTab.Friends)
    var shownTab by rememberSaveable { mutableStateOf(tab) }

    LaunchedEffect(geo) {
        geo?.let {
            api.myGeo(it)
        }
    }

    fun updateCategories() {
        selectedCategory = selectedCategory ?: exploreInitialCategory
        categories = ((exploreInitialCategory?.let(::listOf) ?: emptyList()) + cards
            .flatMap { it.categories ?: emptyList() })
            .distinct()
        exploreInitialCategory = null
    }

    fun onNewPage(page: List<Card>, clear: Boolean) {
        val oldSize = if (clear) 0 else cards.size
        cards = if (clear) {
            page
        } else {
            (cards + page).distinctBy { it.id }
        }
        updateCategories()
        offset = cards.size
        hasMore = cards.size > oldSize
        isError = false
        isLoading = false
        shownGeo = geo
        shownValue = value
        shownTab = tab

        if (clear) {
            scope.launch {
                state.scrollToTop()
            }
        }
    }

    suspend fun loadMore(clear: Boolean = false) {
        if (clear) {
            offset = 0
            hasMore = true
            isLoading = true
            cards = emptyList()
        }
        when (tab) {
            MainTab.Friends,
            MainTab.Local -> {
                api.cards(
                    geo!!,
                    offset = offset,
                    limit = limit,
                    search = value.takeIf { it.isNotBlank() },
                    public = tab == MainTab.Local,
                    onError = { ex ->
                        if (ex is CancellationException || ex is InterruptedException) {
                            // Ignore, probably geo or search value changed, keep isLoading = true
                        } else {
                            isLoading = false
                            isError = true
                        }
                    }
                ) {
                    onNewPage(it, clear)
                }
            }
            MainTab.Saved -> {
                api.savedCards(
                    offset,
                    limit,
                    value.takeIf { it.isNotBlank() },
                    onError = { ex ->
                        if (ex is CancellationException || ex is InterruptedException) {
                            // Ignore, probably geo or search value changed, keep isLoading = true
                        } else {
                            isLoading = false
                            isError = true
                        }
                    }) {
                    onNewPage(it.mapNotNull { it.card }, clear)
                }
            }
        }
    }

    LaunchedEffect(geo, value, tab) {
        if (geo == null) {
            return@LaunchedEffect
        }

        if (hasInitialCards) {
            hasInitialCards = false
            return@LaunchedEffect
        }

        // Don't reload if moving < 100m
        if (shownGeo != null && geo!!.distance(shownGeo!!) < 100 && shownValue == value && shownTab == tab) {
            return@LaunchedEffect
        }

        loadMore(clear = true)
    }

    LocationScaffold(
        geo,
        locationSelector,
        navController,
        appHeader = {
            AppHeader(
                navController,
                stringResource(R.string.explore),
                {},
                me
            ) {
                ScanQrCodeButton(navController)
            }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            AppHeader(
                navController,
                stringResource(R.string.explore),
                {
                    scope.launch {
                        state.scrollToTop()
                    }
                },
                me
            ) {
                IconButton({
                    navController.navigate("map")
                }) {
                    Icon(Icons.Outlined.Map, stringResource(R.string.show_on_map))
                }
                ScanQrCodeButton(navController)
            }
            MainTabs(tab, { tab = it })
            CardList(
                state = state,
                cards = if (selectedCategory == null) cards else cards.filter { it.categories?.contains(selectedCategory) == true },
                isMine = { it.person == me()?.id },
                geo = geo,
                onChanged = {
                    scope.launch {
                        loadMore(clear = true)
                    }
                },
                isLoading = isLoading,
                isError = isError,
                value = value,
                valueChange = { value = it },
                navController = navController,
                placeholder = stringResource(R.string.explore_search_placeholder),
                hasMore = hasMore,
                onLoadMore = {
                    loadMore()
                },
                action = {
                    Icon(Icons.Outlined.Edit, stringResource(R.string.your_cards))
                },
                onAction = {
                    navController.navigate("me")
                }
            ) {
                if (locationSelector.isManual) {
                    ElevatedButton(
                        elevation = ButtonDefaults.elevatedButtonElevation(ElevationDefault * 2),
                        onClick = {
                            locationSelector.reset()
                        }
                    ) {
                        Text(stringResource(R.string.reset_location), modifier = Modifier.padding(end = PaddingDefault))
                        Icon(Icons.Outlined.Clear, stringResource(R.string.reset_location))
                    }
                }
                if (categories.size > 2 && !isLoading) {
                    var viewport by remember { mutableStateOf(Size(0f, 0f)) }
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .horizontalScroll(scrollState)
                            .onPlaced { viewport = it.boundsInParent().size }
                            .horizontalFadingEdge(viewport, scrollState)
                    ) {
                        categories.forEachIndexed { index, category ->
                            OutlinedButton(
                                {
                                    selectedCategory = if (selectedCategory == category) {
                                        null
                                    } else {
                                        category
                                    }
                                },
                                border = IconButtonDefaults.outlinedIconToggleButtonBorder(
                                    true,
                                    selectedCategory == category
                                ),
                                colors = if (selectedCategory != category) ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ) else ButtonDefaults.buttonColors(),
                                modifier = Modifier.padding(end = PaddingDefault)
                            ) {
                                Text(category)
                            }
                        }
                    }
                }
            }
        }
    }
}
