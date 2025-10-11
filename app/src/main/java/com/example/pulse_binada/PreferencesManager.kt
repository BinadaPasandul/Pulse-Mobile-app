package com.example.pulse_binada

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized SharedPreferences manager for the Pulse Binada app
 * Handles all data persistence for user settings, habits, mood tracking, and water consumption
 */
class PreferencesManager private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // SharedPreferences instances for different data categories
    private val habitPreferences: SharedPreferences = 
        context.getSharedPreferences("habit_tracker", Context.MODE_PRIVATE)
    private val moodPreferences: SharedPreferences = 
        context.getSharedPreferences("mood_tracker", Context.MODE_PRIVATE)
    private val waterPreferences: SharedPreferences = 
        context.getSharedPreferences("water_tracker", Context.MODE_PRIVATE)
    private val settingsPreferences: SharedPreferences = 
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    // Date formatter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    // ==================== HABIT TRACKING ====================
    
    /**
     * Data class for habit entries
     */
    data class HabitEntry(
        val id: String,
        val text: String,
        val isCompleted: Boolean,
        val createdAt: String,
        val completedAt: String? = null
    )
    
    /**
     * Save a habit to SharedPreferences
     */
    fun saveHabit(habit: HabitEntry) {
        val habitsJson = habitPreferences.getString("habits", "[]") ?: "[]"
        val habitsArray = JSONArray(habitsJson)
        
        // Check if habit already exists (for updates)
        var existingIndex = -1
        for (i in 0 until habitsArray.length()) {
            val existingHabit = habitsArray.getJSONObject(i)
            if (existingHabit.getString("id") == habit.id) {
                existingIndex = i
                break
            }
        }
        
        val habitJson = JSONObject().apply {
            put("id", habit.id)
            put("text", habit.text)
            put("isCompleted", habit.isCompleted)
            put("createdAt", habit.createdAt)
            put("completedAt", habit.completedAt ?: "")
        }
        
        if (existingIndex >= 0) {
            habitsArray.put(existingIndex, habitJson)
        } else {
            habitsArray.put(habitJson)
        }
        
        habitPreferences.edit()
            .putString("habits", habitsArray.toString())
            .apply()
    }
    
    /**
     * Get all habits from SharedPreferences
     */
    fun getAllHabits(): List<HabitEntry> {
        val habitsJson = habitPreferences.getString("habits", "[]") ?: "[]"
        val habitsArray = JSONArray(habitsJson)
        val habits = mutableListOf<HabitEntry>()
        
        for (i in 0 until habitsArray.length()) {
            val habitJson = habitsArray.getJSONObject(i)
            habits.add(
                HabitEntry(
                    id = habitJson.getString("id"),
                    text = habitJson.getString("text"),
                    isCompleted = habitJson.getBoolean("isCompleted"),
                    createdAt = habitJson.getString("createdAt"),
                    completedAt = if (habitJson.has("completedAt") && !habitJson.getString("completedAt").isEmpty()) 
                        habitJson.getString("completedAt") else null
                )
            )
        }
        
        return habits.sortedByDescending { it.createdAt }
    }
    
    /**
     * Get habits for a specific date
     */
    fun getHabitsForDate(date: String): List<HabitEntry> {
        return getAllHabits().filter { it.createdAt.startsWith(date) }
    }
    
    /**
     * Delete a habit by ID
     */
    fun deleteHabit(habitId: String) {
        val habitsJson = habitPreferences.getString("habits", "[]") ?: "[]"
        val habitsArray = JSONArray(habitsJson)
        val newHabitsArray = JSONArray()
        
        for (i in 0 until habitsArray.length()) {
            val habitJson = habitsArray.getJSONObject(i)
            if (habitJson.getString("id") != habitId) {
                newHabitsArray.put(habitJson)
            }
        }
        
        habitPreferences.edit()
            .putString("habits", newHabitsArray.toString())
            .apply()
    }
    
    /**
     * Get daily habit completion percentage
     */
    fun getDailyHabitCompletion(date: String): Float {
        val dayHabits = getHabitsForDate(date)
        if (dayHabits.isEmpty()) return 0f
        
        val completedCount = dayHabits.count { it.isCompleted }
        return (completedCount.toFloat() / dayHabits.size) * 100f
    }
    
    /**
     * Get weekly habit completion data
     */
    fun getWeeklyHabitCompletion(): Map<String, Float> {
        val weeklyData = mutableMapOf<String, Float>()
        val calendar = Calendar.getInstance()
        
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = dateFormat.format(calendar.time)
            weeklyData[date] = getDailyHabitCompletion(date)
        }
        
        return weeklyData
    }
    
    // ==================== MOOD TRACKING ====================
    
    /**
     * Data class for mood entries
     */
    data class MoodEntry(
        val id: String,
        val emoji: String,
        val mood: String,
        val date: String,
        val time: String,
        val notes: String? = null
    )
    
    /**
     * Save a mood entry to SharedPreferences
     */
    fun saveMoodEntry(mood: MoodEntry) {
        val moodsJson = moodPreferences.getString("mood_entries", "[]") ?: "[]"
        val moodsArray = JSONArray(moodsJson)
        
        val moodJson = JSONObject().apply {
            put("id", mood.id)
            put("emoji", mood.emoji)
            put("mood", mood.mood)
            put("date", mood.date)
            put("time", mood.time)
            put("notes", mood.notes ?: "")
        }
        
        moodsArray.put(moodJson)
        
        moodPreferences.edit()
            .putString("mood_entries", moodsArray.toString())
            .apply()
    }
    
    /**
     * Get all mood entries from SharedPreferences
     */
    fun getAllMoodEntries(): List<MoodEntry> {
        val moodsJson = moodPreferences.getString("mood_entries", "[]") ?: "[]"
        val moodsArray = JSONArray(moodsJson)
        val moods = mutableListOf<MoodEntry>()
        
        for (i in 0 until moodsArray.length()) {
            val moodJson = moodsArray.getJSONObject(i)
            moods.add(
                MoodEntry(
                    id = moodJson.getString("id"),
                    emoji = moodJson.getString("emoji"),
                    mood = moodJson.getString("mood"),
                    date = moodJson.getString("date"),
                    time = moodJson.getString("time"),
                    notes = if (moodJson.has("notes") && !moodJson.getString("notes").isEmpty()) 
                        moodJson.getString("notes") else null
                )
            )
        }
        
        return moods.sortedByDescending { "${it.date} ${it.time}" }
    }
    
    /**
     * Get mood entries for a specific date
     */
    fun getMoodEntriesForDate(date: String): List<MoodEntry> {
        return getAllMoodEntries().filter { it.date == date }
    }
    
    /**
     * Get the latest mood entry for today
     */
    fun getTodayLatestMood(): MoodEntry? {
        val today = dateFormat.format(Date())
        return getMoodEntriesForDate(today).maxByOrNull { it.time }
    }
    
    /**
     * Get mood statistics for the week
     */
    fun getWeeklyMoodStats(): Map<String, String> {
        val weeklyMoods = mutableMapOf<String, String>()
        val calendar = Calendar.getInstance()
        
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = dateFormat.format(calendar.time)
            val dayMoods = getMoodEntriesForDate(date)
            weeklyMoods[date] = dayMoods.maxByOrNull { it.time }?.mood ?: "No mood"
        }
        
        return weeklyMoods
    }
    
    // ==================== WATER TRACKING ====================
    
    /**
     * Data class for water entries
     */
    data class WaterEntry(
        val id: String,
        val date: String,
        val time: String,
        val glasses: Int,
        val notes: String? = null
    )
    
    /**
     * Save a water entry to SharedPreferences
     */
    fun saveWaterEntry(water: WaterEntry) {
        val waterJson = waterPreferences.getString("water_entries", "[]") ?: "[]"
        val waterArray = JSONArray(waterJson)
        
        val waterEntryJson = JSONObject().apply {
            put("id", water.id)
            put("date", water.date)
            put("time", water.time)
            put("glasses", water.glasses)
            put("notes", water.notes ?: "")
        }
        
        waterArray.put(waterEntryJson)
        
        waterPreferences.edit()
            .putString("water_entries", waterArray.toString())
            .apply()
    }
    
    /**
     * Get all water entries from SharedPreferences
     */
    fun getAllWaterEntries(): List<WaterEntry> {
        val waterJson = waterPreferences.getString("water_entries", "[]") ?: "[]"
        val waterArray = JSONArray(waterJson)
        val waterEntries = mutableListOf<WaterEntry>()
        
        for (i in 0 until waterArray.length()) {
            val waterEntryJson = waterArray.getJSONObject(i)
            waterEntries.add(
                WaterEntry(
                    id = waterEntryJson.getString("id"),
                    date = waterEntryJson.getString("date"),
                    time = waterEntryJson.getString("time"),
                    glasses = waterEntryJson.getInt("glasses"),
                    notes = if (waterEntryJson.has("notes") && !waterEntryJson.getString("notes").isEmpty()) 
                        waterEntryJson.getString("notes") else null
                )
            )
        }
        
        return waterEntries.sortedByDescending { "${it.date} ${it.time}" }
    }
    
    /**
     * Get water entries for a specific date
     */
    fun getWaterEntriesForDate(date: String): List<WaterEntry> {
        return getAllWaterEntries().filter { it.date == date }
    }
    
    /**
     * Get total glasses consumed today
     */
    fun getTodayWaterConsumption(): Int {
        val today = dateFormat.format(Date())
        return getWaterEntriesForDate(today).sumOf { it.glasses }
    }
    
    /**
     * Get weekly water consumption data
     */
    fun getWeeklyWaterConsumption(): Map<String, Int> {
        val weeklyData = mutableMapOf<String, Int>()
        val calendar = Calendar.getInstance()
        
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = dateFormat.format(calendar.time)
            weeklyData[date] = getWaterEntriesForDate(date).sumOf { it.glasses }
        }
        
        return weeklyData
    }
    
    /**
     * Calculate water consumption streak
     */
    fun getWaterConsumptionStreak(): Int {
        val calendar = Calendar.getInstance()
        var streak = 0
        val dailyGoal = getDailyWaterGoal()
        
        while (true) {
            val date = dateFormat.format(calendar.time)
            val dayConsumption = getWaterEntriesForDate(date).sumOf { it.glasses }
            
            if (dayConsumption >= dailyGoal) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        
        return streak
    }
    
    // ==================== USER SETTINGS ====================
    
    /**
     * Set daily water goal
     */
    fun setDailyWaterGoal(goal: Int) {
        waterPreferences.edit()
            .putInt("daily_water_goal", goal)
            .apply()
    }
    
    /**
     * Get daily water goal
     */
    fun getDailyWaterGoal(): Int {
        return waterPreferences.getInt("daily_water_goal", 8)
    }
    
    /**
     * Set water reminder interval
     */
    fun setWaterReminderInterval(intervalMinutes: Int) {
        waterPreferences.edit()
            .putInt("reminder_interval_minutes", intervalMinutes)
            .apply()
    }
    
    /**
     * Get water reminder interval
     */
    fun getWaterReminderInterval(): Int {
        return waterPreferences.getInt("reminder_interval_minutes", 60)
    }
    
    /**
     * Enable/disable water reminders
     */
    fun setWaterRemindersEnabled(enabled: Boolean) {
        waterPreferences.edit()
            .putBoolean("reminders_enabled", enabled)
            .apply()
    }
    
    /**
     * Check if water reminders are enabled
     */
    fun areWaterRemindersEnabled(): Boolean {
        return waterPreferences.getBoolean("reminders_enabled", false)
    }
    
    /**
     * Set app theme preference
     */
    fun setAppTheme(theme: String) {
        settingsPreferences.edit()
            .putString("app_theme", theme)
            .apply()
    }
    
    /**
     * Get app theme preference
     */
    fun getAppTheme(): String {
        return settingsPreferences.getString("app_theme", "dark") ?: "dark"
    }
    
    /**
     * Set notification preferences
     */
    fun setNotificationEnabled(enabled: Boolean) {
        settingsPreferences.edit()
            .putBoolean("notifications_enabled", enabled)
            .apply()
    }
    
    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return settingsPreferences.getBoolean("notifications_enabled", true)
    }
    
    /**
     * Set first launch flag
     */
    fun setFirstLaunch(completed: Boolean) {
        settingsPreferences.edit()
            .putBoolean("first_launch_completed", completed)
            .apply()
    }
    
    /**
     * Check if this is the first launch
     */
    fun isFirstLaunch(): Boolean {
        return !settingsPreferences.getBoolean("first_launch_completed", false)
    }
    
    // ==================== ANALYTICS & STATISTICS ====================
    
    /**
     * Get overall wellness score for today
     */
    fun getTodayWellnessScore(): Int {
        val habitScore = (getDailyHabitCompletion(dateFormat.format(Date())) * 0.4).toInt()
        val waterScore = if (getTodayWaterConsumption() >= getDailyWaterGoal()) 30 else 
            (getTodayWaterConsumption().toFloat() / getDailyWaterGoal() * 30).toInt()
        val moodScore = when (getTodayLatestMood()?.mood) {
            "Happy", "Excited", "Grateful", "Peaceful" -> 30
            "Neutral" -> 20
            else -> 10
        }
        
        return habitScore + waterScore + moodScore
    }
    
    /**
     * Get weekly wellness trends
     */
    fun getWeeklyWellnessTrends(): Map<String, Int> {
        val trends = mutableMapOf<String, Int>()
        val calendar = Calendar.getInstance()
        
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = dateFormat.format(calendar.time)
            
            val habitScore = (getDailyHabitCompletion(date) * 0.4).toInt()
            val waterScore = if (getWaterEntriesForDate(date).sumOf { it.glasses } >= getDailyWaterGoal()) 30 else 
                (getWaterEntriesForDate(date).sumOf { it.glasses }.toFloat() / getDailyWaterGoal() * 30).toInt()
            val moodScore = when (getMoodEntriesForDate(date).maxByOrNull { it.time }?.mood) {
                "Happy", "Excited", "Grateful", "Peaceful" -> 30
                "Neutral" -> 20
                else -> 10
            }
            
            trends[date] = habitScore + waterScore + moodScore
        }
        
        return trends
    }
    
    /**
     * Delete a mood entry by ID
     */
    fun deleteMoodEntry(moodId: String) {
        val moodsJson = moodPreferences.getString("mood_entries", "[]") ?: "[]"
        val moodsArray = JSONArray(moodsJson)
        val newMoodsArray = JSONArray()
        
        for (i in 0 until moodsArray.length()) {
            val moodJson = moodsArray.getJSONObject(i)
            if (moodJson.getString("id") != moodId) {
                newMoodsArray.put(moodJson)
            }
        }
        
        moodPreferences.edit()
            .putString("mood_entries", newMoodsArray.toString())
            .apply()
    }
    
    /**
     * Delete a water entry by ID
     */
    fun deleteWaterEntry(waterId: String) {
        val waterJson = waterPreferences.getString("water_entries", "[]") ?: "[]"
        val waterArray = JSONArray(waterJson)
        val newWaterArray = JSONArray()
        
        for (i in 0 until waterArray.length()) {
            val waterJson = waterArray.getJSONObject(i)
            if (waterJson.getString("id") != waterId) {
                newWaterArray.put(waterJson)
            }
        }
        
        waterPreferences.edit()
            .putString("water_entries", newWaterArray.toString())
            .apply()
    }
    
    /**
     * Export all data as JSON (for backup)
     */
    fun exportAllData(): String {
        val exportData = JSONObject().apply {
            put("habits", habitPreferences.getString("habits", "[]") ?: "[]")
            put("mood_entries", moodPreferences.getString("mood_entries", "[]") ?: "[]")
            put("water_entries", waterPreferences.getString("water_entries", "[]") ?: "[]")
            put("settings", JSONObject().apply {
                put("daily_water_goal", getDailyWaterGoal())
                put("reminder_interval", getWaterReminderInterval())
                put("reminders_enabled", areWaterRemindersEnabled())
                put("app_theme", getAppTheme())
                put("notifications_enabled", areNotificationsEnabled())
            })
            put("export_date", dateFormat.format(Date()))
        }
        
        return exportData.toString()
    }
}
