package com.example.mountainpassport_girarifugi

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private val splashDelay = 1500L             // 1.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide action bar
        supportActionBar?.hide()

        // TODO SPLASH ACTIVITY LAYOUT!
        setContentView(R.layout.activity_splash)

        firebaseAuth = FirebaseAuth.getInstance()

        // Add a delay for splash activity screen, then check authentication
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationStatus()
        }, splashDelay)
    }

    private fun checkAuthenticationStatus() {
        val currentUser = firebaseAuth.currentUser

        if (currentUser != null) {
            // User already logged in, navigate to MainActivity
            navigateToMainActivity()
        } else {
            // User not logged in, navigate to SignInActivity
            navigateToSignInActivity()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToSignInActivity() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}