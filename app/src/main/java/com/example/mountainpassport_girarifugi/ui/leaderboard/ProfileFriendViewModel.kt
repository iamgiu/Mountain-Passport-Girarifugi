package com.example.mountainpassport_girarifugi.ui.profile

import android.util.Log
import androidx.lifecycle.*
import com.example.mountainpassport_girarifugi.data.model.UserPoints
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFriendViewModel : ViewModel() {

    companion object {
        private const val TAG = "ProfileFriendViewModel"
    }

    private val firestore = FirebaseFirestore.getInstance()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _stamps = MutableLiveData<List<UserPoints>>()
    val stamps: LiveData<List<UserPoints>> = _stamps

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    /**
     * Carica i timbri dell'amico da Firestore
     */
    fun loadStamps(friendId: String) {
        Log.d(TAG, "🚀 Iniziando caricamento timbri per friendId: $friendId")

        if (friendId.isEmpty()) {
            Log.e(TAG, "❌ friendId è vuoto!")
            _error.postValue("ID utente non valido")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)

            try {
                Log.d(TAG, "📡 Eseguendo query Firestore...")

                val snapshot = firestore.collection("users")
                    .document(friendId)
                    .collection("stamps")
                    .get()
                    .await()

                Log.d(TAG, "📋 Documenti ricevuti: ${snapshot.documents.size}")

                val stampsList = snapshot.documents.mapNotNull { doc ->
                    Log.d(TAG, "📄 Processando documento: ${doc.id}")
                    Log.d(TAG, "📄 Dati documento: ${doc.data}")

                    val refugeName = doc.getString("refugeName")
                    val dateMillis = doc.getLong("date")

                    Log.d(TAG, "📄 refugeName: $refugeName, dateMillis: $dateMillis")

                    if (refugeName == null || dateMillis == null) {
                        Log.w(TAG, "⚠️ Documento con dati mancanti, saltato")
                        return@mapNotNull null
                    }

                    val timestamp = Timestamp(dateMillis / 1000, 0)

                    UserPoints(
                        rifugioId = 0,
                        rifugioName = refugeName,
                        visitDate = timestamp,
                        pointsEarned = 0,
                        userId = friendId
                    )
                }.sortedByDescending { it.visitDate.seconds }

                Log.d(TAG, "✅ Timbri processati: ${stampsList.size}")
                stampsList.forEach { stamp ->
                    Log.d(TAG, "🏔️ Timbro: ${stamp.rifugioName} - ${stamp.visitDate.toDate()}")
                }

                _stamps.postValue(stampsList)

            } catch (e: Exception) {
                Log.e(TAG, "💥 Errore nel caricamento timbri: ${e.message}", e)
                _error.postValue("Errore nel caricamento dei timbri: ${e.message}")
                _stamps.postValue(emptyList())
            }

            _isLoading.postValue(false)
        }
    }
}