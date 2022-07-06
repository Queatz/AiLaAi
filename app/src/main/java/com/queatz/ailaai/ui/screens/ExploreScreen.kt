package com.queatz.ailaai.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.queatz.ailaai.Card
import com.queatz.ailaai.Person
import com.queatz.ailaai.api
import com.queatz.ailaai.toLatLng
import com.queatz.ailaai.ui.components.BasicCard
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(navController: NavController, me: Person?) {
    val locationClient = FusedLocationProviderFactory.getFusedLocationProviderClient(
        navController.context as Activity
    )
    var value by remember { mutableStateOf("") }
    var geo: LatLng? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }
    var cards by remember { mutableStateOf(listOf<Card>()) }
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
            Button(
                {
                    if (permissionState.status.shouldShowRationale) {
                        permissionState.launchPermissionRequest()
                    } else {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${navController.context.packageName}")
                        )
                        (navController.context as Activity).startActivity(intent)
                    }
                }
            ) {
                Text(if (permissionState.status.shouldShowRationale) "Find my location" else "Open Settings")
            }

            if (permissionState.status.shouldShowRationale.not()) {
                Text(
                    "The location permission is disabled in settings.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    } else if (geo == null) {
        locationClient.observeLocation(LocationRequest.createDefault())
            .filter { it is Outcome.Success && it.value.lastLocation != null }
            .takeWhile { coroutineScope.isActive }
                // todo dispose on close
            .take(1)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                geo = (it as Outcome.Success).value.lastLocation!!.toLatLng()
            }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingDefault)
        ) {
            Text("Finding your location...", color = MaterialTheme.colorScheme.secondary)
        }
    } else {
        LaunchedEffect(geo, value) {
            isLoading = true
            try {
                cards = api.cards(geo!!, value.takeIf { it.isNotBlank() })
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            isLoading = false
        }

        Box {
            LazyColumn(
                contentPadding = PaddingValues(
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading) {
                    item {
                        LinearProgressIndicator(
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaddingDefault)
                        )
                    }
                }

                items(cards) {
                    BasicCard(
                        {
                            coroutineScope.launch {
                                try {
                                    navController.navigate("group/${api.cardGroup(it.id!!).id!!}")
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
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(),
                elevation = CardDefaults.elevatedCardElevation(ElevationDefault / 2),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PaddingDefault * 2)
                    .fillMaxWidth()
            ) {
                val keyboardController = LocalSoftwareKeyboardController.current!!
                OutlinedTextField(
                    value,
                    onValueChange = { value = it },
                    placeholder = { Text("Search") },
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController.hide()
                    }),
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}
