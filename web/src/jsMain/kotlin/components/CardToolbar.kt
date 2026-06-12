package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import application
import api
import app.ailaai.api.saveCard
import app.ailaai.api.unsaveCard
import app.compose.rememberDarkMode
import app.dialog.dialog
import appString
import com.queatz.db.Card
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun CardToolbar(card: Card, styles: (StyleScope.() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    val darkMode = rememberDarkMode()
    val me by application.me.collectAsState()

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Row)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.FlexStart)
            flexWrap(FlexWrap.Wrap)
            gap(.5.r)
            styles?.invoke(this)
        }
    }) {
        // Directions
        card.geo?.let { geo ->
            if (geo.size >= 2) {
                val lat = geo[0]
                val lng = geo[1]
                val directionsString = appString { directions }
                IconButton(
                    name = "directions",
                    text = directionsString,
                    title = directionsString,
                    background = true,
                    backgroundColor = if (darkMode) Styles.colors.dark.surface else Styles.colors.secondary,
                    onClick = {
                        val name = card.name ?: ""
                        val url = "https://www.google.com/maps?q=$lat,$lng($name)"
                        window.open(url, "_blank")
                    }
                )
            }
        }

        // Share
        val shareString = appString { share }
        val copyLinkString = appString { copyLink }
        val closeString = appString { close }
        IconButton(
            name = "share",
            text = shareString,
            title = shareString,
            background = true,
            backgroundColor = if (darkMode) Styles.colors.dark.surface else Styles.colors.secondary,
            onClick = {
                scope.launch {
                    val url = "${window.location.origin}/page/${card.id ?: card.url ?: ""}"
                    dialog(
                        title = shareString,
                        confirmButton = copyLinkString,
                        cancelButton = closeString
                    ) { _ ->
                        Div({
                            style {
                                property("word-break", "break-all")
                            }
                        }) {
                            Text(url)
                        }
                    }.let { confirmed ->
                        if (confirmed == true) {
                            window.navigator.clipboard.writeText(url)
                        }
                    }
                }
            }
        )

        // Save
        if (me != null) {
            var isSaving by remember { mutableStateOf(false) }
            var isSaved by remember(card.saved) { mutableStateOf(card.saved == true) }
            val savedString = appString { saved }
            val saveString = appString { save }
            IconButton(
                name = "bookmark",
                text = if (isSaved) savedString else saveString,
                title = if (isSaved) savedString else saveString,
                background = true,
                backgroundColor = if (darkMode) Styles.colors.dark.surface else Styles.colors.secondary,
                isLoading = isSaving,
                iconStyles = {
                    if (isSaved) {
                        property("font-variation-settings", "'FILL' 1")
                    }
                },
                onClick = {
                    scope.launch {
                        isSaving = true
                        val success = if (isSaved) {
                            var ok = false
                            api.unsaveCard(card.id!!) { ok = true }
                            ok
                        } else {
                            var ok = false
                            api.saveCard(card.id!!) { ok = true }
                            ok
                        }
                        if (success) {
                            isSaved = !isSaved
                            card.saved = isSaved
                        }
                        isSaving = false
                    }
                }
            )
        }
    }
}
