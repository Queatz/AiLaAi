package com.queatz.ailaai.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.cards
import app.ailaai.api.myGeo
import app.ailaai.api.savedCards
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.state.latLngSaver
import com.queatz.db.Card
import io.ktor.utils.io.*
import kotlinx.coroutines.launch

var exploreInitialCategory: String? = null

private var cache = mutableMapOf<MainTab, List<Card>>()

@Composable
fun ExploreScreen() {
    val paidString = stringResource(R.string.paid)
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var value by rememberSavableStateOf("")
    var shownValue by rememberSavableStateOf(value)
    var filterPaid by rememberSavableStateOf(false)
    var selectedCategory by rememberSaveable { mutableStateOf(exploreInitialCategory) }
    var categories by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var geo: LatLng? by rememberSaveable(stateSaver = latLngSaver()) { mutableStateOf(null) }
    var mapGeo: LatLng? by rememberSaveable(stateSaver = latLngSaver()) { mutableStateOf(null) }
    var isError by rememberStateOf(false)
    var showAsMap by rememberSavableStateOf(false)
    var offset by remember { mutableIntStateOf(0) }
    val limit = 20
    var hasMore by rememberStateOf(true)
    var shownGeo: LatLng? by remember { mutableStateOf(null) }
    val nav = nav
    val locationSelector = locationSelector(
        geo,
        { geo = it },
        nav.context as Activity
    )
    var tab by rememberSavableStateOf(MainTab.Friends)
    var shownTab by rememberSavableStateOf(tab)
    var cards by remember { mutableStateOf(cache[tab] ?: emptyList()) }
    var isLoading by rememberStateOf(cards.isEmpty())
    val filters by remember(filterPaid, tab) {
        mutableStateOf(
            when (tab) {
                MainTab.Local, MainTab.Friends -> {
                    listOf(
                        SearchFilter(
                            paidString,
                            filterPaid
                        ) {
                            filterPaid = !filterPaid
                        }
                    )
                }

                else -> emptyList()
            }
        )
    }

    LaunchedEffect(geo) {
        geo?.let {
            api.myGeo(it.toGeo())
        }
    }

    LaunchedEffect(cards) {
        cache[tab] = cards
    }

    fun updateCategories() {
        selectedCategory = selectedCategory ?: exploreInitialCategory
        categories = ((exploreInitialCategory.inList() + cards
            .flatMap { it.categories ?: emptyList() }) + selectedCategory.inList())
            .sortedDistinct()
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
        val geo = (mapGeo?.takeIf { showAsMap } ?: geo) ?: return
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
                    geo.toGeo(),
                    offset = offset,
                    limit = limit,
                    paid = filterPaid,
                    search = value.notBlank,
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
                    value.notBlank,
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

    LaunchedEffect(filterPaid) {
        loadMore(clear = true)
    }

    LaunchedEffect(geo, mapGeo, value, tab) {
        if (geo == null && mapGeo == null) {
            return@LaunchedEffect
        }

        // Don't reload if moving < 100m
        if (shownGeo != null && (mapGeo?.takeIf { showAsMap }
                ?: geo ?: return@LaunchedEffect).distance(shownGeo!!) < 100 && shownValue == value && shownTab == tab) {
            return@LaunchedEffect
        }

        // The map doesn't clear for geo updates, but should for value and tab changes
        loadMore(clear = shownValue != value || shownTab != tab)
    }

    ResumeEffect {
        loadMore()
    }

    LocationScaffold(
        geo,
        locationSelector,
        appHeader = {
            AppHeader(
                stringResource(R.string.explore),
                {},
            ) {
                ScanQrCodeButton()
            }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppHeader(
                stringResource(R.string.cards),
                {
                    scope.launch {
                        state.scrollToTop()
                    }
                }
            ) {
                IconButton({
                    showAsMap = !showAsMap
                }) {
                    Icon(if (showAsMap) Icons.Outlined.ViewAgenda else Icons.Outlined.Map, stringResource(R.string.map))
                }
                ScanQrCodeButton()
            }

            val cardsOfCategory = remember(cards, selectedCategory) {
                if (selectedCategory == null) cards else cards.filter {
                    it.categories?.contains(
                        selectedCategory
                    ) == true
                }
            }

            MainTabs(tab, { tab = it })
            if (showAsMap) {
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    MapScreen(nav, cardsOfCategory) {
                        mapGeo = it
                    }
                    PageInput(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    ) {
                        SearchContent(
                            locationSelector,
                            isLoading,
                            filters,
                            categories,
                            selectedCategory
                        ) {
                            selectedCategory = it
                        }
                        SearchFieldAndAction(
                            value,
                            { value = it },
                            placeholder = stringResource(R.string.search),
                            action = {
                                Icon(Icons.Outlined.Edit, stringResource(R.string.your_cards))
                            },
                            onAction = {
                                nav.navigate("me")
                            },
                        )
                    }
                }
            } else {
                val me = me
                CardList(
                    state = state,
                    cards = cardsOfCategory,
                    isMine = { it.person == me?.id },
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
                    placeholder = stringResource(R.string.search),
                    hasMore = hasMore,
                    onLoadMore = {
                        loadMore()
                    },
                    action = {
                        Icon(Icons.Outlined.Edit, stringResource(R.string.your_cards))
                    },
                    onAction = {
                        nav.navigate("me")
                    },
                    modifier = Modifier
                        .swipeMainTabs {
                            when (val it = MainTab.entries.swipe(tab, it)) {
                                is SwipeResult.Previous -> {
                                    nav.navigate("schedule")
                                }

                                is SwipeResult.Next -> {
                                    nav.navigate("stories")
                                }

                                is SwipeResult.Select<*> -> {
                                    tab = it.item as MainTab
                                }
                            }
                        }
                ) {
                    SearchContent(
                        locationSelector,
                        isLoading,
                        filters,
                        categories,
                        selectedCategory
                    ) {
                        selectedCategory = it
                    }
                }
            }
        }
    }
}

