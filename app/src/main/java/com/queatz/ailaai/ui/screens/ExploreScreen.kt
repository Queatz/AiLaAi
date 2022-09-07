package com.queatz.ailaai.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import at.bluesource.choicesdk.core.Outcome
import at.bluesource.choicesdk.location.common.LocationRequest
import at.bluesource.choicesdk.location.factory.FusedLocationProviderFactory
import at.bluesource.choicesdk.maps.common.LatLng
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.BasicCard
import com.queatz.ailaai.ui.state.gsonSaver
import com.queatz.ailaai.ui.state.latLngSaver
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(navController: NavController, me: () -> Person?) {
    val locationClient = FusedLocationProviderFactory.getFusedLocationProviderClient(
        navController.context as Activity
    )
    var value by rememberSaveable { mutableStateOf("") }
    var geo: LatLng? by rememberSaveable(stateSaver = latLngSaver()) { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }
    var cards by rememberSaveable(stateSaver = gsonSaver<List<Card>>()) { mutableStateOf(listOf()) }
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val coroutineScope = rememberCoroutineScope()

    if (!permissionState.status.isGranted) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PaddingDefault * 2, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingDefault)
        ) {
            val showOpenSettings = permissionState.status.shouldShowRationale

            Button(
                {
                    if (showOpenSettings) {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${navController.context.packageName}")
                        )
                        (navController.context as Activity).startActivity(intent)
                    } else {
                        permissionState.launchPermissionRequest()
                    }
                }
            ) {
                Text(if (showOpenSettings) stringResource(R.string.open_settings) else stringResource(R.string.find_my_location))
            }

            if (showOpenSettings) {
                Text(
                    stringResource(R.string.location_disabled_description),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    } else if (geo == null) {
        LaunchedEffect(true) {
            locationClient.observeLocation(LocationRequest.createDefault())
                .filter { it is Outcome.Success && it.value.lastLocation != null }
                .takeWhile { coroutineScope.isActive }
                // todo dispose on close
                .take(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    geo = (it as Outcome.Success).value.lastLocation!!.toLatLng()
                }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingDefault)
        ) {
            Text(stringResource(R.string.finding_your_location), color = MaterialTheme.colorScheme.secondary)
        }
    } else {
        var initialSearch by remember { mutableStateOf(cards.isEmpty()) }

        LaunchedEffect(geo, value) {
            if (!initialSearch) {
                initialSearch = true
                return@LaunchedEffect
            }

            isLoading = true
            try {
                cards = api.cards(geo!!, value.takeIf { it.isNotBlank() }).filter { it.person != me()?.id }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            isLoading = false
        }

        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingDefault * 2, vertical = PaddingDefault + 80.dp)
                )
            } else if (cards.isEmpty()) {
                    Text(
                        stringResource(R.string.no_cards_to_show),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .padding(horizontal = PaddingDefault * 2, vertical = PaddingDefault + 80.dp)
                    )
            } else {
                LazyVerticalGrid(
                    contentPadding = PaddingValues(
                        PaddingDefault,
                        PaddingDefault,
                        PaddingDefault,
                        PaddingDefault + 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Top),
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(240.dp)
                ) {
                    items(items = cards, key = { it.id!! }) {
                        BasicCard(
                            {
                                navController.navigate("card/${it.id!!}")
                            },
                            onReply = {
                                coroutineScope.launch {
                                    try {
                                        val groupId = api.cardGroup(it.id!!).id!!
                                        api.sendMessage(
                                            groupId,
                                            Message(attachment = gson.toJson(CardAttachment(it.id!!)))
                                        )
                                        navController.navigate("group/${groupId}")
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                            },
                            activity = navController.context as Activity,
                            card = it
                        )
                    }
                }
            }
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(),
                elevation = CardDefaults.elevatedCardElevation(ElevationDefault / 2),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PaddingDefault * 2)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
            ) {
                val keyboardController = LocalSoftwareKeyboardController.current!!
                OutlinedTextField(
                    value,
                    onValueChange = { value = it },
                    placeholder = { Text(stringResource(R.string.search)) },
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController.hide()
                    }),
                    trailingIcon = {
                        if (value.isNotEmpty()) {
                            Icon(Icons.Outlined.Close, stringResource(R.string.clear), modifier = Modifier.clickable {
                                value = ""
                            })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}
