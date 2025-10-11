package com.example.pulse_binada

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class WaterReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "water_reminder_channel"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_NAME = "Water Reminders"
        const val CHANNEL_DESCRIPTION = "Reminders to drink water throughout the day"
    }

    override fun doWork(): Result {
        return try {
            createNotificationChannel()
            showWaterReminderNotification()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showWaterReminderNotification() {
        val intent = Intent(applicationContext, WaterpageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //  current water consumption for smart messaging
        val sharedPreferences = applicationContext.getSharedPreferences("water_tracker", Context.MODE_PRIVATE)
        val waterJson = sharedPreferences.getString("water_entries", "[]") ?: "[]"
        val dailyGoal = sharedPreferences.getInt("daily_goal", 8)
        
        // Calculate today's progress
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val waterArray = org.json.JSONArray(waterJson)
        var currentGlasses = 0
        
        for (i in 0 until waterArray.length()) {
            val entry = waterArray.getJSONObject(i)
            if (entry.getString("date") == today) {
                currentGlasses = entry.getInt("glasses")
                break
            }
        }
        
        val progress = if (dailyGoal > 0) {
            (currentGlasses.toFloat() / dailyGoal.toFloat()) * 100f
        } else {
            0f
        }
        
        // Smart notification content based on progress
        val (title, content) = when {
            progress >= 100 -> "🎉 Goal Achieved!" to "Amazing! You've reached your daily hydration goal!"
            progress >= 75 -> "💪 Almost There!" to "You're at ${progress.toInt()}%! Just ${dailyGoal - currentGlasses} more glasses!"
            progress >= 50 -> "🚀 Halfway There!" to "Great progress! ${currentGlasses}/${dailyGoal} glasses completed"
            progress >= 25 -> "💧 Keep Going!" to "Good start! Time for another glass of water"
            currentGlasses > 0 -> "🌱 Every Drop Counts!" to "You've had $currentGlasses glasses. Let's add one more!"
            else -> "💧 Let's Start!" to "Time to begin your hydration journey with a glass of water"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .addAction(
                R.drawable.ic_water,
                "Add Glass",
                pendingIntent
            )
            .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            // Check if have notification permission
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(NOTIFICATION_ID, notification)
            }

        }
    }
}
