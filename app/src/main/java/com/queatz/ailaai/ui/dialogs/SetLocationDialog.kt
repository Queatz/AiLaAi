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
import com.queatz.ailaai.ui.components.MapWithMarker
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch


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
                    MapWithMarker(5f, position) {
                        position = it
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
