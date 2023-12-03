package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import at.bluesource.choicesdk.maps.common.*
import at.bluesource.choicesdk.maps.common.Map
import at.bluesource.choicesdk.maps.common.listener.OnMarkerDragListener
import at.bluesource.choicesdk.maps.common.options.MarkerOptions
import com.queatz.ailaai.databinding.LayoutMapBinding
import com.queatz.ailaai.extensions.rememberStateOf
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Composable
fun MapWithMarker(zoomLevel: Float, position: LatLng, modifier: Modifier = Modifier, positionChange: (LatLng) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val disposable = remember { CompositeDisposable() }

    DisposableEffect(Unit) {
        onDispose {
            disposable.dispose()
        }
    }

    var composed by rememberStateOf(false)
    var marker: Marker? by remember { mutableStateOf(null) }
    var map: Map? by remember { mutableStateOf(null) }
    val recenter = remember { MutableSharedFlow<LatLng>() }

    LaunchedEffect(Unit) {
        recenter.collect {
            map?.animateCamera(
                CameraUpdateFactory.get().newCameraPosition(
                    CameraPosition.Builder()
                        .setTarget(it)
                        .setZoom(zoomLevel)
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

        val mapFragment = mapFragmentContainerView.getFragment<MapFragment>()

        mapFragment.getMapObservable().subscribe {
            map = it
            map?.clear()

            map?.getUiSettings()?.isMapToolbarEnabled = true
            map?.getUiSettings()?.isMyLocationButtonEnabled = true

            marker = map!!.addMarker(
                MarkerOptions
                    .create()
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
            map?.setOnMarkerDragListener(object : OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker) {}

                override fun onMarkerDragEnd(marker: Marker) {
                    positionChange(marker.position)
                }

                override fun onMarkerDragStart(marker: Marker) {}
            })

            map?.moveCamera(
                CameraUpdateFactory.get().newCameraPosition(
                    CameraPosition.Builder()
                        .setTarget(position)
                        .setZoom(zoomLevel)
                        .build()
                )
            )
        }.let(disposable::add)
    }
}
