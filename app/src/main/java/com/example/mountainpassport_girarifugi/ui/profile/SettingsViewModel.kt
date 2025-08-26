package com.example.mountainpassport_girarifugi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mountainpassport_girarifugi.ui.user.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsViewModel : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _currentUser = MutableLiveData<User>()
    val currentUser: LiveData<User> = _currentUser

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    private val _validationErrors = MutableLiveData<ValidationErrors>()
    val validationErrors: LiveData<ValidationErrors> = _validationErrors

    private val _resetPasswordSuccess = MutableLiveData<Boolean>()
    val resetPasswordSuccess: LiveData<Boolean> = _resetPasswordSuccess

    private val _resetPasswordError = MutableLiveData<String>()
    val resetPasswordError: LiveData<String> = _resetPasswordError

    init {
        loadUserData()
    }

    fun loadUserData() {
        val currentUser = firebaseAuth.currentUser ?: return

        _isLoading.value = true

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                _isLoading.value = false
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        _currentUser.value = it
                    }
                } else {
                    _errorMessage.value = "Errore nel caricare i dati del profilo"
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _errorMessage.value = "Errore nel caricare i dati: ${e.message}"
            }
    }

    fun validateAndSaveProfile(nome: String, cognome: String, nickname: String) {
        val errors = ValidationErrors()

        // Validation
        when {
            nome.trim().isEmpty() -> {
                errors.nomeError = "Nome è richiesto"
            }
            cognome.trim().isEmpty() -> {
                errors.cognomeError = "Cognome è richiesto"
            }
            nickname.trim().isEmpty() -> {
                errors.nicknameError = "Nickname è richiesto"
            }
            else -> {
                // All valid, proceed with save
                saveProfile(nome.trim(), cognome.trim(), nickname.trim())
                return
            }
        }

        _validationErrors.value = errors
    }

    private fun saveProfile(nome: String, cognome: String, nickname: String) {
        val currentUser = firebaseAuth.currentUser ?: return
        val originalUser = _currentUser.value ?: return

        _isLoading.value = true

        val updatedUser = originalUser.copy(
            nome = nome,
            cognome = cognome,
            nickname = nickname
        )

        firestore.collection("users").document(currentUser.uid)
            .set(updatedUser)
            .addOnSuccessListener {
                _isLoading.value = false
                _currentUser.value = updatedUser
                _saveSuccess.value = true
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _errorMessage.value = "Errore nell'aggiornare il profilo: ${e.message}"
            }
    }

    fun sendPasswordResetEmail() {
        val currentUserEmail = firebaseAuth.currentUser?.email

        if (currentUserEmail.isNullOrEmpty()) {
            _resetPasswordError.value = "Nessuna email associata all'account"
            return
        }

        firebaseAuth.sendPasswordResetEmail(currentUserEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _resetPasswordSuccess.value = true
                } else {
                    val errorMessage = task.exception?.message ?: "Errore nell'invio dell'email di reset"
                    _resetPasswordError.value = errorMessage
                }
            }
    }

    fun performLogout() {
        firebaseAuth.signOut()
        _logoutEvent.value = true
    }

    fun clearValidationErrors() {
        _validationErrors.value = ValidationErrors()
    }

    fun onSaveSuccessHandled() {
        _saveSuccess.value = false
    }

    fun onLogoutEventHandled() {
        _logoutEvent.value = false
    }

    fun onErrorMessageHandled() {
        _errorMessage.value = null
    }

    fun onResetPasswordSuccessHandled() {
        _resetPasswordSuccess.value = false
    }

    fun onResetPasswordErrorHandled() {
        _resetPasswordError.value = null
    }
}

data class ValidationErrors(
    var nomeError: String? = null,
    var cognomeError: String? = null,
    var nicknameError: String? = null
)