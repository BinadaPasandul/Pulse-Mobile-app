# SharedPreferences Implementation Documentation

## Overview

This document outlines the comprehensive SharedPreferences implementation in the Pulse Binada wellness tracking app. The implementation demonstrates effective use of SharedPreferences for saving small data like user settings and daily wellness habits.

## Architecture

### Centralized Management with PreferencesManager

The app uses a centralized `PreferencesManager` class that provides a singleton pattern for managing all SharedPreferences operations across the application.

```kotlin
class PreferencesManager private constructor(context: Context) {
    companion object {
        fun getInstance(context: Context): PreferencesManager
    }
}
```

### Data Categories

The implementation organizes data into four main categories, each with its own SharedPreferences file:

1. **Habit Tracking** (`habit_tracker`)
2. **Mood Tracking** (`mood_tracker`) 
3. **Water Tracking** (`water_tracker`)
4. **App Settings** (`app_settings`)

## Data Models

### HabitEntry
```kotlin
data class HabitEntry(
    val id: String,
    val text: String,
    val isCompleted: Boolean,
    val createdAt: String,
    val completedAt: String? = null
)
```

### MoodEntry
```kotlin
data class MoodEntry(
    val id: String,
    val emoji: String,
    val mood: String,
    val date: String,
    val time: String,
    val notes: String? = null
)
```

### WaterEntry
```kotlin
data class WaterEntry(
    val id: String,
    val date: String,
    val time: String,
    val glasses: Int,
    val notes: String? = null
)
```

## Key Features

### 1. User Settings Management

#### Water Goal Settings
- **Daily Water Goal**: User can set custom daily water consumption goals (1-20 glasses)
- **Reminder Intervals**: Configurable reminder intervals (15-480 minutes)
- **Reminder Toggle**: Enable/disable water reminders

```kotlin
// Save water goal
preferencesManager.setDailyWaterGoal(8)

// Get water goal
val goal = preferencesManager.getDailyWaterGoal()

// Save reminder interval
preferencesManager.setWaterReminderInterval(60)

// Enable/disable reminders
preferencesManager.setWaterRemindersEnabled(true)
```

#### App Preferences
- **Theme Selection**: Dark, Light, or Auto theme
- **Notification Settings**: Enable/disable notifications
- **First Launch Tracking**: Track onboarding completion

```kotlin
// Theme management
preferencesManager.setAppTheme("dark")
val theme = preferencesManager.getAppTheme()

// Notification settings
preferencesManager.setNotificationEnabled(true)
val notificationsEnabled = preferencesManager.areNotificationsEnabled()
```

### 2. Daily Wellness Habits Tracking

#### Habit Management
- **Create Habits**: Add new daily habits with unique IDs
- **Complete Habits**: Mark habits as completed with timestamps
- **Delete Habits**: Remove habits from tracking
- **Progress Tracking**: Calculate daily completion percentages

```kotlin
// Save a habit
val habit = PreferencesManager.HabitEntry(
    id = UUID.randomUUID().toString(),
    text = "Drink 8 glasses of water",
    isCompleted = false,
    createdAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
)
preferencesManager.saveHabit(habit)

// Get all habits
val habits = preferencesManager.getAllHabits()

// Get habits for specific date
val todayHabits = preferencesManager.getHabitsForDate("2024-12-15")

// Get daily completion percentage
val completionPercentage = preferencesManager.getDailyHabitCompletion("2024-12-15")
```

#### Weekly Progress Tracking
- **Weekly Completion Data**: Track habit completion across the week
- **Progress Visualization**: Generate data for progress bars

```kotlin
// Get weekly habit completion data
val weeklyData = preferencesManager.getWeeklyHabitCompletion()
// Returns: Map<String, Float> where key is date and value is completion percentage
```

### 3. Mood Tracking Data

#### Mood Entry Management
- **Save Mood Entries**: Store mood data with emoji, mood name, date, and time
- **Mood History**: Retrieve all mood entries sorted by date/time
- **Daily Mood Tracking**: Get mood entries for specific dates

```kotlin
// Save mood entry
val moodEntry = PreferencesManager.MoodEntry(
    id = UUID.randomUUID().toString(),
    emoji = "😊",
    mood = "Happy",
    date = "2024-12-15",
    time = "14:30"
)
preferencesManager.saveMoodEntry(moodEntry)

// Get all mood entries
val moods = preferencesManager.getAllMoodEntries()

// Get today's latest mood
val todayMood = preferencesManager.getTodayLatestMood()

// Get weekly mood statistics
val weeklyMoods = preferencesManager.getWeeklyMoodStats()
```

### 4. Water Consumption Tracking

#### Water Entry Management
- **Log Water Consumption**: Track glasses of water consumed with timestamps
- **Daily Totals**: Calculate total water consumed per day
- **Weekly Analytics**: Generate weekly consumption data
- **Streak Tracking**: Calculate consecutive days meeting water goals

