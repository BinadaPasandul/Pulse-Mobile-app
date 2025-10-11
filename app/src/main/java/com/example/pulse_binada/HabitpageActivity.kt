package com.example.pulse_binada

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

class HabitpageActivity : AppCompatActivity() {
    
    private lateinit var habitsContainer: LinearLayout
    private lateinit var addHabitButton: LinearLayout
    private lateinit var emptyHabitsState: LinearLayout
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentage: TextView
    private lateinit var progressText: TextView
    private lateinit var currentMoodSection: LinearLayout
    private lateinit var currentMoodEmoji: TextView
    private lateinit var currentMoodName: TextView
    private lateinit var currentMoodTime: TextView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today = dateFormat.format(Date())
    
    // Data model for habits
    data class Habit(
        val id: String,
        val text: String,
        val isCompleted: Boolean,
        val createdAt: String
    )
    
    // Data model for mood entries (matching MoodpageActivity)
    data class MoodEntry(
        val id: String,
        val emoji: String,
        val mood: String,
        val date: String,
        val time: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.habitpage)
        
        // Initialize PreferencesManager for data persistence
        preferencesManager = PreferencesManager.getInstance(this)
        
        // Initialize views
        initializeViews()
        
        // Set up click listeners
        setupClickListeners()
        
        // Set up navigation listeners
        setupNavigationListeners()
        
        // Load and display habits
        loadAndDisplayHabits()
        
        // Update progress initially
        updateProgress()
        
        // Update weekly progress initially
        updateWeeklyProgressBars()
        
        // Load and display current mood
        loadAndDisplayCurrentMood()
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    private fun initializeViews() {
        habitsContainer = findViewById(R.id.habits_container)
        addHabitButton = findViewById(R.id.add_habit_button)
        emptyHabitsState = findViewById(R.id.empty_habits_state)
        progressBar = findViewById(R.id.progress_bar)
        progressPercentage = findViewById(R.id.progress_percentage)
        progressText = findViewById(R.id.progress_text)
        currentMoodSection = findViewById(R.id.current_mood_section)
        currentMoodEmoji = findViewById(R.id.current_mood_emoji)
        currentMoodName = findViewById(R.id.current_mood_name)
        currentMoodTime = findViewById(R.id.current_mood_time)
    }
    
    private fun setupClickListeners() {
        addHabitButton.setOnClickListener {
            showAddHabitDialog()
        }
    }
    
    private fun setupNavigationListeners() {
        // Home navigation (current page - this IS the home page)
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            // Already on home page - just show feedback
            showToast("You're already on the Home page! 🏠")
        }
        
        // Mood navigation
        findViewById<LinearLayout>(R.id.nav_mood).setOnClickListener {
            val intent = Intent(this, MoodpageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        
        // Add Habit navigation (current page - trigger add habit dialog)
        findViewById<LinearLayout>(R.id.nav_add_habit).setOnClickListener {
            // Already on habit page - trigger the add habit dialog
            showAddHabitDialog()
        }
        
        // Water navigation
        findViewById<LinearLayout>(R.id.nav_water).setOnClickListener {
            val intent = Intent(this, WaterpageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        
        // Settings navigation
        findViewById<LinearLayout>(R.id.nav_settings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_habit, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.et_habit_text)
        val addButton = dialogView.findViewById<Button>(R.id.btn_add_habit)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)
        
        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add New Habit")
            .create()
        
        // Enable/disable add button based on text input
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                addButton.isEnabled = !s.isNullOrBlank()
            }
        })
        
        addButton.setOnClickListener {
            val habitText = editText.text.toString().trim()
            if (habitText.isNotEmpty()) {
                addHabit(habitText)
                dialog.dismiss()
            }
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Initially disable add button
        addButton.isEnabled = false
        
        dialog.show()
    }
    //add habit function
    private fun addHabit(habitText: String) {
        val habitId = UUID.randomUUID().toString()
        val habit = Habit(habitId, habitText, false, today)
        
        // Save habit to SharedPreferences
        saveHabit(habit)
        

        addHabitToUI(habit)


        emptyHabitsState.visibility = View.GONE

        updateProgress()

        updateWeeklyProgressBars()
    }

