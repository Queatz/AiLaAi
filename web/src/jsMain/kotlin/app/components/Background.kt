package app.components

import EffectStyles
import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import application
import com.queatz.db.RainEffect
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.s
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.Image
import kotlin.random.Random

@Composable
fun Background(attrs: AttrsScope<HTMLDivElement>.() -> Unit = {}, content: @Composable () -> Unit) {
    val background by application.background.collectAsState(null)
    val effects by application.effects.collectAsState(null)
    var loaded by remember { mutableStateOf(false) }
    var preload by remember { mutableStateOf<Image?>(null) }

    LaunchedEffect(background?.first) {
        loaded = false
        background?.first?.let { backgroundUrl ->
            preload = Image().apply {
                addEventListener("load", {
                    loaded = true
                    preload = null
                })
                src = backgroundUrl
            }
        }
    }

    if (!effects.isNullOrEmpty()) {
        effects?.forEach {
            key(it) {
                when (it) {
                    is RainEffect -> {
                        Div({
                            classes(EffectStyles.container)
                        }) {
                            (0..it.amount.times(100.0).toInt()).forEach {
                                key(it) {
                                    Div({
                                        classes(EffectStyles.drop)

                                        style {
                                            left((Random.nextFloat() * 100f).vw)
                                            property("animation-delay", (Random.nextFloat() * 5f).s)
                                            property("animation-duration", (.25f + Random.nextFloat() * .5f).s)
                                        }
                                    }) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Div(attrs) {
        Div({
            if (loaded) {
                classes(Styles.backgroundPhoto, Styles.backgroundPhotoLoaded)
            } else {
                classes(Styles.backgroundPhoto)
            }

            style {
                if (background != null && loaded) {
                    backgroundImage("url(${background!!.first})")
                    opacity(background!!.second)
                }
            }
        }) {

        }
        content()
    }
}
