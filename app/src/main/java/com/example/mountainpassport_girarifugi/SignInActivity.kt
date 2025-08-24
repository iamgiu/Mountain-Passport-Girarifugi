package com.example.mountainpassport_girarifugi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mountainpassport_girarifugi.databinding.ActivitySignInBinding
import com.example.mountainpassport_girarifugi.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide action bar for full screen experience
        supportActionBar?.hide()

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Navigate to Sign Up
        binding.signUpText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Sign In button click
        binding.signInButton.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val password = binding.passwordEt.text.toString().trim()

            if (validateInput(email, password)) {
                signInUser(email, password)
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.emailLayout.error = "Email richiesta"
                false
            }
            password.isEmpty() -> {
                binding.passwordLayout.error = "Password richiesta"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailLayout.error = "Inserisci un'email valida"
                false
            }
            else -> {
                binding.emailLayout.error = null
                binding.passwordLayout.error = null
                true
            }
        }
    }

    private fun signInUser(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Accesso effettuato con successo!", Toast.LENGTH_SHORT).show()
                    checkUserProfile()
                } else {
                    val errorMessage = task.exception?.message ?: "Accesso non riuscito"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkUserProfile() {
        val currentUser = firebaseAuth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null && user.isProfileComplete()) {
                        // Profile is complete, go to main activity
                        navigateToMainActivity()
                    } else {
                        // Profile is incomplete, go to profile setup
                        navigateToProfileSetup()
                    }
                } else {
                    // User document doesn't exist, create profile
                    navigateToProfileSetup()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Errore nel verificare il profilo: ${e.message}", Toast.LENGTH_SHORT).show()
                // In case of error, go to profile setup
                navigateToProfileSetup()
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToProfileSetup() {
        val intent = Intent(this, ProfileSetupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}