package com.queatz.ailaai.ui.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import app.ailaai.api.upgradeCard
import app.ailaai.api.upgradeCardDetails
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.bold
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.CardUpgradeBody
import com.queatz.db.CardUpgradeDetails
import kotlinx.coroutines.launch

@Composable
fun CardUpgradeDialog(
    onDismissRequest: () -> Unit,
    cardId: String,
    currentLevel: Int,
    onConfirm: () -> Unit
) {
    var details by rememberStateOf<CardUpgradeDetails?>(null)
    var isLoading by rememberStateOf(false)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        api.upgradeCardDetails(cardId) {
            details = it
        }
    }

    DialogBase(
        onDismissRequest
    ) {
        DialogLayout(
            content = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        if (currentLevel > 0) pluralStringResource(R.plurals.level_x, currentLevel, currentLevel) else stringResource(R.string.upgrade),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(bottom = 1.pad)
                    )
                }

                when {
                    details == null -> {
                        Loading()
                    }
                    !details!!.available -> {
                        val pointsString = pluralStringResource(R.plurals.x_points, details!!.points, details!!.points.format())
                        val levelString = pluralStringResource(R.plurals.level_x, details!!.level, details!!.level.format())
                        Text(
                            // todo: translate
                            buildAnnotatedString {
                                append("Upgrading this page to ")
                                bold {
                                    append(levelString)
                                }

                                append(" requires ")

                                bold {
                                    append(pointsString)
                                }

                                append(".")
                            }
                        )
                    }
                    details!!.available -> {
                        val pointsString = pluralStringResource(R.plurals.x_points, details!!.points, details!!.points.format())
                        val levelString = pluralStringResource(R.plurals.level_x, details!!.level, details!!.level.format())
                        Text(
                            // todo: translate
                            buildAnnotatedString {
                                append("Upgrade this page to ")
                                bold {
                                    append(levelString)
                                }

                                append(" for ")

                                bold {
                                    append(pointsString)
                                }

                                append("?")
                            }
                        )
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            api.upgradeCard(cardId, CardUpgradeBody(currentLevel + 1)) {
                                onDismissRequest()
                                onConfirm()
                            }
                            isLoading = false
                        }
                    },
                    enabled = details?.available == true
                ) {
                    Text(stringResource(R.string.upgrade))
                }
            }
        )
    }
}