    //save the habits in to storage
    private fun saveHabit(habit: Habit) {
        // Convert to PreferencesManager.HabitEntry format
        val habitEntry = PreferencesManager.HabitEntry(
            id = habit.id,
            text = habit.text,
            isCompleted = habit.isCompleted,
            createdAt = habit.createdAt
        )
        
        // Save using PreferencesManager
        preferencesManager.saveHabit(habitEntry)
    }
    
    private fun addHabitToUI(habit: Habit) {
        val habitView = createHabitView(habit)
        habitsContainer.addView(habitView, 0) // Add at the beginning
    }
    
    private fun createHabitView(habit: Habit): View {
        val habitView = LayoutInflater.from(this).inflate(R.layout.habit_item, habitsContainer, false)
        
        val checkbox = habitView.findViewById<CheckBox>(R.id.habit_checkbox)
        val habitText = habitView.findViewById<TextView>(R.id.habit_text)
        val editButton = habitView.findViewById<ImageView>(R.id.habit_edit)
        val deleteButton = habitView.findViewById<ImageView>(R.id.habit_delete)
        
        habitText.text = habit.text
        checkbox.isChecked = habit.isCompleted
        
        // checkbox listener
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            updateHabitCompletion(habit.id, isChecked)
            habitText.alpha = if (isChecked) 0.7f else 1.0f
            updateProgress()
        }
        
        // edit button listener
        editButton.setOnClickListener {
            showEditHabitDialog(habit)
        }
        
