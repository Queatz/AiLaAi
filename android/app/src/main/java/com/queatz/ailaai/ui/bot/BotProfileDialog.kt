package com.queatz.ailaai.ui.bot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.bot
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Bot

@Composable
fun BotProfileDialog(onDismissRequest: () -> Unit, botId: String) {
    DialogBase(onDismissRequest) {
        DialogLayout(
            content = {
                var isLoading by rememberStateOf(true)
                var bot by rememberStateOf<Bot?>(null)

                LaunchedEffect(botId) {
                    api.bot(botId) {
                        bot = it
                    }
                    isLoading = false
                }

                if (isLoading) {
                    Loading()
                } else {
                    bot?.let { bot ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(bottom = 1.pad)
                                    .weight(1f)
                            ) {
                                Text(
                                    bot.name ?: stringResource(R.string.new_bot),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                        BotProfile(bot)
                    } ?: run {
                        EmptyText(stringResource(R.string.bot_not_found))
                    }
                }
            },
            actions = {
                TextButton(onDismissRequest) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
