package com.queatz.ailaai.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.app.NotificationCompat
import androidx.work.*
import app.ailaai.api.myGeo
import at.bluesource.choicesdk.core.Outcome
import at.bluesource.choicesdk.location.common.LocationRequest
import at.bluesource.choicesdk.location.factory.FusedLocationProviderFactory
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.extensions.toLatLng
import com.queatz.ailaai.services.Push
import com.queatz.push.UpdateLocationPushData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.minutes

fun Push.receive(data: UpdateLocationPushData) {
    // 1. Check for location permissions
    if (checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        // Permissions not granted, cannot proceed
        return
    }

    // 2. Get the current location in a worker with foreground service and notification
    val workRequest = OneTimeWorkRequestBuilder<LocationUpdateWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(
            uniqueWorkName = "location_update_work",
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = workRequest
        )
}

class LocationUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override suspend fun doWork(): Result {
        // Create notification channel for foreground service
        createNotificationChannel()

        // Set foreground service with notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.updating_location))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        setForeground(ForegroundInfo(NOTIFICATION_ID, notification))

        // Get current location
        val locationClient = FusedLocationProviderFactory.getFusedLocationProviderClient(context)
        val done = CompletableDeferred<Unit>()

        try {
            val disposable = locationClient
                .observeLocation(
                    LocationRequest.Builder()
                        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                        .setInterval(10000) // 10 seconds
                        .setMaxWaitTime(60000) // 1 minute
                        .build()
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { outcome ->
                    if (outcome is Outcome.Success) {
                        val location = outcome.value.lastLocation!!
                        val latLng = location.toLatLng()

                        // 3. Send location to server via API call
                        CoroutineScope(Dispatchers.IO).launch {
                            api.myGeo(
                                geo = latLng.toGeo()
                            ) {
                                done.complete(Unit)
                            }
                        }
                    }
                }

            // Wait for location to be fetched
            withTimeoutOrNull(1.minutes) {
                done.await()
            }

            disposable.dispose()

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }

    private fun createNotificationChannel() {
        val name = context.getString(R.string.location_update_channel)
        val descriptionText = context.getString(R.string.location_update_channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "location_update_channel"
        private const val NOTIFICATION_ID = 1
    }
}
