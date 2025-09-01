package com.example.mountainpassport_girarifugi.data.repository

import android.content.Context
import android.util.Log
import com.example.mountainpassport_girarifugi.data.model.Review
import com.example.mountainpassport_girarifugi.data.model.RifugioStats
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseInitializer(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val rifugioRepository = RifugioRepository(context)
    
    companion object {
        private const val TAG = "FirebaseInitializer"
    }
    
    /**
     * Inizializza Firebase creando le raccolte necessarie con dati di esempio
     */
    suspend fun initializeFirebase() {
        try {
            Log.d(TAG, "Inizializzazione Firebase")

            createSampleReviews()
            createSampleStats()
            createSamplePoints()
            
            Log.d(TAG, "Firebase inizializzato con successo")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'inizializzazione Firebase: ${e.message}")
        }
    }
    
    /**
     * Crea recensioni di esempio per alcuni rifugi
     */
    private suspend fun createSampleReviews() {
        try {
            val sampleReviews = listOf(
                Review(
                    rifugioId = 1,
                    userId = "user_123",
                    userName = "Mario Rossi",
                    rating = 4.5f,
                    comment = "Rifugio fantastico con vista mozzafiato! Servizio eccellente.",
                    timestamp = Timestamp.now()
                ),
                Review(
                    rifugioId = 1,
                    userId = "user_456",
                    userName = "Anna Bianchi",
                    rating = 4.0f,
                    comment = "Ottima posizione per escursioni. Cibo delizioso.",
                    timestamp = Timestamp.now()
                ),
                Review(
                    rifugioId = 2,
                    userId = "user_789",
                    userName = "Luca Verdi",
                    rating = 5.0f,
                    comment = "Esperienza indimenticabile! Personale molto cordiale.",
                    timestamp = Timestamp.now()
                ),
                Review(
                    rifugioId = 3,
                    userId = "user_123",
                    userName = "Mario Rossi",
                    rating = 3.5f,
                    comment = "Bivacco essenziale ma funzionale. Perfetto per il pernottamento.",
                    timestamp = Timestamp.now()
                )
            )

            sampleReviews.forEach { review ->
                firestore.collection("reviews")
                    .add(review)
                    .await()
            }
            
            Log.d(TAG, "Recensioni di esempio create: ${sampleReviews.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella creazione delle recensioni: ${e.message}")
        }
    }
    
    /**
     * Crea statistiche di esempio per alcuni rifugi
     */
    private suspend fun createSampleStats() {
        try {
            val sampleStats = listOf(
                RifugioStats(
                    rifugioId = 1,
                    averageRating = 4.25f,
                    totalReviews = 2,
                    totalVisits = 15,
                    totalSaves = 8,
                    lastUpdated = Timestamp.now()
                ),
                RifugioStats(
                    rifugioId = 2,
                    averageRating = 5.0f,
                    totalReviews = 1,
                    totalVisits = 12,
                    totalSaves = 6,
                    lastUpdated = Timestamp.now()
                ),
                RifugioStats(
                    rifugioId = 3,
                    averageRating = 3.5f,
                    totalReviews = 1,
                    totalVisits = 8,
                    totalSaves = 3,
                    lastUpdated = Timestamp.now()
                )
            )

            sampleStats.forEach { stats ->
                firestore.collection("rifugio_stats")
                    .document(stats.rifugioId.toString())
                    .set(stats)
                    .await()
            }
            
            Log.d(TAG, "Statistiche di esempio create: ${sampleStats.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella creazione delle statistiche: ${e.message}")
        }
    }
    
    /**
     * Verifica se Firebase è inizializzato
     */
    suspend fun isFirebaseInitialized(): Boolean {
        return try {
            val snapshot = firestore.collection("reviews")
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Crea dati di esempio per il sistema di punti
     */
    private suspend fun createSamplePoints() {
        try {
            Log.d(TAG, "Creazione dati di esempio per il sistema di punti")

            val sampleUsers = listOf(
                "user_123" to 1250,
                "user_456" to 890,
                "user_789" to 2100
            )
            
            for ((userId, points) in sampleUsers) {
                val userStats = com.example.mountainpassport_girarifugi.data.model.UserPointsStats(
                    userId = userId,
                    totalPoints = points,
                    totalVisits = points / 25,
                    monthlyPoints = points / 3,
                    monthlyVisits = (points / 25) / 3,
                    lastUpdated = com.google.firebase.Timestamp.now()
                )
                
                firestore.collection("user_points_stats")
                    .document(userId)
                    .set(userStats)
                    .await()
            }

            val sampleVisits = listOf(
                com.example.mountainpassport_girarifugi.data.model.UserPoints(
                    userId = "user_123",
                    rifugioId = 1,
                    rifugioName = "3A 14998",
                    pointsEarned = 58, // 29 * 2 (punti doppi)
                    visitDate = com.google.firebase.Timestamp.now(),
                    visitType = com.example.mountainpassport_girarifugi.data.model.VisitType.VISIT,
                    isDoublePoints = true
                ),
                com.example.mountainpassport_girarifugi.data.model.UserPoints(
                    userId = "user_456",
                    rifugioId = 4,
                    rifugioName = "Accà Lascio",
                    pointsEarned = 12,
                    visitDate = com.google.firebase.Timestamp.now(),
                    visitType = com.example.mountainpassport_girarifugi.data.model.VisitType.VISIT,
                    isDoublePoints = false
                )
            )
            
            for (visit in sampleVisits) {
                firestore.collection("user_points")
                    .add(visit)
                    .await()
            }
            
            Log.d(TAG, "Dati di esempio per il sistema di punti creati con successo")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella creazione dati di esempio per i punti: ${e.message}")
        }
    }
}
