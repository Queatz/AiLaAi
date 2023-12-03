import app.NavPage
import app.ailaai.api.me
import com.queatz.db.Person
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import org.w3c.dom.get
import org.w3c.dom.set

val application = Application()

class Application {
    val me = MutableStateFlow<Person?>(null)
    val bearerToken = MutableStateFlow<String?>(null)

    var navPage: NavPage = NavPage.Groups
        private set

    var language: String = "en"

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
}
