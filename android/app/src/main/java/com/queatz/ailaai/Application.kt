package com.queatz.ailaai

import android.content.Context
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import app.ailaai.api.crash
import app.ailaai.api.myDevice
import at.bluesource.choicesdk.core.ChoiceSdk
import at.bluesource.choicesdk.messaging.common.RemoteMessage
import at.bluesource.choicesdk.messaging.factory.MessagingRepositoryFactory
import com.google.auto.service.AutoService
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.maps.MapClientIdentify
import com.huawei.hms.maps.MapsInitializer
import com.queatz.ailaai.data.api
import com.queatz.ailaai.db.db
import com.queatz.ailaai.services.calls
import com.queatz.ailaai.services.connectivity
import com.queatz.ailaai.services.push
import com.queatz.ailaai.services.ui
import com.queatz.ailaai.slideshow.slideshow
import com.queatz.db.DeviceType
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observers.DisposableObserver
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

class Application : android.app.Application() {

    private var disposable = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()

        try {
            VideoSDK.initialize(applicationContext)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        ChoiceSdk.init(this)
        db.init(this)
        api.init(this)
        push.init(this)
        ui.init(this)
        calls.init(this)
        slideshow.init(this)
        connectivity.start(this)

        val scope = CoroutineScope(Dispatchers.IO)

        val deviceType = when (HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(this) == ConnectionResult.SUCCESS) {
            true -> DeviceType.Hms
            else -> DeviceType.Gms
        }

        // todo this should be in a new version of ChoiceSDK
        if (deviceType == DeviceType.Hms) {
            MapsInitializer.initialize(this)
        }

        val tokenObserver = object : DisposableObserver<String>() {
            override fun onNext(token: String) {
                if (token.isBlank()) {
                    return
                }

                scope.launch {
                    api.myDevice(deviceType, token, onError = {})
                }
            }
            override fun onError(throwable: Throwable) {
                throwable.printStackTrace()
            }
            override fun onComplete() {}
        }

        MessagingRepositoryFactory.getMessagingService()
            .getNewTokenObservable()
            .subscribeWith(tokenObserver)
            .let(disposable::add)

        MessagingRepositoryFactory.getMessagingService().requestToken(this)

        val messageObserver: DisposableObserver<RemoteMessage> = object : DisposableObserver<RemoteMessage>() {
            override fun onNext(remoteMessage: RemoteMessage) {
                push.got(remoteMessage.data)
            }

            override fun onError(throwable: Throwable) {
                throwable.printStackTrace()
            }
            override fun onComplete() {}
        }

        MessagingRepositoryFactory.getMessagingService()
            .getMessageReceivedObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(messageObserver)
            .let(disposable::add)
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
        disposable.dispose()
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
