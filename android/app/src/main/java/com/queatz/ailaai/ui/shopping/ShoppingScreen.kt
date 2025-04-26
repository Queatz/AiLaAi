package com.queatz.ailaai.ui.shopping

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.queatz.ailaai.ui.components.SearchFieldAndAction
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
    var isLoading by rememberStateOf(true)
    var geo: LatLng? by remember { mutableStateOf(null) }
    val nav = nav
    val reloadFlow = remember {
        MutableSharedFlow<Boolean>()
    }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var categories by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showEditCardDialog by remember { mutableStateOf(false) }
    val locationSelector = locationSelector(
        geo = geo,
        onGeoChange = { geo = it },
        activity = nav.context as Activity
    )

    LaunchedEffect(allCards) {
        categories = allCards
            .flatMap { it.categories ?: emptyList() }
            .sortedDistinct()
    }

    val filteredCards = remember(
        allCards,
        searchText,
        selectedCategory
    ) {
        allCards
            .filter { card ->
                (searchText.isBlank() || card.name?.contains(searchText, true) == true ||
                        card.conversation?.contains(searchText, true) == true ||
                        card.location?.contains(searchText, true) == true) &&
                (selectedCategory == null || card.categories?.contains(selectedCategory) == true)
            }
    }

    suspend fun reload(passive: Boolean = false) {
        isLoading = !passive || allCards.isEmpty()
        geo?.let { currentGeo ->
            api.cards(
                geo = currentGeo.toGeo(),
                paid = true,
                search = searchText.notBlank,
                onError = {
                    if (!passive && it !is CancellationException) {
                        // Handle error
                    }
                }
            ) {
                allCards = it
            }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        reloadFlow.collectLatest {
            reload(it)
        }
    }

    LaunchedEffect(geo, searchText) {
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
                        showEditCardDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sell,
                        contentDescription = stringResource(R.string.sell)
                    )
                }
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
                // Categories filter
                if (categories.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.pad),
                        modifier = Modifier.padding(horizontal = 1.pad)
                    ) {
                        categories.forEach { category ->
                            val isSelected = selectedCategory == category
                            Button(
                                onClick = {
                                    selectedCategory = if (isSelected) null else category
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(category)
                            }
                        }
                    }
                }
                SearchFieldAndAction(
                    value = searchText,
                    valueChange = { searchText = it },
                    placeholder = stringResource(R.string.search),
                )
            }
        }
    }
}
