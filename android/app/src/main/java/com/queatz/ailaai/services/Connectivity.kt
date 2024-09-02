package com.queatz.ailaai.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private class ConnectivityObserver(val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(getNetworkStatus())
    val isConnected = _isConnected.asStateFlow()

    val hasConnectivity @Composable get() = isConnected.collectAsState().value

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.value = true
        }

        override fun onLost(network: Network) {
            _isConnected.value = false
        }
    }

    fun start() {
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            networkCallback
        )
    }

    fun stop() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    fun refresh() {
        _isConnected.value = getNetworkStatus()
    }

    private fun getNetworkStatus(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

val connectivity by lazy {
    Connectivity()
}

class Connectivity {

    private lateinit var observer: ConnectivityObserver

    val hasConnectivity get() = observer.hasConnectivity

    fun start(context: Context) {
        observer = ConnectivityObserver(context)
        observer.start()
    }

    fun stop() {
        observer.stop()
    }

    fun refresh() {
        observer.refresh()
    }
}
