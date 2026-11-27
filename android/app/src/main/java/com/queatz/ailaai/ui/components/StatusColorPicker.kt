package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.hex
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.PersonChip
import com.queatz.db.parseChipHexColor

@Composable
fun StatusColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
) {
    val controller = rememberColorPickerController()

    HsvColorPicker(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(10.dp),
        controller = controller,
        onColorChanged = {
            onColorChanged(it.color)
        },
        initialColor = initialColor,
    )
    BrightnessSlider(
        controller = controller,
        initialColor = initialColor,
        borderRadius = 24.dp,
        borderSize = 2.dp,
        wheelRadius = 9.dp,
        borderColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .height(24.dp)
            .widthIn(max = 240.dp),
    )
}

@Composable
fun ChipColorPickerDialog(
    initialColorHex: String,
    previewChip: PersonChip,
    previewValue: String,
    onDismissRequest: () -> Unit,
    onColorSelected: (String) -> Unit,
) {
    val initialColor = parseChipHexColor(initialColorHex)?.let { Color(it.toArgbInt()) } ?: Color.White
    var color by rememberStateOf(initialColor)

    DialogBase(onDismissRequest = onDismissRequest) {
        DialogLayout(
            scrollable = true,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                val liveChip = PersonChip(
                    name = previewChip.name,
                    url = previewChip.url,
                    format = previewChip.format,
                    prefix = previewChip.prefix,
                    color = color.hex,
                    jsonPath = previewChip.jsonPath,
                    isAbove = previewChip.isAbove,
                    order = previewChip.order,
                )
                ChipItem(
                    chip = liveChip,
                    value = previewValue,
                    onClick = {},
                    onLongClick = {},
                )
                Spacer(Modifier.height(1.pad))
                StatusColorPicker(
                    initialColor = initialColor,
                    onColorChanged = { color = it },
                )
            },
            actions = {
                DialogCloseButton(onDismissRequest)
                TextButton(
                    onClick = {
                        onColorSelected(color.hex)
                    },
                ) {
                    Text(stringResource(R.string.choose))
                }
            },
        )
    }
}
