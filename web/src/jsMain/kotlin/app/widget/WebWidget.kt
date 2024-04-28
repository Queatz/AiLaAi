package app.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import com.queatz.db.Widget
import com.queatz.widgets.widgets.WebData
import json
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Iframe
import widget

@Composable
fun WebWidget(widgetId: String) {
    var widget by remember(widgetId) {
        mutableStateOf<Widget?>(null)
    }

    var data by remember(widgetId) {
        mutableStateOf<WebData?>(null)
    }

    LaunchedEffect(widgetId) {
        // todo loading
        api.widget(widgetId) {
            it.data ?: return@widget
            widget = it
            data = json.decodeFromString<WebData>(it.data!!)
        }
    }

    data?.url?.let { url ->
        Iframe(
            {
                style {
                    property("border", "none")
                    width(100.percent)
                    height(394.px)
                }
                attr("allowfullscreen", "true")
                attr("allow", "autoplay; fullscreen")
                attr("src", "https://musescore.com/user/4609986/scores/1749181/embed")
            }
        )
    }
}
