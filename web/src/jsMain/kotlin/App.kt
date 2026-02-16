import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import app.NavPage
import app.ailaai.api.me
import app.messaages.inList
import com.queatz.db.Effect
import com.queatz.db.Person
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import lib.enUS
import lib.ru
import lib.vi
import org.w3c.dom.get
import org.w3c.dom.set

val application by lazy { Application() }

enum class AppLayout {
    Default,
    Kiosk
}

class Application {
    private val _effects = MutableStateFlow<List<List<Effect>>>(emptyList())
    private val _background = MutableStateFlow<List<Pair<String, Float>>>(emptyList())

    val me = MutableStateFlow<Person?>(null)
    val layout = MutableStateFlow(localStorage["layout"]?.let { AppLayout.valueOf(it) } ?: AppLayout.Default)
    val effects = _effects.map { it.lastOrNull() }
    val background = _background.map { it.lastOrNull() }
    val bearerToken = MutableStateFlow<String?>(null)

    var navPage: NavPage = NavPage.Groups
        private set

    var language: String = "en"

    val locale get() = when (language) {
        "vi" -> vi
        "ru" -> ru
        else -> enUS
    }

    init {
        bearerToken.value = localStorage["bearer"]

        val meJson = localStorage["me"]
        if (meJson != null) {
            try {
                me.value = json.decodeFromString<Person>(meJson)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        val navPageJson = localStorage["app.nav"]
        if (navPageJson != null) {
            try {
                navPage = json.decodeFromString<NavPage>(navPageJson)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun appString(block: Strings.() -> Translation) = getString(block(Strings), language)

    fun setNavPage(navPage: NavPage?) {
        this.navPage = navPage ?: NavPage.Groups
        if (navPage != null) {
            localStorage["app.nav"] = json.encodeToString(navPage)
        } else {
            localStorage.removeItem("app.nav")
        }
    }

    fun toggleLayout() {
        layout.value = when(layout.value) {
            AppLayout.Default -> AppLayout.Kiosk
            else -> AppLayout.Default
        }

        localStorage["layout"] = layout.value.name
    }

    fun setMe(me: Person?) {
        this.me.value = me
        if (me != null) {
            localStorage["me"] = json.encodeToString(me)
        } else {
            localStorage.removeItem("me")
        }
    }

    fun setToken(token: String?) {
        this.bearerToken.value = token
        if (token != null) {
            localStorage["bearer"] = token
        } else {
            localStorage.removeItem("bearer")
        }
    }

    fun signOut() {
        setToken(null)
        setMe(null)
        setNavPage(null)
    }

    suspend fun sync() {
        if (me.value != null && bearerToken.value != null) {
            api.me {
                setMe(it)
            }
        }
    }

    @Composable
    fun background(url: String?, alpha: Float = 1f) {
        if (url != null) {
            val value = url to alpha
            DisposableEffect(url, alpha) {
                _background.update {
                    it + value
                }

                onDispose {
                    _background.update {
                        it - value
                    }
                }
            }
        }
    }

    @Composable
    fun effects(effects: List<Effect>?) {
        if (!effects.isNullOrEmpty()) {
            val value = effects.inList()
            DisposableEffect(effects) {
                _effects.update {
                    it + value
                }

                onDispose {
                    _effects.update {
                        it - value
                    }
                }
            }
        }
    }
}
