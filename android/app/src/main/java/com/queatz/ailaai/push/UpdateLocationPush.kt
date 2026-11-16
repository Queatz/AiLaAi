package com.queatz.ailaai.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.app.NotificationCompat
import androidx.work.*
import app.ailaai.api.myGeo
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.extensions.toLatLng
import com.queatz.ailaai.services.Push
import com.queatz.push.UpdateLocationPushData
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
        val locationClient = LocationServices.getFusedLocationProviderClient(context)
        val done = CompletableDeferred<Unit>()
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
            .setMaxUpdateDelayMillis(60_000L)
            .build()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val latLng = location.toLatLng()
                locationClient.removeLocationUpdates(this)

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

        try {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            // Wait for location to be fetched
            withTimeoutOrNull(1.minutes) {
                done.await()
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        } finally {
            locationClient.removeLocationUpdates(locationCallback)
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
