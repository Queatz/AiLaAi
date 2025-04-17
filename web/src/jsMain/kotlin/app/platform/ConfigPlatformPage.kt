package app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.platform
import app.ailaai.api.profile
import app.components.Empty
import app.dialog.inputDialog
import appString
import appText
import com.queatz.db.PersonProfile
import com.queatz.db.PlatformConfig
import components.Loading
import components.Switch
import kotlinx.browser.window
import kotlinx.coroutines.launch
import notEmpty
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun ConfigPlatformPage() {
    val scope = rememberCoroutineScope()
    var platformConfig by remember {
        mutableStateOf<PlatformConfig?>(null)
    }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        api.platform {
            platformConfig = it
        }
        isLoading = false
    }

    suspend fun update(config: PlatformConfig) {
        api.platform(config) {
            platformConfig = it
        }
    }

    fun addPlatformHost() {
        scope.launch {
            val personId = inputDialog(
                // todo: translate
                "Profile ID"
            )

            if (personId != null) {
                update(PlatformConfig(hosts = platformConfig!!.hosts!! + personId))
            }
        }
    }

    if (isLoading) {
        Loading()
    } else {
        Div({
            style {
                fontSize(18.px)
                margin(1.r)
            }
        }) {
            Div(
                {
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        marginBottom(1.r)
                    }
                }
            ) {
                // todo: translate
                Span({ style { color(Color.gray) } }) {
                    Text("Invite only")
                }
                Switch(
                    platformConfig?.inviteOnly ?: false,
                            onValue = {},
                    onChange = {
                        scope.launch {
                            update(PlatformConfig(inviteOnly = it))
                        }
                    },
                    styles = {
                        marginLeft(1.r)
                    }
                )
            }
            Div {
                Div(
                    {
                        style {
                            color(Color.gray)
                        }
                    }
                ) {
                    // todo: translate
                    Text("Platform hosts")
                }
            }

            platformConfig?.hosts?.notEmpty?.forEach { id ->
                key(id) {
                    var profile by remember { mutableStateOf<PersonProfile?>(null) }

                    LaunchedEffect(id) {
                        api.profile(id) {
                            profile = it
                        }
                    }

                    Div({
                        style {
                            padding(1.r, 0.r)
                            cursor("pointer")
                        }

                        // todo: translate
                        title("Go to profile")

                        onClick {
                            window.open("/profile/$id", target = "_blank")
                        }
                    }) {
                        Text(profile?.let { "${it.person.name ?: appString { someone }} ($id)" } ?: id)
                    }
                }
            } ?: run {
                Div({
                    style {
                        padding(1.r, 0.r)
                    }
                }) {
                    appText { none }
                }
            }

            Button(
                {
                    classes(Styles.outlineButton)

                    onClick {
                        addPlatformHost()
                    }
                }
            ) {
                // todo: translate
                Text("Add a platform host")
            }
        }
    }
}
