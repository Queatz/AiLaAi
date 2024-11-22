package app.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import application
import com.queatz.db.Widget
import com.queatz.widgets.widgets.FormData
import json
import kotlinx.serialization.SerializationException
import widget

@Composable
fun FormWidget(widgetId: String) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()

    var widget by remember(widgetId) {
        mutableStateOf<Widget?>(null)
    }

    var data by remember {
        mutableStateOf<FormData?>(null)
    }

    LaunchedEffect(widgetId) {
        // todo loading
        api.widget(widgetId) {
            it.data ?: return@widget
            widget = it
            data = try {
                json.decodeFromString<FormData>(it.data!!)
            } catch (e: SerializationException) {
                e.printStackTrace()
                null
            }
        }
    }

    data?.let { data ->
        // todo render form
    }
}
