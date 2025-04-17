package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.theme.pad

@Composable
fun RateMessageDialog(
    onDismissRequest: () -> Unit,
    initialRating: Int? = null,
    onRemoveRating: () -> Unit,
    onRate: (Int) -> Unit
) {
    var customRating by remember { mutableStateOf(initialRating?.toString() ?: "") }
    val commonRatings = remember { listOf(-2, -1, 0, 1, 2) }

    DialogBase(onDismissRequest) {
        DialogLayout(
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var viewport by remember { mutableStateOf(Size(0f, 0f)) }
                    val scrollState = rememberScrollState()

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.pad),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1.pad)
                            .horizontalScroll(scrollState)
                            .horizontalFadingEdge(viewport, scrollState)
                            .onPlaced { viewport = it.boundsInParent().size }
                    ) {
                        commonRatings.forEach { rating ->
                            val ratingText = if (rating > 0) "+$rating" else rating.toString()
                            OutlinedButton(
                                onClick = {
                                    onRate(rating)
                                    onDismissRequest()
                                }
                            ) {
                                Text(
                                    text = ratingText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customRating,
                        onValueChange = { customRating = it },
                        label = { Text(stringResource(R.string.custom)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        shape = MaterialTheme.shapes.large
                    )
                }
            },
            actions = {
                if (initialRating != null) {
                    TextButton(
                        onClick = {
                            onRemoveRating()
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.remove_rating))
                    }
                }

                DialogCloseButton(onClick = onDismissRequest)

                TextButton(
                    onClick = {
                        customRating.toIntOrNull()?.let {
                            onRate(it)
                        }
                        onDismissRequest()
                    },
                    enabled = customRating.toIntOrNull() != null
                ) {
                    Text(stringResource(R.string.rate))
                }
            }
        )
    }
}
