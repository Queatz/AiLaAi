package com.queatz.ailaai

import android.content.Context
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import app.ailaai.api.crash
import app.ailaai.api.myDevice
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.google.auto.service.AutoService
import com.google.firebase.messaging.FirebaseMessaging
import com.queatz.ailaai.cache.buildAppImageLoader
import com.queatz.ailaai.cache.groupMessageCache
import com.queatz.ailaai.data.api
import com.queatz.ailaai.db.db
import com.queatz.ailaai.services.calls
import com.queatz.ailaai.services.connectivity
import com.queatz.ailaai.services.push
import com.queatz.ailaai.services.ui
import com.queatz.ailaai.slideshow.slideshow
import com.queatz.db.DeviceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.videosdk.rtc.android.VideoSDK
import org.acra.config.CoreConfiguration
import org.acra.config.toast
import org.acra.data.CrashReportData
import org.acra.ktx.initAcra
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory
import java.util.Locale


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class Application : android.app.Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        if (!groupMessageCache.isInitialized()) {
            groupMessageCache.init(this)
        }
        return buildAppImageLoader(
            context = this,
            mode = groupMessageCache.mode.value,
            sizeLimit = groupMessageCache.sizeLimit.value,
        )
    }

    override fun onCreate() {
        super.onCreate()

        try {
            VideoSDK.initialize(applicationContext)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        db.init(this)
        api.init(this)
        groupMessageCache.init(this)
        push.init(this)
        ui.init(this)
        calls.init(this)
        slideshow.init(this)
        connectivity.start(this)
        com.queatz.ailaai.services.chips.init(this)

        val scope = CoroutineScope(Dispatchers.IO)

        @Suppress("DEPRECATION")
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNotBlank()) {
                    scope.launch {
                        api.myDevice(DeviceType.Gms, token, onError = {})
                    }
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }


    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        //ACRA.DEV_LOGGING = true
        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = org.acra.data.StringFormat.JSON
            toast {
                text = getString(R.string.app_crashed)
            }
        }
    }

    override fun onTerminate() {
        connectivity.stop()
        super.onTerminate()
    }
}

val appLanguage get() = AppCompatDelegate.getApplicationLocales().get(0)?.language

class YourOwnSender : ReportSender {

    val scope = CoroutineScope(Dispatchers.IO)

    override fun send(context: Context, errorContent: CrashReportData) {
        if (!BuildConfig.DEBUG) {
            scope.launch {
                api.crash(errorContent.toJSON())
            }
        }
    }
}

@AutoService(ReportSenderFactory::class)
@Keep
@Suppress("UNUSED")
class CrashSenderFactory : ReportSenderFactory {

    override fun create(context: Context, config: CoreConfiguration) : ReportSender {
        return YourOwnSender()
    }

    //optional implementation in case you want to disable your sender in certain cases
    override fun enabled(config : CoreConfiguration) : Boolean {
        return true
    }
}

fun Context.setAppLocale(locale: Locale): Context {
    val config = resources.configuration
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return createConfigurationContext(config)
}
