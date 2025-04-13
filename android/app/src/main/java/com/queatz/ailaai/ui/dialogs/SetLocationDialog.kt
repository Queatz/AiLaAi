package com.queatz.ailaai.ui.dialogs

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.MapWithMarker
import com.queatz.ailaai.ui.theme.pad


@SuppressLint("MissingPermission")
@Composable
fun SetLocationDialog(
    onDismissRequest: () -> Unit,
    confirmButton: String = stringResource(R.string.choose_location),
    initialLocation: LatLng = LatLng(0.0, 0.0),
    initialZoom: Float = 5f,
    title: String? = null,
    actions: @Composable () -> Unit = {},
    onRemoveLocation: (() -> Unit)? = null,
    onLocation: (LatLng) -> Unit,
) {
    var position by remember { mutableStateOf(initialLocation) }

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
        ) {
            title?.let { title ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(bottom = 1.pad)
                        .fillMaxWidth()
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                        modifier = Modifier
                            .weight(1f)
                    )

                    actions()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(PaddingValues(vertical = 2.pad))
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            ) {
                MapWithMarker(initialZoom, position) {
                    position = it
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                onRemoveLocation?.let { onRemoveLocation ->
                    TextButton(
                        onClick = {
                            onDismissRequest()
                        }
                    ) {
                        Text(
                            stringResource(R.string.remove),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                DialogCloseButton(onDismissRequest)
                TextButton(
                    onClick = {
                        onLocation(position)
                        onDismissRequest()
                    }
                ) {
                    Text(confirmButton)
                }
            }
        }
    }
}
