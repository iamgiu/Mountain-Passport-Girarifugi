package com.example.mountainpassport_girarifugi.user

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mountainpassport_girarifugi.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide action bar for full screen experience
        supportActionBar?.hide()

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Navigate to Sign In
        binding.signInText.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        // Sign Up button click
        binding.signUpButton.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val password = binding.passwordEt.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEt.text.toString().trim()

            if (validateInput(email, password, confirmPassword)) {
                createUserAccount(email, password)
            }
        }
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.emailLayout.error = "Email è richiesta"
                false
            }
            password.isEmpty() -> {
                binding.passwordLayout.error = "Password è richiesta"
                false
            }
            confirmPassword.isEmpty() -> {
                binding.confirmPasswordLayout.error = "Conferma la tua password"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailLayout.error = "Inserisci un'email valida"
                false
            }
            password.length < 6 -> {
                binding.passwordLayout.error = "La password deve avere almeno 6 caratteri"
                false
            }
            password != confirmPassword -> {
                binding.confirmPasswordLayout.error = "Le password non corrispondono"
                false
            }
            else -> {
                // Clear all errors
                binding.emailLayout.error = null
                binding.passwordLayout.error = null
                binding.confirmPasswordLayout.error = null
                true
            }
        }
    }

    private fun createUserAccount(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account creato con successo!", Toast.LENGTH_SHORT).show()
                    // Navigate to profile setup for new users
                    navigateToProfileSetup()
                } else {
                    val errorMessage = task.exception?.message ?: "Creazione account non riuscita"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToProfileSetup() {
        val intent = Intent(this, ProfileSetupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}