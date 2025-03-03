package com.queatz.ailaai.ui.bot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Bot

@Composable
fun BotProfile(bot: Bot) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.pad)
    ) {
        bot.photo?.notBlank?.let { photo ->
            AsyncImage(
                model = api.url(photo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )
        }
        bot.description?.let {
            Text(
                it,
                textAlign = TextAlign.Center
            )
        }
    }
}
