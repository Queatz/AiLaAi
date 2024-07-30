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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.bluesource.choicesdk.maps.common.*
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
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
                TextButton(
                    {
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

