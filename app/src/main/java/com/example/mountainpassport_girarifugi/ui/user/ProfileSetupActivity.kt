package com.example.mountainpassport_girarifugi.ui.user

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mountainpassport_girarifugi.MainActivity
import com.example.mountainpassport_girarifugi.databinding.ActivityProfileSetupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupClickListeners()
        loadExistingUserData()
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            if (validateInput()) {
                saveUserProfile()
            }
        }
    }

    private fun loadExistingUserData() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            binding.emailText.text = currentUser.email

            // Load existing user data from Firestore if available
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        user?.let {
                            binding.nomeEt.setText(it.nome)
                            binding.cognomeEt.setText(it.cognome)
                            binding.nicknameEt.setText(it.nickname)
                        }
                    }
                }
        }
    }

    private fun validateInput(): Boolean {
        val nome = binding.nomeEt.text.toString().trim()
        val cognome = binding.cognomeEt.text.toString().trim()
        val nickname = binding.nicknameEt.text.toString().trim()

        return when {
            nome.isEmpty() -> {
                binding.nomeLayout.error = "Nome è richiesto"
                false
            }
            cognome.isEmpty() -> {
                binding.cognomeLayout.error = "Cognome è richiesto"
                false
            }
            nickname.isEmpty() -> {
                binding.nicknameLayout.error = "Nickname è richiesto"
                false
            }
            else -> {
                binding.nomeLayout.error = null
                binding.cognomeLayout.error = null
                binding.nicknameLayout.error = null
                true
            }
        }
    }

    private fun saveUserProfile() {
        val currentUser = firebaseAuth.currentUser ?: return

        binding.saveButton.isEnabled = false

        val nome = binding.nomeEt.text.toString().trim()
        val cognome = binding.cognomeEt.text.toString().trim()
        val nickname = binding.nicknameEt.text.toString().trim()

        val user = User(
            uid = currentUser.uid,
            email = currentUser.email ?: "",
            nome = nome,
            cognome = cognome,
            nickname = nickname
        )

        firestore.collection("users").document(currentUser.uid)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Profilo salvato con successo!", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Errore nel salvare il profilo: ${e.message}", Toast.LENGTH_LONG).show()
                binding.saveButton.isEnabled = true
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}