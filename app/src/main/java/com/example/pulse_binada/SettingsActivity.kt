package com.example.pulse_binada

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.settings)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Set up navigation listeners
        setupNavigationListeners()
    }
    
    private fun setupNavigationListeners() {
        // Home navigation
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this, HabitpageActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Mood navigation
        findViewById<LinearLayout>(R.id.nav_mood).setOnClickListener {
            val intent = Intent(this, MoodpageActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Add habit navigation
        findViewById<LinearLayout>(R.id.nav_add_habit).setOnClickListener {
            val intent = Intent(this, HabitpageActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Water navigation
        findViewById<LinearLayout>(R.id.nav_water).setOnClickListener {
            val intent = Intent(this, WaterpageActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Settings navigation (current page, no action needed)
        findViewById<LinearLayout>(R.id.nav_settings).setOnClickListener {
            // Already on settings page
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}