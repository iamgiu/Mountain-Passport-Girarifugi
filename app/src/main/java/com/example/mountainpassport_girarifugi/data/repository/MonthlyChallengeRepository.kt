package com.example.mountainpassport_girarifugi.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.*

class MonthlyChallengeRepository {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getCurrentChallenge(): MonthlyChallenge? {
        val monthKey = getCurrentMonthKey()
        val doc = firestore.collection("monthly_challenges")
            .document(monthKey)
            .get()
            .await()

        return if (doc.exists()) {
            doc.toObject(MonthlyChallenge::class.java)
        } else null
    }

    private fun getCurrentMonthKey(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    /**
     * Reset mensile dei punteggi: azzera solo se resetDone = false
     */
    suspend fun resetMonthlyPointsIfNeeded() {
        try {
            val monthKey = getCurrentMonthKey()
            val challengeRef = firestore.collection("monthly_challenges").document(monthKey)
            val snapshot = challengeRef.get().await()

            // Se il documento non esiste → crealo
            if (!snapshot.exists()) {
                challengeRef.set(
                    mapOf(
                        "resetDone" to false,
                        "bonusRifugi" to emptyList<Int>(),
                        "startDate" to com.google.firebase.Timestamp(Date()),
                        "endDate" to null
                    ),
                    SetOptions.merge()
                ).await()
                Log.d("MonthlyReset", "Creato challenge $monthKey")
            }

            val resetDone = snapshot.getBoolean("resetDone") ?: false
            if (resetDone) {
                Log.d("MonthlyReset", "Reset già eseguito per $monthKey, esco")
                return
            }

            // 🚀 Avvio reset
            val batch = firestore.batch()

            // Reset punti utenti
            val usersSnap = firestore.collection("users").get().await()
            for (doc in usersSnap.documents) {
                batch.update(doc.reference, "points", 0)
            }

            // Reset stats mensili
            val statsSnap = firestore.collection("user_points_stats").get().await()
            for (doc in statsSnap.documents) {
                batch.update(
                    doc.reference,
                    mapOf(
                        "monthlyPoints" to 0,
                        "monthlyVisits" to 0
                    )
                )
            }

            // Aggiorna resetDone = true
            batch.update(challengeRef, "resetDone", true)

            batch.commit().await()
            Log.d("MonthlyReset", "✅ Reset completato per $monthKey")

        } catch (e: Exception) {
            Log.e("MonthlyReset", "Errore nel reset mensile: ${e.message}", e)
        }
    }
}

data class MonthlyChallenge(
    val startDate: com.google.firebase.Timestamp? = null,
    val endDate: com.google.firebase.Timestamp? = null,
    val bonusRifugi: List<Int> = emptyList(),
    val resetDone: Boolean = false
)