        // delete button listener
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog(habit.id, habitView)
        }
        

        habitView.setOnLongClickListener {
            showEditHabitDialog(habit)
            true
        }
        

        habitText.alpha = if (habit.isCompleted) 0.7f else 1.0f
        
        return habitView
    }


    //update habit
    private fun updateHabitCompletion(habitId: String, isCompleted: Boolean) {
        // Get all habits and find the one to update
        val habits = preferencesManager.getAllHabits()
        val habitToUpdate = habits.find { it.id == habitId }
        
        if (habitToUpdate != null) {
            // Create updated habit entry
            val updatedHabit = habitToUpdate.copy(
                isCompleted = isCompleted,
                completedAt = if (isCompleted) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()) else null
            )
            
            // Save the updated habit
            preferencesManager.saveHabit(updatedHabit)
            
            // Update weekly progress after habit completion change
            updateWeeklyProgressBars()
        }
    }
    
    private fun showDeleteConfirmationDialog(habitId: String, habitView: View) {
        AlertDialog.Builder(this)
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete this habit?")
            .setPositiveButton("Delete") { _, _ ->
                deleteHabit(habitId, habitView)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //delete habit
    private fun deleteHabit(habitId: String, habitView: View) {
        // Use PreferencesManager to delete the habit
        preferencesManager.deleteHabit(habitId)
        
        habitsContainer.removeView(habitView)
        
        // Update progress after deletion
        updateProgress()
        
        // Update weekly progress after deletion
        updateWeeklyProgressBars()
        
        // Show empty state if no habits left
        if (habitsContainer.childCount == 1) { // Only empty state view left
            emptyHabitsState.visibility = View.VISIBLE
        }
    }


    //Display habits
    private fun loadAndDisplayHabits() {
        // Use PreferencesManager to get all habits
        val habits = preferencesManager.getAllHabits()
        
        if (habits.isEmpty()) {
            emptyHabitsState.visibility = View.VISIBLE
            return
        }
        
        emptyHabitsState.visibility = View.GONE
        
        habitsContainer.removeAllViews()
        habitsContainer.addView(emptyHabitsState) // Add empty state back
        
        // Add each habit to UI
        habits.forEach { habitEntry ->
            val habit = Habit(
                habitEntry.id,
                habitEntry.text,
                habitEntry.isCompleted,
                habitEntry.createdAt
            )
            addHabitToUI(habit)
        }
    }

    //progress updating and showing the progression
    private fun updateProgress() {
        // Use PreferencesManager to get today's habits
        val todayHabits = preferencesManager.getHabitsForDate(today)
        
        if (todayHabits.isEmpty()) {
            // No habits
            progressBar.progress = 0
            progressPercentage.text = "0%"
            progressText.text = "0 of 0 habits completed"
            return
        }
        
        val completedCount = todayHabits.count { it.isCompleted }
        val totalHabits = todayHabits.size
        
        // Calculate percentage
        val percentage = if (totalHabits > 0) {
            (completedCount * 100) / totalHabits
        } else {
            0
        }
        
        // Update UI
        progressBar.progress = percentage
        progressPercentage.text = "$percentage%"
        progressText.text = "$completedCount of $totalHabits habits completed"
        
        // motivational messages based on progress
        when {
            percentage == 100 && totalHabits > 0 -> {
                progressText.text = "🎉 All habits completed! Amazing!"
                progressText.setTextColor(getColor(R.color.primary_color))
            }
            percentage >= 75 -> {
                progressText.text = "$completedCount of $totalHabits habits completed - Almost there!"
                progressText.setTextColor(getColor(R.color.primary_color))
            }
            percentage >= 50 -> {
                progressText.text = "$completedCount of $totalHabits habits completed - Great progress!"
                progressText.setTextColor(getColor(R.color.primary_color))
            }
            else -> {
                progressText.text = "$completedCount of $totalHabits habits completed"
                progressText.setTextColor(getColor(android.R.color.white))
            }
        }
    }
    
    private fun showEditHabitDialog(habit: Habit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_habit, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.et_edit_habit_text)
        val saveButton = dialogView.findViewById<Button>(R.id.btn_save_habit)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel_edit)
        
        // Pre-fill the text with current habit text
        editText.setText(habit.text)
        editText.setSelection(habit.text.length) // Place cursor at end
        
        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Edit Habit")
            .create()
        

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newText = s.toString().trim()
                saveButton.isEnabled = newText.isNotEmpty() && newText != habit.text
            }
        })
        
        saveButton.setOnClickListener {
            val newHabitText = editText.text.toString().trim()
            if (newHabitText.isNotEmpty() && newHabitText != habit.text) {
                updateHabitText(habit.id, newHabitText)
                dialog.dismiss()
            }
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        

        saveButton.isEnabled = false
        
        dialog.show()
    }
    
    private fun updateHabitText(habitId: String, newText: String) {
        // Get all habits and find the one to update
        val habits = preferencesManager.getAllHabits()
        val habitToUpdate = habits.find { it.id == habitId }
        
        if (habitToUpdate != null) {
            // Create updated habit entry
            val updatedHabit = habitToUpdate.copy(text = newText)
            
            // Save the updated habit
            preferencesManager.saveHabit(updatedHabit)
            
            // Refresh the UI to show updated text
            loadAndDisplayHabits()
            
            // Show success message
            showToast("Habit updated successfully")
        }
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
        
        // Get daily completion percentages for the current week
        val dailyCompletion = getWeeklyDailyCompletion()
        
        progressBars.forEachIndexed { index, progressBar ->
            val completionPercentage = dailyCompletion.getOrElse(index) { 0 }
            progressBar.progress = completionPercentage
        }
    }
    
    private fun getWeeklyDailyCompletion(): List<Int> {
        // Use PreferencesManager to get weekly habit completion data
        val weeklyData = preferencesManager.getWeeklyHabitCompletion()
        
        // Convert to list format expected by the UI
        val calendar = Calendar.getInstance()
        val completionList = mutableListOf<Int>()
        
        // Get the start of the current week (Monday)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        
        for (i in 0 until 7) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val completion = weeklyData[date]?.toInt() ?: 0
            completionList.add(completion)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return completionList
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    // Mood-related methods
    private fun loadAndDisplayCurrentMood() {
        // Use PreferencesManager to get today's latest mood
        val todayMood = preferencesManager.getTodayLatestMood()
        
        if (todayMood != null) {
            displayCurrentMood(todayMood)
        } else {
            currentMoodSection.visibility = View.GONE
        }
    }
    
    private fun displayCurrentMood(moodEntry: PreferencesManager.MoodEntry) {
        currentMoodEmoji.text = moodEntry.emoji
        currentMoodName.text = moodEntry.mood
        currentMoodTime.text = formatTimeForDisplay(moodEntry.time)
        currentMoodSection.visibility = View.VISIBLE
    }
    
    private fun formatTimeForDisplay(timeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val time = inputFormat.parse(timeString)
            outputFormat.format(time ?: Date())
        } catch (e: Exception) {
            timeString
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh mood display
        loadAndDisplayCurrentMood()
    }
}