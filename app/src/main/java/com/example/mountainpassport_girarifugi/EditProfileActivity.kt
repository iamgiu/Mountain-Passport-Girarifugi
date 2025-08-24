package com.example.mountainpassport_girarifugi

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mountainpassport_girarifugi.databinding.ActivityEditProfileBinding
import com.example.mountainpassport_girarifugi.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var originalUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupClickListeners()
        loadCurrentUserData()
    }

    private fun setupClickListeners() {
        binding.salvaButton.setOnClickListener {
            if (validateInput()) {
                saveProfileChanges()
            }
        }

        binding.annullaButton.setOnClickListener {
            finish() // Torna alla pagina precedente senza salvare
        }
    }

    private fun loadCurrentUserData() {
        val currentUser = firebaseAuth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        originalUser = it
                        populateFields(it)
                    }
                } else {
                    Toast.makeText(this, "Errore nel caricare i dati del profilo", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Errore nel caricare i dati: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun populateFields(user: User) {
        binding.nomeEt.setText(user.nome)
        binding.cognomeEt.setText(user.cognome)
        binding.nicknameEt.setText(user.nickname)
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

    private fun saveProfileChanges() {
        val currentUser = firebaseAuth.currentUser ?: return

        // Disabilita i pulsanti durante il salvataggio
        binding.salvaButton.isEnabled = false
        binding.annullaButton.isEnabled = false

        val nome = binding.nomeEt.text.toString().trim()
        val cognome = binding.cognomeEt.text.toString().trim()
        val nickname = binding.nicknameEt.text.toString().trim()

        val updatedUser = originalUser.copy(
            nome = nome,
            cognome = cognome,
            nickname = nickname
        )

        firestore.collection("users").document(currentUser.uid)
            .set(updatedUser)
            .addOnSuccessListener {
                Toast.makeText(this, "Profilo aggiornato con successo!", Toast.LENGTH_SHORT).show()
                finish() // Torna alla pagina precedente
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Errore nell'aggiornare il profilo: ${e.message}", Toast.LENGTH_LONG).show()
                // Riabilita i pulsanti in caso di errore
                binding.salvaButton.isEnabled = true
                binding.annullaButton.isEnabled = true
            }
    }
}