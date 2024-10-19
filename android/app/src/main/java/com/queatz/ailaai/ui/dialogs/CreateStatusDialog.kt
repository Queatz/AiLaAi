package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.ailaai.api.createStatus
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.hex
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.SearchField
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Status
import kotlinx.coroutines.launch
import java.util.logging.Logger

@Composable
fun CreateStatusDialog(
    onDismissRequest: () -> Unit,
    initialColor: Color = Color.White,
    onCreate: (Status) -> Unit
) {
    val scope = rememberCoroutineScope()
    val controller = rememberColorPickerController()
    var color by rememberStateOf(initialColor)
    var name by rememberStateOf("")
    var isSaving by rememberStateOf(false)

    DialogBase(onDismissRequest) {
        DialogLayout(
            scrollable = true,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                Row(
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(.25f.pad)
                            .size(12.dp)
                            .shadow(3.dp, CircleShape)
                            .clip(CircleShape)
                            .background(color)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = .5f),
                                        Color.White.copy(alpha = 0f)
                                    ),
                                    center = Offset(
                                        4.5f.dp.px.toFloat(),
                                        4.5f.dp.px.toFloat()
                                    ),
                                    radius = 9.dp.px.toFloat()
                                ),
                                shape = CircleShape
                            )
                            .zIndex(1f)
                    )
                    SearchField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = stringResource(R.string.status),
                        imeAction = ImeAction.Default,
                        showClear = false,
                        autoFocus = true,
                        useMaxWidth = false
                    )
                }
                Spacer(Modifier.height(1.pad))
                HsvColorPicker(
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(10.dp),
                    controller = controller,
                    onColorChanged = {
                        color = it.color
                    },
                    initialColor = initialColor
                )
                BrightnessSlider(
                    controller = controller,
                    initialColor = initialColor,
                    borderRadius = 24.dp,
                    borderSize = 2.dp,
                    wheelRadius = 9.dp,
                    borderColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(24.dp).widthIn(max = 240.dp)
                )
            }
        ) {
            TextButton(onDismissRequest) {
                Text(stringResource(R.string.close))
            }
            TextButton(
                onClick = {
                    isSaving = true
                    scope.launch {
                        api.createStatus(
                            Status(
                                name = name,
                                color = color.hex
                            )
                        ) {
                            onCreate(it)
                        }
                        isSaving = false
                    }
                },
                enabled = name.isNotBlank() && !isSaving
            ) {
                Text(stringResource(R.string.choose))
            }
        }
    }
}
