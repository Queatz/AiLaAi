package com.queatz.ailaai.ui.shopping

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.cards
import app.ailaai.api.savedCards
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.inDp
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.sortedDistinct
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.card.template.CreateCardWithTemplate
import com.queatz.ailaai.ui.card.template.CreateCardWithTemplateDialog
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.CardItem
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.ScanQrCodeButton
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.screens.SearchContent
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun ShoppingScreen() {
    val state = rememberLazyGridState()
    var searchText by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var allCards by remember { mutableStateOf(emptyList<Card>()) }
    var favoriteCards by remember { mutableStateOf(emptyList<Card>()) }
    var isLoading by rememberStateOf(true)
    var hasMore by remember { mutableStateOf(true) }
    val pageSize = 20
    var page by remember { mutableStateOf(0) }
    var geo: LatLng? by remember { mutableStateOf(null) }
    val nav = nav
    val reloadFlow = remember {
        MutableSharedFlow<Boolean>()
    }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var categories by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showEditCardDialog by remember { mutableStateOf(false) }
    var showFavorites by rememberSaveable { mutableStateOf(false) }
    val locationSelector = locationSelector(
        geo = geo,
        onGeoChange = { geo = it },
        activity = nav.context as Activity
    )

    LaunchedEffect(allCards, favoriteCards, showFavorites) {
        categories = if (showFavorites) {
            favoriteCards
        } else {
            allCards
        }
            .flatMap { it.categories ?: emptyList() }
            .sortedDistinct()
    }

    val filteredCards = remember(
        allCards,
        favoriteCards,
        searchText,
        selectedCategory,
        showFavorites
    ) {
        if (showFavorites) {
            favoriteCards
        } else {
            allCards

        }.filter { card ->
            (searchText.isBlank() || card.name?.contains(searchText, true) == true ||
                    card.conversation?.contains(searchText, true) == true ||
                    card.location?.contains(searchText, true) == true) &&
                    (selectedCategory == null || card.categories?.contains(selectedCategory) == true)
        }
    }

    suspend fun reload(passive: Boolean = false, loadMore: Boolean = false) {
        if (!loadMore) {
            page = 0
            hasMore = true
        }

        isLoading = !passive || (if (showFavorites) favoriteCards.isEmpty() else allCards.isEmpty())

        if (showFavorites) {
            api.savedCards(
                search = searchText.notBlank,
                offset = if (loadMore) page * pageSize else 0,
                limit = pageSize,
                onError = { error ->
                    if (!passive && error !is CancellationException) {
                        // Handle error
                    }
                }
            ) { result ->
                favoriteCards = if (loadMore) {
                    (favoriteCards + result.mapNotNull { it.card }).distinctBy { it.id!! }
                } else {
                    result.mapNotNull { it.card }
                }
                hasMore = result.isNotEmpty()
            }
        } else {
            geo?.let { currentGeo ->
                api.cards(
                    geo = currentGeo.toGeo(),
                    paid = true,
                    search = searchText.notBlank,
                    offset = if (loadMore) page * pageSize else 0,
                    limit = pageSize,
                    onError = { error ->
                        if (!passive && error !is CancellationException) {
                            // Handle error
                        }
                    }
                ) { result ->
                    allCards = if (loadMore) {
                        (allCards + result).distinctBy { it.id!! }
                    } else {
                        result
                    }
                    hasMore = result.isNotEmpty()
                    if (loadMore && result.isNotEmpty()) {
                        page++
                    }
                }
            }
        }

        isLoading = false
    }

    suspend fun loadMore() {
        if (hasMore && !isLoading) {
            reload(passive = true, loadMore = true)
        }
    }

    LaunchedEffect(Unit) {
        reloadFlow.collectLatest { passive ->
            reload(passive, false)
        }
    }

    LaunchedEffect(geo, searchText, showFavorites) {
        reloadFlow.emit(false)
    }

    ResumeEffect {
        reloadFlow.emit(true)
    }

    if (showEditCardDialog) {
        CreateCardWithTemplateDialog(
            onDismissRequest = {
                showEditCardDialog = false
            },
            template = CreateCardWithTemplate.Product,
        ) { card ->
            nav.appNavigate(AppNav.Page(card.id!!))
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppHeader(
            title = stringResource(R.string.shopping),
            onTitleClick = {
                scope.launch {
                    state.scrollToTop()
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        showFavorites = !showFavorites
                        scope.launch {
                            reloadFlow.emit(false)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (showFavorites) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        // todo: translate
                        contentDescription = "Favorites"
                    )
                }
                IconButton(
                    onClick = {
                        showEditCardDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sell,
                        contentDescription = stringResource(R.string.sell)
                    )
                }
                ScanQrCodeButton()
            }
        )

        var h by rememberStateOf(80.dp.px)

        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyVerticalGrid(
                state = state,
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(
                    1.pad,
                    1.pad,
                    1.pad,
                    3.pad + h.inDp()
                ),
                verticalArrangement = Arrangement.spacedBy(1.pad),
                horizontalArrangement = Arrangement.spacedBy(1.pad),
                modifier = Modifier.fillMaxSize()
            ) {
                val maxLineSpan = 2
                if (isLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Loading()
                    }
                } else if (filteredCards.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.no_cards),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(2.pad)
                        )

                        Button(
                            onClick = {
                                nav.appNavigate(AppNav.Explore)
                            }
                        ) {
                            Icon(Icons.Outlined.Search, null)
                            Text(
                                text = stringResource(R.string.explore),
                                modifier = Modifier.padding(start = 1.pad)
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredCards,
                        key = { it.id!! }
                    ) { card ->
                        CardItem(
                            card = card,
                            onClick = {
                                nav.appNavigate(AppNav.Page(card.id!!))
                            }
                        )

                        // Check if we're at the last item and need to load more
                        if (card == filteredCards.lastOrNull()) {
                            LaunchedEffect(card) {
                                loadMore()
                            }
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
                    category = selectedCategory,
                    onCategory = { selectedCategory = it }
                )
                SearchFieldAndAction(
                    value = searchText,
                    valueChange = { searchText = it },
                    placeholder = stringResource(R.string.search),
                )
            }
        }
    }
}
