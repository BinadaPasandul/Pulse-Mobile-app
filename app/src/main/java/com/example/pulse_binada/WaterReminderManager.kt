package com.example.pulse_binada

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class WaterReminderManager(private val context: Context) {

    companion object {
        private const val WORK_NAME = "water_reminder_work"
        private const val REMINDER_INTERVAL_KEY = "reminder_interval_minutes"
    }

    private val workManager = WorkManager.getInstance(context)


    fun scheduleWaterReminders(intervalMinutes: Int) {
        // Cancel existing work first
        cancelWaterReminders()

        // Create constraints for the work
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .build()

        // Create input data
        val inputData = Data.Builder()
            .putInt(REMINDER_INTERVAL_KEY, intervalMinutes)
            .build()

        // Create periodic work request
        val waterReminderWork = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            intervalMinutes.toLong(),
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        // Enqueue the work
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            waterReminderWork
        )
    }

    /**
     * Cancel all water reminder notifications
     */
    fun cancelWaterReminders() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    /**
     *  if water reminders are currently scheduled
     */
    fun areRemindersScheduled(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
        return workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
    }

    //Get the current reminder interval from SharedPreferences

    fun getReminderInterval(): Int {
        val sharedPreferences = context.getSharedPreferences("water_tracker", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("reminder_interval_minutes", 60) // Default 60 minutes
    }

    /**
     * Save the reminder interval to SharedPreferences
     */
    fun saveReminderInterval(intervalMinutes: Int) {
        val sharedPreferences = context.getSharedPreferences("water_tracker", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putInt("reminder_interval_minutes", intervalMinutes)
            .apply()
    }

    /**
     *  if reminders are enabled
     */
    fun areRemindersEnabled(): Boolean {
        val sharedPreferences = context.getSharedPreferences("water_tracker", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("reminders_enabled", false)
    }

    /**
     * Enable or disable reminders
     */
    fun setRemindersEnabled(enabled: Boolean) {
        val sharedPreferences = context.getSharedPreferences("water_tracker", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("reminders_enabled", enabled)
            .apply()
    }

    /**
      smart reminder suggestions based on user's hydration patterns
     */
    fun getSmartReminderSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Get user's average daily consumption
        val avgConsumption = getAverageDailyConsumption()
        val dailyGoal = getReminderInterval() // Using this as a proxy for goal
        
        when {
            avgConsumption < dailyGoal * 0.5 -> {
                suggestions.add("Start your day with a glass of water")
                suggestions.add("Set reminders every 2 hours")
                suggestions.add("Keep a water bottle nearby")
            }
            avgConsumption < dailyGoal * 0.75 -> {
                suggestions.add("You're making good progress!")
                suggestions.add("Try drinking water before meals")
                suggestions.add("Set reminders every 3 hours")
            }
            avgConsumption >= dailyGoal -> {
                suggestions.add("Excellent hydration habits!")
                suggestions.add("Consider reducing reminder frequency")
                suggestions.add("You might only need morning reminders")
            }
            else -> {
                suggestions.add("You're almost at your goal!")
                suggestions.add("Try drinking water with snacks")
                suggestions.add("Set reminders every 4 hours")
            }
        }
        
        return suggestions
    }

    /**
     * Calculate average daily water consumption
     */
    private fun getAverageDailyConsumption(): Int {
        val sharedPreferences = context.getSharedPreferences("water_tracker", Context.MODE_PRIVATE)
        val waterJson = sharedPreferences.getString("water_entries", "[]") ?: "[]"
        val waterArray = org.json.JSONArray(waterJson)
        
        if (waterArray.length() == 0) return 0
        
        var totalGlasses = 0
        var dayCount = 0
        val dates = mutableSetOf<String>()
        
        for (i in 0 until waterArray.length()) {
            val entry = waterArray.getJSONObject(i)
            val date = entry.getString("date")
            if (dates.add(date)) {
                dayCount++
            }
            totalGlasses += entry.getInt("glasses")
        }
        
        return if (dayCount > 0) totalGlasses / dayCount else 0
    }

    /**
       optimal reminder interval based on user patterns
     */
    fun getOptimalReminderInterval(): Int {
        val avgConsumption = getAverageDailyConsumption()
        val dailyGoal = 8 // Default goal
        
        return when {
            avgConsumption < dailyGoal * 0.5 -> 90 // 1.5 hours for low consumption
            avgConsumption < dailyGoal * 0.75 -> 120 // 2 hours for moderate consumption
            avgConsumption >= dailyGoal -> 240 // 4 hours for good consumption
            else -> 150 // 2.5 hours for default
        }
    }


    fun needsMoreFrequentReminders(): Boolean {
        val avgConsumption = getAverageDailyConsumption()
        val dailyGoal = 8
        return avgConsumption < dailyGoal * 0.6
    }
}
