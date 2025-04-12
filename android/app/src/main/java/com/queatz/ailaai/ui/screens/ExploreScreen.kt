package com.queatz.ailaai.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.distance
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.formatDistance
import com.queatz.ailaai.extensions.hint
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.rememberSavableStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.extensions.toLatLng
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.DisplayText
import com.queatz.ailaai.ui.components.LocationScaffold
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.ScanQrCodeButton
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.components.SearchFilter
import com.queatz.ailaai.ui.control.mapCardsControl
import com.queatz.ailaai.ui.control.mapInventoryControl
import com.queatz.ailaai.ui.state.latLngSaver
import com.queatz.ailaai.ui.story.StoriesScreen
import com.queatz.ailaai.ui.story.StoriesScreenState
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen() {
    val paidString = stringResource(R.string.paid)
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var value by rememberSavableStateOf("")
    var shownValue by rememberSavableStateOf(value)
    var filterPaid by rememberSavableStateOf(false)
    var geo: LatLng? by rememberSaveable(stateSaver = latLngSaver()) { mutableStateOf(null) }
    var mapGeo: LatLng? by rememberSaveable(stateSaver = latLngSaver()) { mutableStateOf(null) }
    var mapAltitude: Double? by rememberSaveable { mutableStateOf<Double?>(null) }
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
        filterPaid = filterPaid,
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

    val filters by remember(filterPaid, cards) {
        mutableStateOf(
            if (cards.any { it.pay != null }) {
                SearchFilter(
                    name = paidString,
                    icon = Icons.Outlined.Payments,
                    selected = filterPaid
                ) {
                    filterPaid = !filterPaid
                }.inList()
            } else {
                emptyList()
            }
        )
    }

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
    val storiesState = remember { StoriesScreenState() }

    LaunchedEffect(geo) {
        geo?.let {
            api.myGeo(it.toGeo())
        }
    }

    LaunchedEffect(filterPaid) {
        mapCardsControl.loadMore(true)
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
            StoriesScreen(
                mapCardsControl = mapCardsControl,
                geo = mapGeo ?: geo,
                myGeo = geo,
                title = mapCardsControl.areaCard?.name,
                hint = mapCardsControl.areaCard?.hint,
                distance = geo?.let { myGeo ->
                    mapCardsControl.areaCard?.geo?.toLatLng()?.let {
                        myGeo.distance(it).formatDistance()
                    }
                },
                onTitleClick = {
                    mapCardsControl.areaCard?.takeIf { !it.name.isNullOrBlank() }?.let { card ->
                        nav.appNavigate(AppNav.Page(card.id!!))
                    }
                },
                onExpandRequest = {
                    scope.launch {
                        if (bottomSheetState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                            bottomSheetState.bottomSheetState.expand()
                        }
                    }
                },
                valueChange = { value = it },
                storiesState = storiesState,
                value = value,
                locationSelector = locationSelector,
                filters = filters,
            )
        }
    ) { paddingValues ->
        val title = stringResource(R.string.map)
        LocationScaffold(
            geo = geo,
            locationSelector = locationSelector,
            appHeader = {
                AppHeader(
                    title = title,
                    onTitleClick = {},
                ) {
                    ScanQrCodeButton()
                }
            },
            modifier = Modifier.padding(paddingValues),
            rationale = {
                // todo: translate
                DisplayText("Discover and post pages in your town.")
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(paddingValues),
            ) {
                AppHeader(
                    title = title,
                    onTitleClick = {
                        scope.launch {
                            state.scrollToTop()
                        }
                    }
                ) {
                    ScanQrCodeButton()
                }

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