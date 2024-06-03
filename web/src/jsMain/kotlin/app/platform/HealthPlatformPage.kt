package app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.stats
import app.ailaai.api.statsHealth
import com.queatz.db.AppStats
import com.queatz.db.StatsHealth
import components.Loading
import json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun HealthPlatformPage() {
    var stats by remember {
        mutableStateOf<StatsHealth?>(null)
    }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        api.statsHealth {
            stats = it
        }
        isLoading = false
    }

    if (isLoading) {
        Loading()
    } else if (stats != null) {
        Div({
            style {
                fontSize(18.px)
                margin(1.r)
            }
        }) {
            val stats = remember {
                json.encodeToJsonElement(stats).jsonObject.entries
            }

            stats.forEach {
                Div {
                    Span({ style { color(Color.gray) } }) { Text(it.key) }
                    Text(" ")
                    Text(it.value.jsonPrimitive.content)
                }
            }
        }
    }
}
