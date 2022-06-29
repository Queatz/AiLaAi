package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlin.random.Random

@Composable
fun MessageItem() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val isMe = Random.nextBoolean()

        if (!isMe) ProfileImage(PaddingValues(PaddingDefault, PaddingDefault, 0.dp, PaddingDefault))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Message ".repeat(Random.nextInt(1, 20)),
                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(if (isMe) Alignment.End else Alignment.Start)
                    .padding(PaddingDefault)
                    .clip(MaterialTheme.shapes.large)
                    .background(if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
                    .border(if (isMe) 0.dp else 1.dp, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.large)
                    .clickable {
                        // todo message toolbar
                    }
                    .padding(PaddingDefault * 2, PaddingDefault)
                    .fillMaxWidth(.75f)
            )
        }
    }
}

@Composable
fun ProfileImage(padding: PaddingValues) {
    AsyncImage(
        model = "https://minimaltoolkit.com/images/randomdata/${
            listOf("female", "male").random()
        }/${Random.nextInt(1, 100)}.jpg",
        "",
        Modifier
            .padding(padding)
            .requiredSize(32.dp)
            .clip(CircleShape)
    )
}