```kotlin
// Save water entry
val waterEntry = PreferencesManager.WaterEntry(
    id = UUID.randomUUID().toString(),
    date = "2024-12-15",
    time = "10:30",
    glasses = 1
)
preferencesManager.saveWaterEntry(waterEntry)

// Get today's water consumption
val todayWater = preferencesManager.getTodayWaterConsumption()

// Get weekly water consumption
val weeklyWater = preferencesManager.getWeeklyWaterConsumption()

// Calculate water consumption streak
val streak = preferencesManager.getWaterConsumptionStreak()
```

## Analytics and Statistics

### Wellness Score Calculation
The app calculates a comprehensive wellness score based on:
- **Habit Completion** (40% weight): Daily habit completion percentage
- **Water Consumption** (30% weight): Achievement of daily water goal
- **Mood Tracking** (30% weight): Positive mood entries

```kotlin
// Get today's wellness score (0-100)
val wellnessScore = preferencesManager.getTodayWellnessScore()

// Get weekly wellness trends
val weeklyTrends = preferencesManager.getWeeklyWellnessTrends()
```

### Data Export and Management
- **Export All Data**: Generate JSON export of all user data
- **Clear All Data**: Reset all stored data (with confirmation)
- **Data Backup**: Structured JSON format for data portability

```kotlin
// Export all data as JSON
val exportData = preferencesManager.exportAllData()

// Clear all data
preferencesManager.clearAllData()
```

## Implementation Examples

### SettingsActivity Usage
The `SettingsActivity` demonstrates comprehensive SharedPreferences usage:

```kotlin
class SettingsActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize PreferencesManager
        preferencesManager = PreferencesManager.getInstance(this)
        
        // Load current settings
        loadCurrentSettings()
        
        // Set up listeners
        setupClickListeners()
    }
    
    private fun loadCurrentSettings() {
        // Load water goal
        dailyWaterGoalInput.setText(preferencesManager.getDailyWaterGoal().toString())
        
        // Load reminder interval
        reminderIntervalInput.setText(preferencesManager.getWaterReminderInterval().toString())
        
        // Load notification settings
        notificationsSwitch.isChecked = preferencesManager.areNotificationsEnabled()
    }
    
    private fun saveWaterGoal() {
        val goal = dailyWaterGoalInput.text.toString().toIntOrNull()
        if (goal != null && goal in 1..20) {
            preferencesManager.setDailyWaterGoal(goal)
            showToast("Daily water goal updated to $goal glasses")
        }
    }
}
```

### Activity Integration
Activities use the PreferencesManager for data persistence:

```kotlin
class HabitpageActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize PreferencesManager
        preferencesManager = PreferencesManager.getInstance(this)
        
        // Load and display habits
        loadAndDisplayHabits()
    }
    
    private fun loadAndDisplayHabits() {
        val habits = preferencesManager.getAllHabits()
        // Display habits in UI
    }
    
    private fun saveHabit(habit: Habit) {
        val habitEntry = PreferencesManager.HabitEntry(
            id = habit.id,
            text = habit.text,
            isCompleted = habit.isCompleted,
            createdAt = habit.createdAt
        )
        preferencesManager.saveHabit(habitEntry)
    }
}
```

## Best Practices Demonstrated

### 1. Centralized Data Management
- Single point of access for all SharedPreferences operations
- Consistent data models across the application
- Reduced code duplication

### 2. Data Validation
- Input validation for user settings (water goals, reminder intervals)
- Range checking for numeric values
- Error handling for invalid data

### 3. Data Persistence Patterns
- JSON serialization for complex data structures
- Unique ID generation for data entries
- Timestamp tracking for all entries

### 4. User Experience
- Real-time settings updates
- Confirmation dialogs for destructive actions
- Toast notifications for user feedback

### 5. Analytics Integration
- Wellness score calculation
- Progress tracking and visualization
- Historical data analysis

## File Structure

```
app/src/main/java/com/example/pulse_binada/
├── PreferencesManager.kt          # Centralized SharedPreferences management
├── HabitpageActivity.kt           # Habit tracking with SharedPreferences
├── MoodpageActivity.kt            # Mood tracking with SharedPreferences
├── WaterpageActivity.kt           # Water tracking with SharedPreferences
├── SettingsActivity.kt            # Settings management with SharedPreferences
└── WaterReminderManager.kt        # Water reminder settings (existing)
```

## Conclusion

This SharedPreferences implementation demonstrates:

1. **Effective Data Organization**: Separate SharedPreferences files for different data categories
2. **Centralized Management**: Single PreferencesManager class for all operations
3. **Comprehensive Settings**: User preferences for goals, reminders, themes, and notifications
4. **Wellness Tracking**: Complete tracking of habits, moods, and water consumption
5. **Analytics Integration**: Wellness scoring and progress tracking
6. **Data Management**: Export, backup, and reset functionality

The implementation provides a robust foundation for persistent data storage in the wellness tracking app, ensuring user settings and daily habits are properly saved and retrieved across app sessions.
