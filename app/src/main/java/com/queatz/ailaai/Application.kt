package com.queatz.ailaai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import at.bluesource.choicesdk.core.ChoiceSdk
import at.bluesource.choicesdk.messaging.common.RemoteMessage
import at.bluesource.choicesdk.messaging.factory.MessagingRepositoryFactory
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.observers.DisposableObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        ChoiceSdk.init(this)
        api.init(this)
        push.init(this)
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        val deviceType = when (HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(this) == ConnectionResult.SUCCESS) {
            true -> DeviceType.Hms
            else -> DeviceType.Gms
        }

        val tokenObserver = object : DisposableObserver<String>() {
            override fun onNext(token: String) {
                if (token.isBlank()) {
                    return
                }

                coroutineScope.launch {
                    try {
                        api.myDevice(deviceType, token)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
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

        MessagingRepositoryFactory.getMessagingService().requestToken(this)

        val messageObserver: DisposableObserver<RemoteMessage> = object : DisposableObserver<RemoteMessage>() {
            override fun onNext(remoteMessage: RemoteMessage) {
                push.receive(remoteMessage.data)
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
    }
}

enum class DeviceType {
    Hms,
    Gms
}
