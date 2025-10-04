package com.example.pulse_binada

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button

class Onboarding1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.onboarding1)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get button references
        val nextButton = findViewById<Button>(R.id.nextButton)
        val skipButton = findViewById<Button>(R.id.skipButton)

        // Navigate to Onboarding2Activity when clicking "Next"
        nextButton.setOnClickListener {
            val intent = Intent(this, Onboarding2Activity::class.java)
            startActivity(intent)
        }

        // (Optional) You can decide what "Skip" should do later (e.g., go to Home or Login)
        skipButton.setOnClickListener {

             val intent = Intent(this, HabitpageActivity::class.java)
             startActivity(intent)
            finish()
        }
    }
}
