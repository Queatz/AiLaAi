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
import app.ailaai.api.downgradeCard
import app.ailaai.api.downgradeCardDetails
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
import com.queatz.db.CardDowngradeBody
import com.queatz.db.CardDowngradeDetails
import kotlinx.coroutines.launch

@Composable
fun CardDowngradeDialog(
    onDismissRequest: () -> Unit,
    cardId: String,
    currentLevel: Int,
    onConfirm: () -> Unit
) {
    var details by rememberStateOf<CardDowngradeDetails?>(null)
    var isLoading by rememberStateOf(false)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        api.downgradeCardDetails(cardId) {
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
                        if (currentLevel > 0) pluralStringResource(R.plurals.level_x, currentLevel, currentLevel) else stringResource(R.string.downgrade),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(bottom = 1.pad)
                    )
                }

                when {
                    details == null -> {
                        Loading()
                    }
                    else -> {
                        val pointsString = pluralStringResource(R.plurals.x_points, details!!.points, details!!.points.format())
                        val levelString = pluralStringResource(R.plurals.level_x, details!!.level, details!!.level.format())
                        Text(
                            buildAnnotatedString {
                                val formattedString = stringResource(R.string.downgrade_page_to_level_recover_points, levelString, pointsString)
                                val levelIndex = formattedString.indexOf(levelString)
                                val pointsIndex = formattedString.indexOf(pointsString)

                                if (levelIndex >= 0 && pointsIndex >= 0) {
                                    append(formattedString.substring(0, levelIndex))
                                    bold {
                                        append(levelString)
                                    }
                                    append(formattedString.substring(levelIndex + levelString.length, pointsIndex))
                                    bold {
                                        append(pointsString)
                                    }
                                    append(formattedString.substring(pointsIndex + pointsString.length))
                                } else {
                                    append(formattedString)
                                }
                            }
                        )
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
                Button(
                    {
                        scope.launch {
                            isLoading = true
                            api.downgradeCard(cardId, CardDowngradeBody(currentLevel - 1)) {
                                onDismissRequest()
                                onConfirm()
                            }
                            isLoading = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.downgrade))
                }
            }
        )
    }
}
