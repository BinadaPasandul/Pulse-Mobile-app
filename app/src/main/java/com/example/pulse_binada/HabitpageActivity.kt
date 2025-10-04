package com.example.pulse_binada

import android.content.Context
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

class HabitpageActivity : AppCompatActivity() {
    
    private lateinit var habitsContainer: LinearLayout
    private lateinit var addHabitButton: LinearLayout
    private lateinit var emptyHabitsState: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentage: TextView
    private lateinit var progressText: TextView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today = dateFormat.format(Date())
    
    // Data model for habits
    data class Habit(
        val id: String,
        val text: String,
        val isCompleted: Boolean,
        val createdAt: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.habitpage)
        
        // Initialize SharedPreferences for data persistence
        sharedPreferences = getSharedPreferences("habit_tracker", Context.MODE_PRIVATE)
        
        // Initialize views
        initializeViews()
        
        // Set up click listeners
        setupClickListeners()
        
        // Load and display habits
        loadAndDisplayHabits()
        
        // Update progress initially
        updateProgress()
        
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
    }
    
    private fun setupClickListeners() {
        addHabitButton.setOnClickListener {
            showAddHabitDialog()
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
    
    private fun addHabit(habitText: String) {
        val habitId = UUID.randomUUID().toString()
        val habit = Habit(habitId, habitText, false, today)
        
        // Save habit to SharedPreferences
        saveHabit(habit)
        
        // Add habit to UI
        addHabitToUI(habit)
        
        // Hide empty state if it was visible
        emptyHabitsState.visibility = View.GONE
        
        // Update progress after adding new habit
        updateProgress()
    }
    
    private fun saveHabit(habit: Habit) {
        val habitsJson = sharedPreferences.getString("habits", "[]") ?: "[]"
        val habitsArray = JSONArray(habitsJson)
        
        val habitJson = JSONObject().apply {
            put("id", habit.id)
            put("text", habit.text)
            put("isCompleted", habit.isCompleted)
            put("createdAt", habit.createdAt)
            put("completedDates", JSONArray()) // Track completion dates
        }
        
        habitsArray.put(habitJson)
        
        sharedPreferences.edit()
            .putString("habits", habitsArray.toString())
            .apply()
    }
    
    private fun addHabitToUI(habit: Habit) {
        val habitView = createHabitView(habit)
        habitsContainer.addView(habitView, 0) // Add at the beginning
    }
    
    private fun createHabitView(habit: Habit): View {
        val habitView = LayoutInflater.from(this).inflate(R.layout.habit_item, habitsContainer, false)
        
        val checkbox = habitView.findViewById<CheckBox>(R.id.habit_checkbox)
        val habitText = habitView.findViewById<TextView>(R.id.habit_text)
        val deleteButton = habitView.findViewById<ImageView>(R.id.habit_delete)
        
        habitText.text = habit.text
        checkbox.isChecked = habit.isCompleted
        
        // Set up checkbox listener
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            updateHabitCompletion(habit.id, isChecked)
            habitText.alpha = if (isChecked) 0.7f else 1.0f
            updateProgress() // Update progress when habit is completed/uncompleted
        }
        
        // Set up delete button listener
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog(habit.id, habitView)
        }
        
        // Set initial text opacity based on completion status
        habitText.alpha = if (habit.isCompleted) 0.7f else 1.0f
        
        return habitView
    }
    
    private fun updateHabitCompletion(habitId: String, isCompleted: Boolean) {
        val habitsJson = sharedPreferences.getString("habits", "[]") ?: "[]"
        val habitsArray = JSONArray(habitsJson)
        
        for (i in 0 until habitsArray.length()) {
            val habitJson = habitsArray.getJSONObject(i)
            if (habitJson.getString("id") == habitId) {
                habitJson.put("isCompleted", isCompleted)
                
                // Track completion dates
                val completedDates = habitJson.getJSONArray("completedDates")
                if (isCompleted) {
                    if (!completedDates.toString().contains(today)) {
                        completedDates.put(today)
                    }
                } else {
                    // Remove today's completion if unchecked
                    val newArray = JSONArray()
                    for (j in 0 until completedDates.length()) {
                        if (completedDates.getString(j) != today) {
                            newArray.put(completedDates.getString(j))
                        }
                    }
                    habitJson.put("completedDates", newArray)
                }
                break
            }
        }
        
        sharedPreferences.edit()
            .putString("habits", habitsArray.toString())
            .apply()
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
    
    private fun deleteHabit(habitId: String, habitView: View) {
        val habitsJson = sharedPreferences.getString("habits", "[]") ?: "[]"
        val habitsArray = JSONArray(habitsJson)
        
        for (i in 0 until habitsArray.length()) {
            val habitJson = habitsArray.getJSONObject(i)
            if (habitJson.getString("id") == habitId) {
                habitsArray.remove(i)
                break
            }
        }
        
        sharedPreferences.edit()
            .putString("habits", habitsArray.toString())
            .apply()
        
        habitsContainer.removeView(habitView)
        
        // Update progress after deletion
        updateProgress()
        
        // Show empty state if no habits left
        if (habitsContainer.childCount == 1) { // Only empty state view left
            emptyHabitsState.visibility = View.VISIBLE
        }
    }
    
    private fun loadAndDisplayHabits() {
        val habitsJson = sharedPreferences.getString("habits", "[]") ?: "[]"
        val habitsArray = JSONArray(habitsJson)
        
        if (habitsArray.length() == 0) {
            emptyHabitsState.visibility = View.VISIBLE
            return
        }
        
        emptyHabitsState.visibility = View.GONE
        
        // Clear existing habit views (keep empty state)
        habitsContainer.removeAllViews()
        habitsContainer.addView(emptyHabitsState) // Add empty state back
        
        // Add each habit to UI
        for (i in 0 until habitsArray.length()) {
            val habitJson = habitsArray.getJSONObject(i)
            val habit = Habit(
                habitJson.getString("id"),
                habitJson.getString("text"),
                habitJson.getBoolean("isCompleted"),
                habitJson.getString("createdAt")
            )
            addHabitToUI(habit)
        }
    }
    
    private fun updateProgress() {
        val habitsJson = sharedPreferences.getString("habits", "[]") ?: "[]"
        val habitsArray = JSONArray(habitsJson)
        
        if (habitsArray.length() == 0) {
            // No habits - show 0%
            progressBar.progress = 0
            progressPercentage.text = "0%"
            progressText.text = "0 of 0 habits completed"
            return
        }
        
        var completedCount = 0
        val totalHabits = habitsArray.length()
        
        for (i in 0 until habitsArray.length()) {
            val habitJson = habitsArray.getJSONObject(i)
            val isCompleted = habitJson.getBoolean("isCompleted")
            if (isCompleted) {
                completedCount++
            }
        }
        
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
        
        // Add some motivational messages based on progress
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
}