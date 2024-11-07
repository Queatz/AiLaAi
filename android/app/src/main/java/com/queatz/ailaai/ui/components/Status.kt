package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.ui.screens.OutlinedText
import com.queatz.ailaai.ui.theme.pad
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

@Composable
fun Status(
    text: String? = null,
    photo: String? = null,
    color: Color? = null,
    seen: Instant? = null,
    block: @Composable () -> Unit,
) {
    Box {
        block()

        Box(
            modifier = Modifier.matchParentSize()
        ) {
            if (!photo.isNullOrBlank() || !text.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .offset(y = -1.pad)
                        .align(Alignment.TopCenter)
                        .shadow(3.dp, MaterialTheme.shapes.large)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .padding(vertical = .25f.pad, horizontal = .5f.pad)
                        .zIndex(1f)
                ) {
                    photo?.notBlank?.let { photo ->
                        AsyncImage(
                            model = photo.let(api::url),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(0.25f.pad)
                                .requiredSize(16.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        )
                    }
                    text?.notBlank?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            color?.let { color ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .shadow(3.dp, CircleShape)
                        .background(color, CircleShape)
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
                if (seen?.let { it < Clock.System.now() - 30.minutes } == true) {
                    OutlinedText(
                        stringResource(R.string.z),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(.25f.pad)
                            .offset(3.dp, -1.dp)
                            .zIndex(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
