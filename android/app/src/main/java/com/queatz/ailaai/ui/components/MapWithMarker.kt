package com.queatz.ailaai.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.queatz.ailaai.databinding.LayoutMapBinding
import com.queatz.ailaai.extensions.rememberStateOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapWithMarker(
    zoomLevel: Float,
    position: LatLng,
    modifier: Modifier = Modifier,
    positionChange: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val coarseLocationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

    var composed by rememberStateOf(false)
    var marker: Marker? by remember { mutableStateOf(null) }
    var map: GoogleMap? by remember { mutableStateOf(null) }
    val recenter = remember { MutableSharedFlow<LatLng>() }

    LaunchedEffect(Unit) {
        recenter.collect {
            map?.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(it)
                        .zoom(zoomLevel)
                        .build()
                )
            )
        }
    }

    LaunchedEffect(map, position) {
        marker?.position = position
        recenter.emit(position)
    }

    AndroidViewBinding(
        LayoutMapBinding::inflate,
        modifier = modifier
            .fillMaxSize()
    ) {
        if (composed) return@AndroidViewBinding else composed = true
        mapFragmentContainerView.doOnAttach { it.doOnDetach { mapFragmentContainerView.removeAllViews() } }

        val mapFragment = mapFragmentContainerView.getFragment<SupportMapFragment>()

        mapFragment.getMapAsync {
            map = it.apply {
                clear()
                uiSettings.apply {
                    isMapToolbarEnabled = true
                    isCompassEnabled = true
                    isMyLocationButtonEnabled = true
                }

                val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if ((locationPermissionState.status == PermissionStatus.Granted || coarseLocationPermissionState.status == PermissionStatus.Granted) && (hasFineLocation || hasCoarseLocation)) {
                    try {
                        isMyLocationEnabled = true
                    } catch (_: SecurityException) {
                    }
                }
            }

            marker = map!!.addMarker(
                MarkerOptions()
                    .position(position)
                    .draggable(true)
            )!!

            map?.setOnMapClickListener {
                positionChange(it)
            }

            map?.setOnMarkerClickListener {
                coroutineScope.launch {
                    recenter.emit(position)
                }
                true
            }
            map?.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker) {}

                override fun onMarkerDragEnd(marker: Marker) {
                    positionChange(marker.position)
                }

                override fun onMarkerDragStart(marker: Marker) {}
            })

            map?.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(position)
                        .zoom(zoomLevel)
                        .build()
                )
            )
        }
    }
}
