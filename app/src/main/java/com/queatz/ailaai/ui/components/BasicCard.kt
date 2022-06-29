package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicCard(navController: NavHostController, nameAndLocation: Pair<String, String>) {
    val seed = remember { Random.nextInt() }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(.75f)
                .clickable {
                    navController.navigate("messages/${nameAndLocation.first}")
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://random.imagecdn.app/v1/image?width=600&height=1200&category=girl&format=image&seed=$seed")
                    .crossfade(true)
                    .build(),
                contentDescription = "Image",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(alpha = .8f))
                    .padding(PaddingDefault * 2)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            MaterialTheme.typography.titleMedium.toSpanStyle().copy(fontWeight = FontWeight.Bold)
                        ) {
                            append(nameAndLocation.first)
                        }
                        append("  ")
                        withStyle(
                            MaterialTheme.typography.titleSmall.toSpanStyle()
                                .copy(color = MaterialTheme.colorScheme.secondary)
                        ) {
                            append(nameAndLocation.second)
                        }
                        append("\nI'm an app developer from Austin who loves prototyping and brainstorming ideas. Let's chat!")
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PaddingDefault)
                )

                val recomposeScope = currentRecomposeScope

                listOf(
                    "I want to talk about an app idea",
                    "I want to learn programming",
                    "\uD83D\uDCEC Send a message",
                )
                    .sortedBy { Random.nextInt(1, 4) }
                    .take(Random.nextInt(1, 4))
                    .forEach {
                        Button({
                            recomposeScope.invalidate()
                        }) {
                            Text(it, overflow = TextOverflow.Ellipsis, maxLines = 1)
                        }
                    }
//                IconButton({
//                    recomposeScope.invalidate()
//                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
//                    Icon(Icons.Outlined.Refresh, "Refresh")
//                }
            }
        }
    }
}
