package com.queatz.ailaai.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.myGeo
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.control.mapCardsControl
import com.queatz.ailaai.ui.control.mapInventoryControl
import com.queatz.ailaai.ui.state.latLngSaver
import com.queatz.ailaai.ui.story.SheetScreen
import com.queatz.ailaai.ui.story.SheetScreenState
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen() {
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var value by rememberSavableStateOf("")
    var shownValue by rememberSavableStateOf(value)
    var geo: LatLng? by rememberSaveable(stateSaver = latLngSaver()) { mutableStateOf(null) }
    var mapGeo: LatLng? by rememberSaveable(stateSaver = latLngSaver()) { mutableStateOf(null) }
    var mapAltitude: Double? by rememberSaveable { mutableStateOf(null) }
    var shownGeo: LatLng? by remember { mutableStateOf(null) }
    val nav = nav
    val locationSelector = locationSelector(
        geo = geo,
        onGeoChange = { geo = it },
        activity = nav.context as Activity
    )

    val mapCardsControl = mapCardsControl(
        geo = mapGeo ?: geo,
        altitude = mapAltitude,
        value = value,
        onLoadNewPage = { geo, value, clear ->
            shownGeo = geo
            shownValue = value
            if (clear) {
                scope.launch {
                    state.scrollToTop()
                }
            }
        }
    )

    val cards = mapCardsControl.cards
    val filters = emptyList<SearchFilter>()

    val mapInventoryControl = mapInventoryControl(geo = mapGeo ?: geo)
    val mapControl = remember { MapControl() }
    val mapCategoriesControl = mapCardsControl.mapCategoriesControl

    // Posts
    val bottomSheetState = rememberBottomSheetScaffoldState()
    val sheetPeekHeight = 120.dp
    val sheetCornerRadius by animateDpAsState(
        targetValue = if (bottomSheetState.bottomSheetState.targetValue == SheetValue.Expanded) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 500)
    )
    val storiesState = remember { SheetScreenState() }

    LaunchedEffect(geo) {
        geo?.let {
            api.myGeo(it.toGeo())
        }
    }


    LaunchedEffect(geo, mapGeo, value, mapCategoriesControl.selectedCategory) {
        if (geo == null && mapGeo == null) {
            return@LaunchedEffect
        }

        val moveUnder100 = shownGeo?.let { shownGeo ->
            (mapGeo ?: geo)?.distance(shownGeo)?.let { it < 100 }
        } != false

        // Don't reload if moving < 100m
        if (shownGeo != null && moveUnder100 && shownValue == value) {
            return@LaunchedEffect
        }

        // The map doesn't clear for geo updates, but should for value and tab changes
        mapCardsControl.loadMore(
            !moveUnder100 || shownValue != value
        )
    }

    ResumeEffect {
        mapCardsControl.loadMore(false)
    }

    BackHandler(bottomSheetState.bottomSheetState.currentValue == SheetValue.Expanded) {
        scope.launch {
            bottomSheetState.bottomSheetState.partialExpand()
        }
    }

    LaunchedEffect(bottomSheetState.bottomSheetState.currentValue) {
        if (bottomSheetState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
            storiesState.scrollToTop()
        }
    }

    BottomSheetScaffold(
        scaffoldState = bottomSheetState,
        sheetPeekHeight = sheetPeekHeight,
        sheetShadowElevation = 2.elevation,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(0.dp),
        sheetDragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 1.5f.pad),
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Box(Modifier.size(width = 32.dp, height = 4.dp))
            }
        },
        sheetShape = RoundedCornerShape(
            topStart = CornerSize(sheetCornerRadius),
            topEnd = CornerSize(sheetCornerRadius),
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp),
        ),
        sheetContent = {
            if ((mapGeo ?: geo) == null) {
                EmptyText(
                    text = stringResource(R.string.updating_location)
                )
            } else {
                SheetScreen(
                    mapCardsControl = mapCardsControl,
                    geo = mapGeo ?: geo,
                    myGeo = geo,
                    onExpandRequest = {
                        scope.launch {
                            if (bottomSheetState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                                bottomSheetState.bottomSheetState.expand()
                            }
                        }
                    },
                    valueChange = { value = it },
                    sheetState = storiesState,
                    value = value,
                    locationSelector = locationSelector,
                    filters = filters,
                )
            }
        }
    ) { paddingValues ->
        LocationScaffold(
            geo = geo,
            locationSelector = locationSelector,
            appHeader = {
                AppHeader(
                    showSignalsButton = true,
                )
            },
            modifier = Modifier.padding(paddingValues),
            rationale = {
                        DisplayText(stringResource(R.string.discover_and_post_pages))
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(paddingValues),
            ) {
                AppHeader(
                    showSignalsButton = true
                )

                var viewportHeight by remember { mutableIntStateOf(0) }
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        MapScreen(
                            control = mapControl,
                            cards = mapCategoriesControl.cardsOfCategory,
                            inventories = mapInventoryControl.inventories,
                            bottomPadding = viewportHeight,
                            onCard = {
                                nav.appNavigate(AppNav.Page(it))
                            },
                            onInventory = {
                                mapInventoryControl.showInventory(it)
                            },
                            onGeo = {
                                mapGeo = it
                            },
                            onAltitude = {
                                mapAltitude = it
                            }
                        )
                    }
                    PageInput(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onPlaced { viewportHeight = it.boundsInParent().size.height.toInt() }
                    ) {
                        SearchContent(
                            locationSelector = locationSelector,
                            isLoading = mapCardsControl.isLoading,
                            filters = filters,
                            categories = mapCategoriesControl.categories,
                            category = mapCategoriesControl.selectedCategory
                        ) {
                            mapCategoriesControl.selectCategory(it)
                        }
                        SearchFieldAndAction(
                            value = value,
                            valueChange = { value = it },
                            placeholder = stringResource(R.string.search_map),
                            action = {
                                Icon(Icons.Outlined.Edit, stringResource(R.string.your_cards))
                            },
                            onAction = {
                                nav.appNavigate(AppNav.Me)
                            },
                        )
                    }
                }
            }
        }
    }
}
