package com.example.pulse_binada

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WaterpageActivity : AppCompatActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var circularProgressView: CircularProgressView
    private lateinit var progressTextView: TextView
    private lateinit var progressPercentageTextView: TextView
    private lateinit var addGlassButton: Button
    private lateinit var removeGlassButton: Button
    private lateinit var goalInput: EditText
    private lateinit var saveGoalButton: Button
    private lateinit var waterHistoryContainer: LinearLayout
    private lateinit var emptyWaterState: LinearLayout
    
    // New UI elements
    private lateinit var achievementBadge: LinearLayout
    private lateinit var achievementText: TextView
    private lateinit var motivationalMessage: TextView
    private lateinit var waterDropIcon: ImageView
    private lateinit var floatingDrop1: ImageView
    private lateinit var floatingDrop2: ImageView
    
    // Analytics UI elements
    private lateinit var weeklyTotal: TextView
    private lateinit var streakCount: TextView
    private lateinit var goalsAchieved: TextView
    private lateinit var totalWater: TextView
    
    // Smart suggestions UI elements
    private lateinit var suggestionCount: TextView
    private lateinit var suggestion1: TextView
    private lateinit var suggestion2: TextView
    private lateinit var suggestion3: TextView
    
    // Reminder UI elements
    private lateinit var reminderIntervalInput: EditText
    private lateinit var enableRemindersButton: Button
    private lateinit var resetProgressButton: Button
    
    private var currentGlasses = 0
    private var dailyGoal = 8
    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    // Reminder manager
    private lateinit var reminderManager: WaterReminderManager
    
    // Permission launcher for notifications
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupReminders()
        } else {
            showToast("Notification permission is required for reminders")
        }
    }
    
    // Data model for water entries
    data class WaterEntry(
        val id: String,
        val date: String,
        val time: String,
        val glasses: Int
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.waterpage)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize PreferencesManager
        preferencesManager = PreferencesManager.getInstance(this)
        
        // Initialize reminder manager
        reminderManager = WaterReminderManager(this)
        
        // Initialize views
        initializeViews()
        
        // Set up click listeners
        setupClickListeners()
        
        // Load data and update UI
        loadDataAndUpdateUI()
        
        // Set up navigation listeners
        setupNavigationListeners()
        
        // Load reminder settings
        loadReminderSettings()
    }
    
    private fun initializeViews() {
        circularProgressView = findViewById(R.id.circular_progress)
        progressTextView = findViewById(R.id.progress_text)
        progressPercentageTextView = findViewById(R.id.progress_percentage)
        addGlassButton = findViewById(R.id.add_glass_button)
        removeGlassButton = findViewById(R.id.remove_glass_button)
        goalInput = findViewById(R.id.goal_input)
        saveGoalButton = findViewById(R.id.save_goal_button)
        waterHistoryContainer = findViewById(R.id.water_history_container)
        emptyWaterState = findViewById(R.id.empty_water_state)
        
        // New UI elements
        achievementBadge = findViewById(R.id.achievement_badge)
        achievementText = findViewById(R.id.achievement_text)
        motivationalMessage = findViewById(R.id.motivational_message)
        waterDropIcon = findViewById(R.id.water_drop_icon)
        floatingDrop1 = findViewById(R.id.floating_drop_1)
        floatingDrop2 = findViewById(R.id.floating_drop_2)
        
        // Analytics UI elements
        weeklyTotal = findViewById(R.id.weekly_total)
        streakCount = findViewById(R.id.streak_count)
        goalsAchieved = findViewById(R.id.goals_achieved)
        totalWater = findViewById(R.id.total_water)
        
        // Smart suggestions UI elements
        suggestionCount = findViewById(R.id.suggestion_count)
        suggestion1 = findViewById(R.id.suggestion_1)
        suggestion2 = findViewById(R.id.suggestion_2)
        suggestion3 = findViewById(R.id.suggestion_3)
        
        // Reminder UI elements
        reminderIntervalInput = findViewById(R.id.reminder_interval_input)
        enableRemindersButton = findViewById(R.id.enable_reminders_button)
        resetProgressButton = findViewById(R.id.reset_progress_button)
    }
    
    private fun setupClickListeners() {
        addGlassButton.setOnClickListener {
            addGlass()
        }
        
        removeGlassButton.setOnClickListener {
            removeGlass()
        }
        
        saveGoalButton.setOnClickListener {
            saveGoal()
        }
        
        enableRemindersButton.setOnClickListener {
            toggleReminders()
        }
        
        resetProgressButton.setOnClickListener {
            resetTodayProgress()
        }
        
        // Enable save button when goal input changes
        goalInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveGoalButton.isEnabled = !s.isNullOrBlank()
            }
        })
    }
    
    private fun addGlass() {
        currentGlasses++
        
        // Animate button press
        addGlassButton.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                addGlassButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
        
        updateUI()
        saveWaterEntry()
        
        // Enhanced toast messages based on progress
        val progress = if (dailyGoal > 0) {
            (currentGlasses.toFloat() / dailyGoal.toFloat()) * 100f
        } else {
            0f
        }
        
        val message = when {
            progress >= 100 -> "Perfect! You've reached your daily goal! 🎉"
            progress >= 75 -> "Almost there! Great job! 💪"
            progress >= 50 -> "Halfway there! Keep going! 🚀"
            else -> "Glass of water added! 💧"
        }
        
        showToast(message)
    }
    
    private fun removeGlass() {
        if (currentGlasses > 0) {
            currentGlasses--
            
            // Animate button press
            removeGlassButton.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    removeGlassButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
            
            updateUI()
            saveWaterEntry()
            showToast("Glass of water removed")
        } else {
            showToast("No glasses to remove")
        }
    }
    
    private fun saveGoal() {
        val goalText = goalInput.text.toString().trim()
        if (goalText.isNotEmpty()) {
            val newGoal = goalText.toIntOrNull() ?: return
            if (newGoal in 1..20) {
                dailyGoal = newGoal
                // Save using PreferencesManager
                preferencesManager.setDailyWaterGoal(dailyGoal)
                updateUI()
                showToast("Daily goal updated to $dailyGoal glasses")
            } else {
                showToast("Goal must be between 1 and 20 glasses")
            }
        }
    }
    
    private fun updateUI() {
        // Update progress text
        progressTextView.text = "$currentGlasses/$dailyGoal"
        
        // Update progress percentage
        val progress = if (dailyGoal > 0) {
            (currentGlasses.toFloat() / dailyGoal.toFloat()) * 100f
        } else {
            0f
        }
        progressPercentageTextView.text = "${progress.toInt()}%"
        
        // Update circular progress with animation
        circularProgressView.setProgress(progress)
        
        // Enable/disable remove button
        removeGlassButton.isEnabled = currentGlasses > 0
        removeGlassButton.alpha = if (currentGlasses > 0) 1.0f else 0.5f
        
        // Update motivational message
        updateMotivationalMessage()
        
        // Check for achievements
        checkAchievements()
        
        // Update analytics
        updateAnalytics()
        
        // Start floating animations
        startFloatingAnimations()
    }
    
    private fun saveWaterEntry() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        
        val waterEntry = PreferencesManager.WaterEntry(
            UUID.randomUUID().toString(),
            today,
            currentTime,
            currentGlasses
        )
        
        // Save using PreferencesManager
        preferencesManager.saveWaterEntry(waterEntry)
    }
    
    private fun loadDataAndUpdateUI() {
        // Load daily goal using PreferencesManager
        dailyGoal = preferencesManager.getDailyWaterGoal()
        goalInput.setText(dailyGoal.toString())
        
        // Load today's water consumption
        loadTodayWaterConsumption()
        
        // Update UI
        updateUI()
        
        // Load water history
        loadWaterHistory()
    }
    
    private fun loadTodayWaterConsumption() {
        // Use PreferencesManager to get today's water consumption
        currentGlasses = preferencesManager.getTodayWaterConsumption()
    }
    
    private fun loadWaterHistory() {
        // Use PreferencesManager to get all water entries
        val waterEntries = preferencesManager.getAllWaterEntries()
        
        if (waterEntries.isEmpty()) {
            emptyWaterState.visibility = View.VISIBLE
            return
        }
        
        emptyWaterState.visibility = View.GONE
        
        // Clear existing views (keep empty state)
        waterHistoryContainer.removeAllViews()
        waterHistoryContainer.addView(emptyWaterState) // Add empty state back
        
        // Add water history entries (most recent first)
        waterEntries.forEach { waterEntry ->
            val localWaterEntry = WaterEntry(
                waterEntry.id,
                waterEntry.date,
                waterEntry.time,
                waterEntry.glasses
            )
            addWaterEntryToUI(localWaterEntry)
        }
    }
    
    private fun addWaterEntryToUI(waterEntry: WaterEntry) {
        val waterView = createWaterEntryView(waterEntry)
        waterHistoryContainer.addView(waterView, 0) // Add at the beginning
    }
    
    private fun createWaterEntryView(waterEntry: WaterEntry): View {
        val waterView = LayoutInflater.from(this).inflate(R.layout.water_entry_item, waterHistoryContainer, false)
        
        val dateTextView = waterView.findViewById<TextView>(R.id.water_entry_date)
        val timeTextView = waterView.findViewById<TextView>(R.id.water_entry_time)
        val glassesTextView = waterView.findViewById<TextView>(R.id.water_entry_glasses)
        val progressBar = waterView.findViewById<ProgressBar>(R.id.water_entry_progress)
        val percentageTextView = waterView.findViewById<TextView>(R.id.water_entry_percentage)
        val achievementLayout = waterView.findViewById<LinearLayout>(R.id.entry_achievement)
        
        val isToday = waterEntry.date == today
        val displayDate = if (isToday) "Today" else formatDateForDisplay(waterEntry.date)
        
        dateTextView.text = displayDate
        timeTextView.text = waterEntry.time
        glassesTextView.text = "${waterEntry.glasses} glasses"
        
        // Calculate and display percentage
        val percentage = if (dailyGoal > 0) {
            (waterEntry.glasses.toFloat() / dailyGoal.toFloat() * 100).toInt()
        } else {
            0
        }
        percentageTextView.text = "$percentage%"
        
        // Set progress bar
        progressBar.max = dailyGoal
        progressBar.progress = waterEntry.glasses
        
        // Show achievement badge if goal was reached
        if (waterEntry.glasses >= dailyGoal) {
            achievementLayout.visibility = View.VISIBLE
        } else {
            achievementLayout.visibility = View.GONE
        }
        
        return waterView
    }
    
    private fun formatDateForDisplay(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun setupNavigationListeners() {
        // Home navigation - go to HabitpageActivity (main home)
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this, HabitpageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        
        // Mood navigation
        findViewById<LinearLayout>(R.id.nav_mood).setOnClickListener {
            val intent = Intent(this, MoodpageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        
        // Add Habit navigation - go to HabitpageActivity
        findViewById<LinearLayout>(R.id.nav_add_habit).setOnClickListener {
            val intent = Intent(this, HabitpageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        
        // Water navigation (current page - do nothing)
        findViewById<LinearLayout>(R.id.nav_water).setOnClickListener {
            // Already on water page - just show feedback
            showToast("You're already on the Water page! 💧")
        }
        
        // Settings navigation
        findViewById<LinearLayout>(R.id.nav_settings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun loadReminderSettings() {
        // Load reminder interval
        val interval = reminderManager.getReminderInterval()
        reminderIntervalInput.setText(interval.toString())
        
        // Update button text based on current state
        updateReminderButtonText()
    }
    
    private fun toggleReminders() {
        if (reminderManager.areRemindersEnabled()) {
            // Disable reminders
            reminderManager.setRemindersEnabled(false)
            reminderManager.cancelWaterReminders()
            updateReminderButtonText()
            showToast("Reminders disabled")
        } else {
            // Enable reminders - check permission first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    setupReminders()
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                setupReminders()
            }
        }
    }
    
    private fun setupReminders() {
        val intervalText = reminderIntervalInput.text.toString().trim()
        if (intervalText.isNotEmpty()) {
            val interval = intervalText.toIntOrNull() ?: return
            
            if (interval in 15..480) { // Between 15 minutes and 8 hours
                reminderManager.saveReminderInterval(interval)
                reminderManager.setRemindersEnabled(true)
                reminderManager.scheduleWaterReminders(interval)
                updateReminderButtonText()
                showToast("Reminders enabled every $interval minutes")
            } else {
                showToast("Interval must be between 15 and 480 minutes")
            }
        } else {
            showToast("Please enter a reminder interval")
        }
    }
    
    private fun updateReminderButtonText() {
        if (reminderManager.areRemindersEnabled()) {
            enableRemindersButton.text = "🔕 Disable Reminders"
        } else {
            enableRemindersButton.text = "🔔 Enable Reminders"
        }
    }
    
    private fun resetTodayProgress() {
        currentGlasses = 0
        updateUI()
        saveWaterEntry()
        showToast("Today's progress reset")
    }
    
    private fun updateMotivationalMessage() {
        val progress = if (dailyGoal > 0) {
            (currentGlasses.toFloat() / dailyGoal.toFloat()) * 100f
        } else {
            0f
        }
        
        val messages = when {
            progress >= 100 -> "Amazing! You're fully hydrated! 🌟"
            progress >= 75 -> "Great job! Almost there! 💪"
            progress >= 50 -> "Halfway there! Keep going! 🚀"
            progress >= 25 -> "Good start! Stay hydrated! 💧"
            currentGlasses > 0 -> "Every drop counts! Keep it up! 🌱"
            else -> "Let's start your hydration journey! 💧"
        }
        
        motivationalMessage.text = messages
    }
    
    private fun checkAchievements() {
        val progress = if (dailyGoal > 0) {
            (currentGlasses.toFloat() / dailyGoal.toFloat()) * 100f
        } else {
            0f
        }
        
        if (progress >= 100 && achievementBadge.visibility != View.VISIBLE) {
            achievementBadge.visibility = View.VISIBLE
            achievementText.text = "Goal Reached! 🎉"
            
            // Animate achievement badge
            achievementBadge.alpha = 0f
            achievementBadge.animate()
                .alpha(1f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(300)
                .withEndAction {
                    achievementBadge.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        } else if (progress < 100) {
            achievementBadge.visibility = View.GONE
        }
    }
    
    private fun updateAnalytics() {
        // Calculate weekly total
        val weeklyTotalGlasses = calculateWeeklyTotal()
        weeklyTotal.text = "$weeklyTotalGlasses glasses this week"
        
        // Calculate streak
        val streak = calculateStreak()
        streakCount.text = streak.toString()
        
        // Calculate total goals achieved
        val goalsMet = calculateGoalsAchieved()
        goalsAchieved.text = goalsMet.toString()
        
        // Calculate total water consumed
        val totalGlasses = calculateTotalWaterConsumed()
        totalWater.text = totalGlasses.toString()
        
        // Update weekly progress bars
        updateWeeklyProgressBars()
        
        // Update smart suggestions
        updateSmartSuggestions()
    }
    
    private fun calculateWeeklyTotal(): Int {
        // Use PreferencesManager to get weekly water consumption
        val weeklyData = preferencesManager.getWeeklyWaterConsumption()
        return weeklyData.values.sum()
    }
    
    private fun calculateStreak(): Int {
        // Use PreferencesManager to calculate water consumption streak
        return preferencesManager.getWaterConsumptionStreak()
    }
    
    private fun calculateGoalsAchieved(): Int {
        // Use PreferencesManager to get all water entries and count goals achieved
        val waterEntries = preferencesManager.getAllWaterEntries()
        val dailyGoal = preferencesManager.getDailyWaterGoal()
        
        // Group entries by date and count days where goal was met
        val dailyTotals = waterEntries.groupBy { it.date }
            .mapValues { (_, entries) -> entries.sumOf { it.glasses } }
        
        return dailyTotals.count { (_, total) -> total >= dailyGoal }
    }
    
    private fun calculateTotalWaterConsumed(): Int {
        // Use PreferencesManager to get all water entries and calculate total
        val waterEntries = preferencesManager.getAllWaterEntries()
        return waterEntries.sumOf { it.glasses }
    }
    
    private fun updateWeeklyProgressBars() {
        val progressBars = listOf(
            findViewById<ProgressBar>(R.id.monday_progress),
            findViewById<ProgressBar>(R.id.tuesday_progress),
            findViewById<ProgressBar>(R.id.wednesday_progress),
            findViewById<ProgressBar>(R.id.thursday_progress),
            findViewById<ProgressBar>(R.id.friday_progress),
            findViewById<ProgressBar>(R.id.saturday_progress),
            findViewById<ProgressBar>(R.id.sunday_progress)
        )
        
        // Get daily consumption for the current week using PreferencesManager
        val weeklyData = preferencesManager.getWeeklyWaterConsumption()
        val calendar = Calendar.getInstance()
        
        // Get the start of the current week (Monday)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        
        progressBars.forEachIndexed { index, progressBar ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val consumption = weeklyData[date] ?: 0
            progressBar.max = dailyGoal
            progressBar.progress = consumption
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    
    private fun startFloatingAnimations() {
        // Animate floating water drops
        floatingDrop1.animate()
            .translationY(-20f)
            .alpha(0.3f)
            .setDuration(2000)
            .withEndAction {
                floatingDrop1.animate()
                    .translationY(0f)
                    .alpha(0.6f)
                    .setDuration(2000)
                    .start()
            }
            .start()
        
        floatingDrop2.animate()
            .translationY(15f)
            .alpha(0.2f)
            .setDuration(2500)
            .withEndAction {
                floatingDrop2.animate()
                    .translationY(0f)
                    .alpha(0.4f)
                    .setDuration(2500)
                    .start()
            }
            .start()
        
        // Animate water drop icon
        waterDropIcon.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(1000)
            .withEndAction {
                waterDropIcon.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1000)
                    .start()
            }
            .start()
    }
    
    private fun updateSmartSuggestions() {
        val suggestions = reminderManager.getSmartReminderSuggestions()
        
        // Update suggestion count
        suggestionCount.text = "${suggestions.size} tips"
        
        // Update suggestion texts
        suggestion1.text = suggestions.getOrElse(0) { "Start your day with a glass of water" }
        suggestion2.text = suggestions.getOrElse(1) { "Set reminders every 2 hours" }
        suggestion3.text = suggestions.getOrElse(2) { "Keep a water bottle nearby" }
        
        // Show optimal reminder interval suggestion
        val optimalInterval = reminderManager.getOptimalReminderInterval()
        if (reminderManager.needsMoreFrequentReminders()) {
            suggestion2.text = "Set reminders every ${optimalInterval} minutes"
        }
    }
}