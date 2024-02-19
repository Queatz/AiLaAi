package com.queatz.ailaai.call

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PresentToAll
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.queatz.ailaai.ui.theme.pad

@Composable
fun CallScreen(
    groupId: String,
    isInPipMode: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier) {

        if (!isInPipMode) {
            Row(
                horizontalArrangement = spacedBy(1.pad),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.pad)
            ) {
                FilledTonalIconButton(
                    {

                    }
                ) {
                    Icon(Icons.Outlined.Mic, null)
                }
                FilledTonalIconButton(
                    {

                    }
                ) {
                    Icon(Icons.Outlined.Videocam, null)
                }
                FilledTonalIconButton(
                    {

                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Outlined.PresentToAll, null)
                }
                FilledTonalIconButton(
                    {

                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Outlined.CallEnd, null)
                }
            }
        }
    }
}
