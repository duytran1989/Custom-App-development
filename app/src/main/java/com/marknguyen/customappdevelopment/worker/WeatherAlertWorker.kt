package com.marknguyen.customappdevelopment.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.marknguyen.customappdevelopment.R

class WeatherAlertWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val city = inputData.getString("city") ?: return Result.success()
        val message = inputData.getString("message") ?: return Result.success()
        postNotification(city, message)
        return Result.success()
    }

    private fun postNotification(city: String, message: String) {
        val channelId = "weather_alerts"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Weather Alerts", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Severe weather notifications" }
            )
        }
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Weather Alert — $city")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}
