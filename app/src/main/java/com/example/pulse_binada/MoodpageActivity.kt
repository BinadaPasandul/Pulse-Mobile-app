package com.example.pulse_binada

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MoodpageActivity : AppCompatActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var currentDateTextView: TextView
    private lateinit var selectedMoodDisplay: LinearLayout
    private lateinit var selectedEmojiTextView: TextView
    private lateinit var selectedMoodNameTextView: TextView
    private lateinit var saveMoodButton: Button
    private lateinit var moodHistoryContainer: LinearLayout
    private lateinit var emptyMoodState: LinearLayout
    
    private var selectedMood: String? = null
    private var selectedEmoji: String? = null
    
    // Data model for mood entries
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
        setContentView(R.layout.moodpage)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize PreferencesManager
        preferencesManager = PreferencesManager.getInstance(this)
        
        // Initialize views
        initializeViews()
        
        // Set up current date
        setupCurrentDate()
        
        // Set up emoji selectors
        setupEmojiSelectors()
        
        // Set up save button
        setupSaveButton()
        
        // Load and display mood history
        loadAndDisplayMoodHistory()
        
        // Set up navigation listeners
        setupNavigationListeners()
    }
    
    private fun initializeViews() {
        currentDateTextView = findViewById(R.id.current_date)
        selectedMoodDisplay = findViewById(R.id.selected_mood_display)
        selectedEmojiTextView = findViewById(R.id.selected_emoji)
        selectedMoodNameTextView = findViewById(R.id.selected_mood_name)
        saveMoodButton = findViewById(R.id.save_mood_button)
        moodHistoryContainer = findViewById(R.id.mood_history_container)
        emptyMoodState = findViewById(R.id.empty_mood_state)
    }
    
    private fun setupCurrentDate() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())
        currentDateTextView.text = today
    }
    
    private fun setupEmojiSelectors() {
        // Map of emoji IDs to mood data
        val emojiMoods = mapOf(
            R.id.emoji_happy to Pair("😊", "Happy"),
            R.id.emoji_excited to Pair("🤩", "Excited"),
            R.id.emoji_neutral to Pair("😐", "Neutral"),
            R.id.emoji_sad to Pair("😢", "Sad"),
            R.id.emoji_stressed to Pair("😰", "Stressed"),
            R.id.emoji_grateful to Pair("🙏", "Grateful"),
            R.id.emoji_tired to Pair("😴", "Tired"),
            R.id.emoji_angry to Pair("😠", "Angry"),
            R.id.emoji_anxious to Pair("😟", "Anxious"),
            R.id.emoji_peaceful to Pair("😌", "Peaceful")
        )
        
        emojiMoods.forEach { (emojiId, moodData) ->
            findViewById<LinearLayout>(emojiId).setOnClickListener {
                selectMood(moodData.first, moodData.second)
            }
        }
    }
    
    private fun selectMood(emoji: String, mood: String) {
        selectedEmoji = emoji
        selectedMood = mood
        
        // Update display
        selectedEmojiTextView.text = emoji
        selectedMoodNameTextView.text = mood
        selectedMoodDisplay.visibility = View.VISIBLE
        
        // Reset all emoji backgrounds
        resetEmojiBackgrounds()
        
        // Highlight selected emoji
        val emojiMoods = mapOf(
            "😊" to R.id.emoji_happy,
            "🤩" to R.id.emoji_excited,
            "😐" to R.id.emoji_neutral,
            "😢" to R.id.emoji_sad,
            "😰" to R.id.emoji_stressed,
            "🙏" to R.id.emoji_grateful,
            "😴" to R.id.emoji_tired,
            "😠" to R.id.emoji_angry,
            "😟" to R.id.emoji_anxious,
            "😌" to R.id.emoji_peaceful
        )
        
        emojiMoods[emoji]?.let { emojiId ->
            findViewById<LinearLayout>(emojiId).setBackgroundColor(
                resources.getColor(android.R.color.white, theme).apply { 
                    // Add some transparency
                }
            )
        }
    }
    
    private fun resetEmojiBackgrounds() {
        val emojiIds = listOf(
            R.id.emoji_happy, R.id.emoji_excited, R.id.emoji_neutral, R.id.emoji_sad,
            R.id.emoji_stressed, R.id.emoji_grateful, R.id.emoji_tired, R.id.emoji_angry,
            R.id.emoji_anxious, R.id.emoji_peaceful
        )
        
        emojiIds.forEach { emojiId ->
            findViewById<LinearLayout>(emojiId).setBackgroundResource(0)
        }
    }
    
    private fun setupSaveButton() {
        saveMoodButton.setOnClickListener {
            if (selectedMood != null && selectedEmoji != null) {
                saveMoodEntry(selectedEmoji!!, selectedMood!!)
            }
        }
    }
    
    private fun saveMoodEntry(emoji: String, mood: String) {
        val moodId = UUID.randomUUID().toString()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()
        
        val moodEntry = MoodEntry(
            moodId,
            emoji,
            mood,
            dateFormat.format(now),
            timeFormat.format(now)
        )
        
        // Save to SharedPreferences
        saveMoodToPreferences(moodEntry)
        
        // Add to UI
        addMoodEntryToUI(moodEntry)
        
        // Hide empty state
        emptyMoodState.visibility = View.GONE
        
        // Reset selection
        selectedMoodDisplay.visibility = View.GONE
        selectedMood = null
        selectedEmoji = null
        resetEmojiBackgrounds()
        
        // Show success message
        Toast.makeText(this, "Mood saved successfully!", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveMoodToPreferences(moodEntry: MoodEntry) {
        // Convert to PreferencesManager.MoodEntry format
        val preferencesMoodEntry = PreferencesManager.MoodEntry(
            id = moodEntry.id,
            emoji = moodEntry.emoji,
            mood = moodEntry.mood,
            date = moodEntry.date,
            time = moodEntry.time
        )
        
        // Save using PreferencesManager
        preferencesManager.saveMoodEntry(preferencesMoodEntry)
    }
    
    private fun addMoodEntryToUI(moodEntry: MoodEntry) {
        val moodView = createMoodEntryView(moodEntry)
        moodHistoryContainer.addView(moodView, 0) // Add at the beginning
    }
    
    private fun createMoodEntryView(moodEntry: MoodEntry): View {
        val moodView = LayoutInflater.from(this).inflate(R.layout.mood_entry_item, moodHistoryContainer, false)
        
        val emojiTextView = moodView.findViewById<TextView>(R.id.mood_entry_emoji)
        val moodNameTextView = moodView.findViewById<TextView>(R.id.mood_entry_name)
        val dateTextView = moodView.findViewById<TextView>(R.id.mood_entry_date)
        val timeTextView = moodView.findViewById<TextView>(R.id.mood_entry_time)
        val deleteButton = moodView.findViewById<ImageView>(R.id.mood_entry_delete)
        
        emojiTextView.text = moodEntry.emoji
        moodNameTextView.text = moodEntry.mood
        dateTextView.text = formatDateForDisplay(moodEntry.date)
        timeTextView.text = moodEntry.time
        
        // Set up delete button click listener
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog(moodEntry)
        }
        
        // Add long press for additional delete option
        moodView.setOnLongClickListener {
            showDeleteConfirmationDialog(moodEntry)
            true
        }
        
        return moodView
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
    
    private fun loadAndDisplayMoodHistory() {
        // Use PreferencesManager to get all mood entries
        val moodEntries = preferencesManager.getAllMoodEntries()
        
        if (moodEntries.isEmpty()) {
            emptyMoodState.visibility = View.VISIBLE
            return
        }
        
        emptyMoodState.visibility = View.GONE
        
        // Clear existing views (keep empty state)
        moodHistoryContainer.removeAllViews()
        moodHistoryContainer.addView(emptyMoodState) // Add empty state back
        
        // Add each mood entry to UI (most recent first)
        moodEntries.forEach { moodEntry ->
            val localMoodEntry = MoodEntry(
                moodEntry.id,
                moodEntry.emoji,
                moodEntry.mood,
                moodEntry.date,
                moodEntry.time
            )
            addMoodEntryToUI(localMoodEntry)
        }
    }
    
    private fun setupNavigationListeners() {
        // Home navigation - go to HabitpageActivity (main home)
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this, HabitpageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        
        // Mood navigation (current page - do nothing or show feedback)
        findViewById<LinearLayout>(R.id.nav_mood).setOnClickListener {
            // Already on mood page - just show feedback
            showToast("You're already on the Mood page! 😊")
        }
        
        // Add Habit navigation - go to HabitpageActivity
        findViewById<LinearLayout>(R.id.nav_add_habit).setOnClickListener {
            val intent = Intent(this, HabitpageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
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
    
    private fun showDeleteConfirmationDialog(moodEntry: MoodEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete Mood Entry")
            .setMessage("Are you sure you want to delete this mood entry?\n\n${moodEntry.emoji} ${moodEntry.mood} - ${formatDateForDisplay(moodEntry.date)} at ${moodEntry.time}")
            .setPositiveButton("Delete") { _, _ ->
                deleteMoodEntry(moodEntry)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
    
    private fun deleteMoodEntry(moodEntry: MoodEntry) {
        // Use PreferencesManager to delete the mood entry
        preferencesManager.deleteMoodEntry(moodEntry.id)
        
        // Refresh the UI
        loadAndDisplayMoodHistory()
        
        // Show confirmation message
        showToast("Mood entry deleted successfully")
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}