package com.queatz.ailaai.ui.dialogs

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import at.bluesource.choicesdk.location.factory.FusedLocationProviderFactory
import at.bluesource.choicesdk.maps.common.LatLng
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.queatz.ailaai.R
import com.queatz.ailaai.api.card
import com.queatz.ailaai.api.myCollaborations
import com.queatz.ailaai.api.updateCard
import com.queatz.ailaai.data.Card
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.isTrue
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toList
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalPermissionsApi::class)
@Composable
fun EditCardLocationDialog(
    card: Card,
    navController: NavController,
    activity: Activity,
    onDismissRequest: () -> Unit,
    onChange: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current!!
    val locationClient = FusedLocationProviderFactory.getFusedLocationProviderClient(activity)

    var parentCard by remember { mutableStateOf<Card?>(null) }
    var searchCardsValue by remember { mutableStateOf("") }
    var position by remember { mutableStateOf(LatLng(card.geo?.get(0) ?: 0.0, card.geo?.get(1) ?: 0.0)) }
    val coroutineScope = rememberCoroutineScope()
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val scrollState = rememberScrollState()
    var scrollEnabled by rememberStateOf(true)

    var cardParentType by remember { mutableStateOf<CardParentType?>(null) }

    if (card.offline.isTrue) {
        cardParentType = null
    } else if (card.equipped == true) {
        cardParentType = CardParentType.Person
    } else if (card.parent != null) {
        cardParentType = CardParentType.Card

        LaunchedEffect(Unit) {
            api.card(card.parent!!) { parentCard = it }
        }
    } else if (card.geo != null) {
        cardParentType = CardParentType.Map
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

    DialogBase(onDismissRequest, dismissable = false, modifier = Modifier.wrapContentHeight()) {
        Column(
            modifier = Modifier
                .padding(PaddingDefault * 3)
                .verticalScroll(scrollState, enabled = scrollEnabled)
        ) {
            Text(
                stringResource(R.string.card_location),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = PaddingDefault)
            )
            CardParentSelector(cardParentType) {
                cardParentType = if (cardParentType == it) {
                    null
                } else {
                    it
                }

                when (cardParentType) {
                    CardParentType.Person -> {
                        card.parent = null
                        card.offline = null
                        parentCard = null
                        card.equipped = true
                    }

                    CardParentType.Map -> {
                        card.parent = null
                        card.offline = null
                        parentCard = null
                        card.equipped = false
                    }

                    CardParentType.Card -> {
                        card.equipped = false
                        card.offline = false
                    }

                    else -> {
                        card.offline = true
                        card.parent = null
                        parentCard = null
                        card.equipped = false
                    }
                }
            }
            Column(
                modifier = Modifier
            ) {
                Text(
                    when (cardParentType) {
                        CardParentType.Map -> stringResource(R.string.at_a_location)
                        CardParentType.Card -> stringResource(R.string.inside_another_card)
                        CardParentType.Person -> stringResource(R.string.on_profile)
                        else -> stringResource(R.string.offline)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = PaddingDefault),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                when (cardParentType) {
                    CardParentType.Person -> {
                        Text(
                            stringResource(R.string.on_profile_description),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = PaddingDefault)
                        )
                    }

                    CardParentType.Map -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
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
                            MapWithMarker(14f, position) {
                                position = it
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

                                LaunchedEffect(Unit) {
                                    api.myCollaborations {
                                        myCards = it.filter { it.id != card.id }
                                    }
                                }

                                OutlinedTextField(
                                    searchCardsValue,
                                    onValueChange = { searchCardsValue = it },
                                    label = { Text(stringResource(R.string.search_cards)) },
                                    shape = MaterialTheme.shapes.large,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        keyboardController.hide()
                                    }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = PaddingDefault)
                                )
                                LazyVerticalGrid(
                                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
                                    verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                                    columns = GridCells.Adaptive(120.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(.75f)
                                ) {
                                    items(shownCards, { it.id!! }) {
                                        CardItem(
                                            {
                                                parentCard = it
                                                card.parent = it.id
                                            },
                                            onCategoryClick = {},
                                            card = it,
                                            navController = navController,
                                            isChoosing = true
                                        )
                                    }
                                }
                            }

                            else -> {
                                CardItem(
                                    {
                                        parentCard = null
                                        card.parent = null
                                    },
                                    onCategoryClick = {

                                    },
                                    card = parentCard!!,
                                    isChoosing = true,
                                    navController = navController,
                                    modifier = Modifier
                                        .padding(top = PaddingDefault)
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            stringResource(R.string.discoverable_by_link),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = PaddingDefault)
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                var disableSaveButton by rememberStateOf(false)

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
                            api.updateCard(
                                card.id!!,
                                Card(
                                    geo = position.toList(),
                                    parent = card.parent,
                                    equipped = card.equipped,
                                    offline = card.offline
                                )
                            ) { update ->
                                card.location = update.location
                                card.equipped = update.equipped
                                card.offline = update.offline
                                card.parent = update.parent
                                card.geo = update.geo

                                onDismissRequest()
                                onChange()
                            }
                            disableSaveButton = false
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

