package com.queatz.ailaai.ui.dialogs

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.compose.ui.window.Dialog
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import at.bluesource.choicesdk.maps.common.*
import at.bluesource.choicesdk.maps.common.Map
import at.bluesource.choicesdk.maps.common.listener.OnMarkerDragListener
import at.bluesource.choicesdk.maps.common.options.MarkerOptions
import com.queatz.ailaai.R
import com.queatz.ailaai.databinding.LayoutMapBinding
import com.queatz.ailaai.ui.theme.PaddingDefault


@SuppressLint("MissingPermission")
@Composable
fun SetLocationDialog(onDismissRequest: () -> Unit, onLocation: (LatLng) -> Unit) {
    var position by remember { mutableStateOf(LatLng(0.0, 0.0)) }

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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(PaddingValues(vertical = PaddingDefault * 2))
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
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
                                            .setZoom(5f)
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
                                        .setZoom(5f)
                                        .build()
                                )
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        {
                            onLocation(position)
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.set_my_location))
                    }
                }
            }
        }
    }
}
