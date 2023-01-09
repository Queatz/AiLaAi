package com.queatz.ailaai.ui.dialogs

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.compose.ui.window.Dialog
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import at.bluesource.choicesdk.location.factory.FusedLocationProviderFactory
import at.bluesource.choicesdk.maps.common.*
import at.bluesource.choicesdk.maps.common.Map
import at.bluesource.choicesdk.maps.common.listener.OnMarkerDragListener
import at.bluesource.choicesdk.maps.common.options.MarkerOptions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.queatz.ailaai.Card
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.databinding.LayoutMapBinding
import com.queatz.ailaai.ui.components.BasicCard
import com.queatz.ailaai.ui.components.CardParentSelector
import com.queatz.ailaai.ui.components.CardParentType
import com.queatz.ailaai.ui.components.toList
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditCardLocationDialog(card: Card, activity: Activity, onDismissRequest: () -> Unit, onChange: () -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current!!

    val locationClient = FusedLocationProviderFactory.getFusedLocationProviderClient(
        activity
    )

    var locationName by remember { mutableStateOf(card.location ?: "") }
    var parentCard by remember { mutableStateOf<Card?>(null) }
    var searchCardsValue by remember { mutableStateOf("") }
    var position by remember { mutableStateOf(LatLng(card.geo?.get(0) ?: 0.0, card.geo?.get(1) ?: 0.0)) }
    val coroutineScope = rememberCoroutineScope()
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val scrollState = rememberScrollState()
    var scrollEnabled by remember { mutableStateOf(true) }

    var cardParentType by remember { mutableStateOf(CardParentType.Map) }

    val disposable = remember { CompositeDisposable() }

    DisposableEffect(Unit) {
        onDispose {
            disposable.dispose()
        }
    }

    if (card.equipped == true) {
        cardParentType = CardParentType.Person
    } else {
        card.parent?.let {
            cardParentType = CardParentType.Card

            LaunchedEffect(true) {
                try {
                    parentCard = api.card(it)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    when (permissionState.status) {
        is PermissionStatus.Denied -> {
            if (!permissionState.status.shouldShowRationale) {
                LaunchedEffect(permissionState) {
                    permissionState.launchPermissionRequest()
                }
            }
        }

        else -> {}
    }

    if (position.toList().sum() == 0.0) {
        locationClient.getLastLocation()
            .addOnFailureListener(activity) {
                it.printStackTrace()
            }
            .addOnSuccessListener {
                if (it != null) {
                    position = LatLng(it.latitude, it.longitude)
                }
            }
    }

    Dialog(onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .padding(PaddingDefault * 2)
                .fillMaxHeight(.9f)
        ) {
            Column(
                modifier = Modifier
                    .padding(PaddingDefault * 3)
            ) {
                Text(
                    stringResource(R.string.card_location),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = PaddingDefault)
                )
                OutlinedTextField(
                    locationName,
                    onValueChange = {
                        locationName = it
                    },
                    label = {
                        Text(stringResource(R.string.location_name))
                    },
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController.hide()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Text(
                    stringResource(R.string.location_name_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(PaddingValues(vertical = PaddingDefault * 2))
                )
                CardParentSelector(cardParentType) {
                    cardParentType = it

                    when (it) {
                        CardParentType.Person -> {
                            card.parent = null
                            parentCard = null
                            card.equipped = true
                        }
                        CardParentType.Map -> {
                            card.parent = null
                            parentCard = null
                            card.equipped = false
                        }
                        CardParentType.Card -> {
                            card.equipped = false
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState, enabled = scrollEnabled)
                ) {
                    Text(
                        when (cardParentType) {
                            CardParentType.Map -> stringResource(R.string.on_the_map)
                            CardParentType.Card -> stringResource(R.string.inside_another_card)
                            CardParentType.Person -> stringResource(R.string.you)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = PaddingDefault),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    when (cardParentType) {
                        CardParentType.Person -> {
                            Text(
                                stringResource(R.string.with_you_description),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(PaddingDefault)
                            )
                        }

                        CardParentType.Map -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                                    .padding(PaddingValues(vertical = PaddingDefault * 2))
                                    .clip(MaterialTheme.shapes.large)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                    .motionEventSpy {
                                        if (it.action == MotionEvent.ACTION_UP) {
                                            scrollEnabled = true
                                        }
                                    }
                                    .pointerInteropFilter {
                                        if (it.action == MotionEvent.ACTION_DOWN) {
                                            scrollEnabled = false
                                        }
                                        false
                                    }
                            ) {
                                var composed by remember { mutableStateOf(false) }
                                var marker: Marker? by remember { mutableStateOf(null) }
                                var map: Map? by remember { mutableStateOf(null) }

                                AndroidViewBinding(
                                    LayoutMapBinding::inflate,
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    if (composed) {
                                        if (marker != null) {
                                            marker?.position = position

                                            map?.animateCamera(
                                                CameraUpdateFactory.get().newCameraPosition(
                                                    CameraPosition.Builder()
                                                        .setTarget(position)
                                                        .setZoom(14f)
                                                        .build()
                                                )
                                            )
                                        }
                                        return@AndroidViewBinding
                                    } else composed = true

                                    mapFragmentContainerView.doOnAttach { it.doOnDetach { mapFragmentContainerView.removeAllViews() } }

                                    val mapFragment = mapFragmentContainerView.getFragment<MapFragment>()

                                    mapFragment.getMapObservable().subscribe {
                                        map = it
                                        map?.clear()

                                        map?.getUiSettings()?.isMapToolbarEnabled = true
                                        map?.getUiSettings()?.isMyLocationButtonEnabled = true

                                        marker = map?.addMarker(
                                            MarkerOptions
                                                .create()
                                                .position(position)
                                                .draggable(true)
                                        )!!

                                        map?.setOnMapClickListener {
                                            position = it
                                        }

                                        map?.setOnMarkerClickListener { true }
                                        map?.setOnMarkerDragListener(object : OnMarkerDragListener {
                                            override fun onMarkerDrag(marker: Marker) {}

                                            override fun onMarkerDragEnd(marker: Marker) {
                                                position = marker.position
                                            }

                                            override fun onMarkerDragStart(marker: Marker) {}
                                        })

                                        map?.moveCamera(
                                            CameraUpdateFactory.get().newCameraPosition(
                                                CameraPosition.Builder()
                                                    .setTarget(position)
                                                    .setZoom(14f)
                                                    .build()
                                            )
                                        )
                                    }.let(disposable::add)
                                }
                            }
                            Text(
                                stringResource(R.string.map_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(PaddingValues(bottom = PaddingDefault))
                            )
                        }

                        CardParentType.Card -> {
                            when (parentCard) {
                                null -> {
                                    var myCards by remember { mutableStateOf(listOf<Card>()) }
                                    var shownCards by remember { mutableStateOf(listOf<Card>()) }

                                    LaunchedEffect(myCards, searchCardsValue) {
                                        shownCards = if (searchCardsValue.isBlank()) myCards else myCards.filter {
                                            it.conversation?.contains(searchCardsValue, true) == true ||
                                                    it.name?.contains(searchCardsValue, true) == true ||
                                                    it.location?.contains(searchCardsValue, true) == true
                                        }
                                    }

                                    LaunchedEffect(true) {
                                        try {
                                            myCards = api.myCards().filter { it.id != card.id }
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                        }
                                    }

                                    OutlinedTextField(
                                        searchCardsValue,
                                        onValueChange = { searchCardsValue = it },
                                        label = { Text(stringResource(R.string.search_cards)) },
                                        shape = MaterialTheme.shapes.large,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(onSearch = {
                                            keyboardController.hide()
                                        }),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = PaddingDefault)
                                    )
                                    LazyColumn(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
                                        modifier = Modifier
                                            .weight(1f)
                                    ) {
                                        items(shownCards, { it.id!! }) {
                                            BasicCard(
                                                {
                                                    parentCard = it
                                                    card.parent = it.id
                                                },
                                                activity = activity,
                                                card = it,
                                                isChoosing = true
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    BasicCard(
                                        {
                                            parentCard = null
                                            card.parent = null
                                        },
                                        activity = activity,
                                        card = parentCard!!,
                                        isChoosing = true
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var disableSaveButton by remember { mutableStateOf(false) }

                    TextButton(
                        {
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        {
                            disableSaveButton = true

                            coroutineScope.launch {
                                try {
                                    val update = api.updateCard(
                                        card.id!!,
                                        Card(
                                            location = locationName.trim(),
                                            geo = position.toList(),
                                            parent = card.parent,
                                            equipped = card.equipped
                                        )
                                    )

                                    card.location = update.location
                                    card.equipped = update.equipped
                                    card.parent = update.parent
                                    card.geo = update.geo

                                    onDismissRequest()
                                    onChange()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    disableSaveButton = false
                                }

                            }
                        },
                        enabled = !disableSaveButton && !(cardParentType == CardParentType.Card && card.parent == null)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}
