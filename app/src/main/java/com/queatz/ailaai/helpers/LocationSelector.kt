package com.queatz.ailaai.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import at.bluesource.choicesdk.core.Outcome
import at.bluesource.choicesdk.location.common.LocationRequest
import at.bluesource.choicesdk.location.factory.FusedLocationProviderFactory
import at.bluesource.choicesdk.maps.common.LatLng
import com.google.accompanist.permissions.*
import com.queatz.ailaai.R
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.toLatLng
import com.queatz.ailaai.ui.dialogs.SetLocationDialog
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

val geoKey = stringPreferencesKey("geo")
val geoManualKey = booleanPreferencesKey("geo-manual")

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun locationSelector(
    geo: LatLng?,
    onGeoChange: (LatLng?) -> Unit,
    activity: Activity
): LocationSelector {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationClient = FusedLocationProviderFactory.getFusedLocationProviderClient(activity)
    var geoManual by rememberStateOf(false)
    var showSetMyLocation by rememberStateOf(false)
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val coarseLocationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)
    var disposable: Disposable? = null

    fun start() {
        disposable = locationClient.observeLocation(LocationRequest.createDefault())
            .filter { it is Outcome.Success && it.value.lastLocation != null }
            .takeWhile { scope.isActive }
            .take(1)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                geoManual = false
                onGeoChange((it as Outcome.Success).value.lastLocation!!.toLatLng())
            }
    }

    DisposableEffect(Unit) {
        onDispose { disposable?.dispose() }
    }

    LaunchedEffect(Unit) {
        geoManual = (!locationPermissionState.status.isGranted && !coarseLocationPermissionState.status.isGranted) || context.dataStore.data.first()[geoManualKey] == true
    }

    LaunchedEffect(geoManual) {
        context.dataStore.edit {
            if (geoManual) {
                it[geoManualKey] = true
            } else {
                it.remove(geoManualKey)
            }
        }
    }

    LaunchedEffect(geo) {
        if (geo == null) {
            val savedGeo = context.dataStore.data.first()[geoKey]?.split(",")?.map { it.toDouble() }
            if (savedGeo != null) {
                onGeoChange(LatLng.getFactory().create(savedGeo[0], savedGeo[1]))
            }
        } else {
            context.dataStore.edit {
                it[geoKey] = "${geo.latitude},${geo.longitude}"
            }
        }
    }

    if (showSetMyLocation) {
        SetLocationDialog({ showSetMyLocation = false }, stringResource(R.string.set_my_location)) {
            geoManual = true
            onGeoChange(it)
        }
    }

    return LocationSelector(
        isManualCallback = { geoManual },
        setLocationManuallyCallback = { showSetMyLocation = true },
        startCallback = { start() },
        permissionGrantedCallback = { locationPermissionState.status.isGranted || coarseLocationPermissionState.status.isGranted },
        shouldShowPermissionRationaleCallback = { locationPermissionState.status.shouldShowRationale && coarseLocationPermissionState.status.shouldShowRationale },
        launchPermissionRequestCallback = { locationPermissionState.launchPermissionRequest() },
        resetRequest = {
            scope.launch {
                context.dataStore.edit {
                    it.remove(geoKey)
                    it.remove(geoManualKey)
                }
                onGeoChange(null)
            }
        }
    )
}

class LocationSelector(
    private val isManualCallback: () -> Boolean,
    private val setLocationManuallyCallback: () -> Unit,
    private val startCallback: () -> Unit,
    private val permissionGrantedCallback: () -> Boolean,
    private val shouldShowPermissionRationaleCallback: () -> Boolean,
    private val launchPermissionRequestCallback: () -> Unit,
    private val resetRequest: () -> Unit,
) {
    fun setLocationManually() {
        setLocationManuallyCallback()
    }

    fun start() {
        startCallback()
    }

    fun launchPermissionRequest() {
        launchPermissionRequestCallback()
    }

    fun reset() {
        resetRequest()
    }

    val isGranted get() = permissionGrantedCallback()
    val shouldShowPermissionRationale get() = shouldShowPermissionRationaleCallback()
    val isManual get() = isManualCallback()
}
