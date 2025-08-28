package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mountainpassport_girarifugi.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        val type = intent.getStringExtra("TYPE") ?: "USER"

        if (type == "USER") {
            setContentView(R.layout.fragment_profile_leaderboard) // layout utente
            val name = intent.getStringExtra("USER_NAME") ?: "Nome sconosciuto"
            val username = intent.getStringExtra("USER_USERNAME") ?: ""
            val points = intent.getIntExtra("USER_POINTS", 0)
            val refuges = intent.getIntExtra("USER_REFUGES", 0)
            val avatar = intent.getIntExtra("USER_AVATAR", R.drawable.avatar_sara)

            // Popola le view
            findViewById<TextView>(R.id.fullNameTextView).text = name
            findViewById<TextView>(R.id.usernameTextView).text = username
            findViewById<TextView>(R.id.monthlyScoreTextView).text = "$points"
            findViewById<TextView>(R.id.visitedRefugesTextView).text = "$refuges"
            findViewById<ImageView>(R.id.profileImageView).setImageResource(avatar)
        } else if (type == "GROUP") {
            setContentView(R.layout.fragment_profile_group_leaderboard) // layout gruppo
            val name = intent.getStringExtra("USER_NAME") ?: "Nome sconosciuto"
            val username = intent.getStringExtra("USER_USERNAME") ?: ""
            val points = intent.getIntExtra("USER_POINTS", 0)
            val refuges = intent.getIntExtra("USER_REFUGES", 0)
            val avatar = intent.getIntExtra("USER_AVATAR", R.drawable.avatar_sara)

            // Popola le view
            findViewById<TextView>(R.id.fullNameTextView).text = name
            findViewById<TextView>(R.id.usernameTextView).text = username
            findViewById<TextView>(R.id.monthlyScoreTextView).text = "$points"
            findViewById<TextView>(R.id.visitedRefugesTextView).text = "$refuges"
            findViewById<ImageView>(R.id.profileImageView).setImageResource(avatar)
        }

        setupClickListeners()

    }

    private fun setupClickListeners() {
        // Back button
        findViewById<FloatingActionButton>(R.id.fabBack).setOnClickListener {
            finish()
        }
    }
}