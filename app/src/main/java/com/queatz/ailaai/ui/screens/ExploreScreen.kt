package com.queatz.ailaai.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import at.bluesource.choicesdk.maps.common.LatLng
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.huawei.hms.hmsscankit.ScanKitActivity
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsScan
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.api.cards
import com.queatz.ailaai.api.myGeo
import com.queatz.ailaai.data.Card
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.CardList
import com.queatz.ailaai.ui.components.LocationScaffold
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val qrCodeExplainedKey = booleanPreferencesKey("tutorial.qrCode.explained")

var exploreInitialCategory: String? = null

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ExploreScreen(navController: NavController, me: () -> Person?) {
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var value by rememberSaveable { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(exploreInitialCategory) }
    var categories by remember { mutableStateOf(emptyList<String>()) }
    var geo: LatLng? by remember { mutableStateOf(null) }
    var shownValue by rememberSaveable { mutableStateOf("") }
    var cards by remember { mutableStateOf(emptyList<Card>()) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val initialCameraPermissionState by remember { mutableStateOf(cameraPermissionState.status.isGranted) }
    var showCameraRationale by rememberStateOf(false)
    var showQrCodeExplanationDialog by rememberStateOf(false)
    var hasInitialCards by rememberStateOf(false)
    var isLoading by rememberStateOf(true)
    var isError by rememberStateOf(false)
    var offset by remember { mutableStateOf(0) }
    var hasMore by rememberStateOf(true)
    var shownGeo: LatLng? by remember { mutableStateOf(null) }
    val locationSelector = locationSelector(
        geo,
        { geo = it },
        navController.context as Activity
    )

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

    suspend fun loadMore(clear: Boolean = false) {
        if (clear) {
            offset = 0
            hasMore = true
            isLoading = true
            cards = emptyList()
        }
        api.cards(
            geo!!,
            offset = offset,
            limit = 20,
            search = value.takeIf { it.isNotBlank() },
            onError = { ex ->
                if (ex is CancellationException || ex is InterruptedException) {
                    // Ignore, probably geo or search value changed, keep isLoading = true
                } else {
                    isLoading = false
                    isError = true
                }
            }
        ) { page ->
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
        }
    }

    LaunchedEffect(geo, value) {
        if (geo == null) {
            return@LaunchedEffect
        }

        if (hasInitialCards) {
            hasInitialCards = false
            return@LaunchedEffect
        }

        // Don't reload if moving < 100m
        if (shownGeo != null && geo!!.distance(shownGeo!!) < 100 && shownValue == value) {
            return@LaunchedEffect
        }

        loadMore(clear = true)
    }

    val scanQrLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getParcelableExtra<HmsScan?>(ScanUtil.RESULT)
                ?.let {
                    it.linkUrl?.linkValue?.takeIf { it.startsWith(appDomain) }?.drop(appDomain.length)?.let {
                        when {
                            it.startsWith("/card/") -> {
                                val cardId = it.split("/").getOrNull(2)
                                navController.navigate("card/$cardId")
                                true
                            }
                            it.startsWith("/story/") -> {
                                val cardId = it.split("/").getOrNull(2)
                                navController.navigate("story/$cardId")
                                true
                            }
                            it.startsWith("/profile/") -> {
                                val cardId = it.split("/").getOrNull(2)
                                navController.navigate("profile/$cardId")
                                true
                            }
                            else -> null
                        }
                    }
                } ?: run {
                context.showDidntWork()
            }
        }
    }

    fun scanQrCode() {
        if (cameraPermissionState.status.isGranted) {
            scanQrLauncher.launch(
                // https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides/android-parsing-result-codes-0000001050043969
                // Extracted from ScanUtil.java (startScan)
                Intent(navController.context as Activity, ScanKitActivity::class.java).apply {
                    putExtra("ScanFormatValue", HmsScan.QRCODE_SCAN_TYPE)
                    putExtra("ScanViewValue", 1)
                }
            )
        } else {
            if (cameraPermissionState.status.shouldShowRationale) {
                showCameraRationale = true
            } else {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }

    fun launchScanQrCode() {
        scope.launch {
            if (context.dataStore.data.first()[qrCodeExplainedKey] == true) {
                scanQrCode()
            } else {
                showQrCodeExplanationDialog = true
            }
        }
    }

    if (!initialCameraPermissionState) {
        LaunchedEffect(cameraPermissionState.status.isGranted) {
            if (cameraPermissionState.status.isGranted) {
                scanQrCode()
            }
        }
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
                me,
                showAppIcon = true
            )
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
                me,
                actions = {
                    IconButton({
                        navController.navigate("map")
                    }) {
                        Icon(Icons.Outlined.Map, stringResource(R.string.show_on_map))
                    }
                    IconButton({
                        launchScanQrCode()
                    }) {
                        Icon(Icons.Outlined.QrCodeScanner, stringResource(R.string.scan))
                    }
                },
                showAppIcon = true
            )
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
                showDistance = true,
                placeholder = stringResource(R.string.explore_search_placeholder),
                hasMore = hasMore,
                onLoadMore = {
                    loadMore()
                },
                action = {
                    Icon(Icons.Outlined.Add, stringResource(R.string.add_a_card))
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

    if (showQrCodeExplanationDialog) {
        AlertDialog(
            {
                showQrCodeExplanationDialog = false
            },
            title = {
                Text(stringResource(R.string.scan))
            },
            text = {
                Text(stringResource(R.string.scan_a_qr_code_description))
            },
            confirmButton = {
                TextButton(
                    {
                        scope.launch {
                            context.dataStore.edit {
                                it[qrCodeExplainedKey] = true
                            }
                            showQrCodeExplanationDialog = false
                            scanQrCode()
                        }
                    }
                ) {
                    Text(stringResource(R.string.scan_now))
                }
            },
            dismissButton = {
                TextButton(
                    {
                        showQrCodeExplanationDialog = false
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showCameraRationale) {
        AlertDialog(
            { showCameraRationale = false },
            text = {
                Text(stringResource(R.string.camera_disabled_description))
            },
            confirmButton = {
                TextButton(
                    {
                        showCameraRationale = false
                        navController.goToSettings()
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            }
        )
    }
}
