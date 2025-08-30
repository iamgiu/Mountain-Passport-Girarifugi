package com.example.mountainpassport_girarifugi.data.repository

import android.content.Context
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.example.mountainpassport_girarifugi.data.model.Review
import com.example.mountainpassport_girarifugi.data.model.RifugioStats
import com.example.mountainpassport_girarifugi.data.model.UserRifugioInteraction
import com.example.mountainpassport_girarifugi.data.model.TipoRifugio
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import java.io.IOException

class RifugioRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val gson = Gson()

    // Cache per i dati statici
    private var rifugiCache: List<Rifugio>? = null

    /**
     * Carica tutti i rifugi dal file JSON (dati statici)
     */
    suspend fun getAllRifugi(): List<Rifugio> {
        return rifugiCache ?: loadRifugiFromJson().also { rifugiCache = it }
    }

    /**
     * Carica un rifugio specifico per ID
     */
    suspend fun getRifugioById(id: Int): Rifugio? {
        return getAllRifugi().find { it.id == id }
    }

    /**
     * Cerca rifugi per nome o localitÃ
     */
    suspend fun searchRifugi(query: String): List<Rifugio> {
        val allRifugi = getAllRifugi()
        return allRifugi.filter { rifugio ->
            rifugio.nome.contains(query, ignoreCase = true) ||
                    rifugio.localita.contains(query, ignoreCase = true) ||
                    (rifugio.regione?.contains(query, ignoreCase = true) == true)
        }
    }

    /**
     * Salva i Rifugi nella home
     */
    suspend fun getSavedRifugi(userId: String): List<Rifugio> {
        return try {
            android.util.Log.d("RifugioRepository", "getSavedRifugi chiamato per userId: $userId")

            // Usa UserManager per ottenere gli ID dei rifugi salvati
            val savedIds = com.example.mountainpassport_girarifugi.utils.UserManager.getSavedRifugiIds()
            android.util.Log.d("RifugioRepository", "Rifugi salvati IDs: $savedIds")

            val rifugi = mutableListOf<Rifugio>()
            savedIds.forEach { rifugioId ->
                getRifugioById(rifugioId)?.let { rifugio ->
                    rifugi.add(rifugio)
                    android.util.Log.d("RifugioRepository", "Aggiunto rifugio salvato: ${rifugio.nome}")
                }
            }

            android.util.Log.d("RifugioRepository", "Totale rifugi salvati restituiti: ${rifugi.size}")
            rifugi
        } catch (e: Exception) {
            android.util.Log.e("RifugioRepository", "Errore in getSavedRifugi: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Filtra rifugi per regione
     */
    suspend fun getRifugiByRegione(regione: String): List<Rifugio> {
        return getAllRifugi().filter { it.regione?.equals(regione, ignoreCase = true) == true }
    }

    /**
     * Filtra rifugi per tipo
     */
    suspend fun getRifugiByTipo(tipo: TipoRifugio): List<Rifugio> {
        return getAllRifugi().filter { it.tipo == tipo }
    }

    /**
     * Carica le recensioni per un rifugio (dati dinamici da Firebase)
     */
    suspend fun getReviewsForRifugio(rifugioId: Int): List<Review> {
        return try {
            android.util.Log.d("RifugioRepository", "Caricando recensioni per rifugio ID: $rifugioId")

            val snapshot = firestore.collection("reviews")
                .whereEqualTo("rifugioId", rifugioId)
                .get()
                .await()

            android.util.Log.d("RifugioRepository", "Snapshot ottenuto, documenti trovati: ${snapshot.documents.size}")

            val reviews = snapshot.documents.mapNotNull { doc ->
                android.util.Log.d("RifugioRepository", "Documento ID: ${doc.id}, dati: ${doc.data}")
                doc.toObject(Review::class.java)?.copy(id = doc.id)
            }

            android.util.Log.d("RifugioRepository", "Recensioni caricate: ${reviews.size}")
            reviews.forEach { review ->
                android.util.Log.d("RifugioRepository", "Recensione: ${review.userName} - Rating: ${review.rating}")
            }

            reviews
        } catch (e: Exception) {
            android.util.Log.e("RifugioRepository", "Errore nel caricamento recensioni: ${e.message}")
            emptyList()
        }
    }

    /**
     * Carica le statistiche di un rifugio (dati dinamici da Firebase)
     */
    suspend fun getRifugioStats(rifugioId: Int): RifugioStats? {
        return try {
            val doc = firestore.collection("rifugio_stats")
                .document(rifugioId.toString())
                .get()
                .await()

            doc.toObject(RifugioStats::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verifica se un rifugio Ã¨ salvato dall'utente
     */
    suspend fun isRifugioSaved(userId: String, rifugioId: Int): Boolean {
        return com.example.mountainpassport_girarifugi.utils.UserManager.isRifugioSaved(rifugioId)
    }


    /**
     * Salva/rimuove un rifugio dai preferiti
     */
    suspend fun toggleSaveRifugio(userId: String, rifugioId: Int, save: Boolean) {
        val docRef = firestore.collection("saved_rifugi").document(userId)

        try {
            if (save) {
                // aggiunge l'id nell'array
                docRef.update("rifugi", com.google.firebase.firestore.FieldValue.arrayUnion(rifugioId)).await()
            } else {
                // rimuove l'id dall'array
                docRef.update("rifugi", com.google.firebase.firestore.FieldValue.arrayRemove(rifugioId)).await()
            }
        } catch (e: Exception) {
            // se il documento non esiste ancora, lo creo con l'array iniziale
            if (save) {
                docRef.set(mapOf("rifugi" to listOf(rifugioId))).await()
            }
        }
    }


    /**
     * Aggiunge una recensione
     */
    suspend fun addReview(review: Review) {
        try {
            val docRef = firestore.collection("reviews").add(review).await()

            // Aggiorna l'interazione utente
            val interaction = UserRifugioInteraction(
                userId = review.userId,
                rifugioId = review.rifugioId,
                isVisited = true,
                visitDate = review.timestamp,
                rating = review.rating,
                reviewId = docRef.id,
                lastInteraction = review.timestamp
            )

            firestore.collection("user_rifugio_interactions")
                .document("${review.userId}_${review.rifugioId}")
                .set(interaction)
                .await()

            // Aggiorna le statistiche del rifugio
            updateRifugioStatsAfterReview(review.rifugioId)
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Aggiunge recensioni di test per dimostrare il funzionamento
     */
    suspend fun addTestReviews(rifugioId: Int) {
        android.util.Log.d("RifugioRepository", "addTestReviews() chiamato per rifugio ID: $rifugioId")

        val testReviews = listOf(
            Review(
                rifugioId = rifugioId,
                userId = "user_test_1",
                userName = "Marco Rossi",
                rating = 4.5f,
                comment = "Rifugio fantastico con vista mozzafiato! Il personale Ã¨ stato molto gentile e il cibo ottimo.",
                timestamp = com.google.firebase.Timestamp.now()
            ),
            Review(
                rifugioId = rifugioId,
                userId = "user_test_2",
                userName = "Anna Bianchi",
                rating = 5.0f,
                comment = "Esperienza indimenticabile! Il rifugio Ã¨ pulito e confortevole. TornerÃ² sicuramente.",
                timestamp = com.google.firebase.Timestamp.now()
            ),
            Review(
                rifugioId = rifugioId,
                userId = "user_test_3",
                userName = "Luca Verdi",
                rating = 4.0f,
                comment = "Buon rifugio, posizione strategica per le escursioni. Prezzi onesti.",
                timestamp = com.google.firebase.Timestamp.now()
            )
        )

        android.util.Log.d("RifugioRepository", "Aggiungendo ${testReviews.size} recensioni di test")

        testReviews.forEach { review ->
            try {
                addReview(review)
                android.util.Log.d("RifugioRepository", "Recensione aggiunta: ${review.userName}")
            } catch (e: Exception) {
                android.util.Log.e("RifugioRepository", "Errore nell'aggiunta della recensione: ${e.message}")
            }
        }

        android.util.Log.d("RifugioRepository", "Tutte le recensioni di test aggiunte")
    }

    /**
     * Carica i rifugi dal file JSON
     */
    private fun loadRifugiFromJson(): List<Rifugio> {
        return try {
            val jsonString = context.assets.open("rifugi.json").bufferedReader().use { it.readText() }
            val jsonObject = gson.fromJson(jsonString, com.google.gson.JsonObject::class.java)
            val rifugiArray = jsonObject.getAsJsonArray("rifugi")

            val rifugiList = mutableListOf<Rifugio>()

            rifugiArray.forEach { element ->
                val rifugioJson = element.asJsonObject
                val rifugio = Rifugio(
                    id = rifugioJson.get("id").asInt,
                    nome = rifugioJson.get("nome").asString,
                    localita = rifugioJson.get("localita").asString,
                    altitudine = rifugioJson.get("altitudine").asInt,
                    latitudine = rifugioJson.get("latitudine").asDouble,
                    longitudine = rifugioJson.get("longitudine").asDouble,
                    immagineUrl = if (rifugioJson.has("immagineUrl") && !rifugioJson.get("immagineUrl").isJsonNull) {
                        rifugioJson.get("immagineUrl").asString
                    } else null,
                    descrizione = if (rifugioJson.has("descrizione") && !rifugioJson.get("descrizione").isJsonNull) {
                        rifugioJson.get("descrizione").asString
                    } else null,
                    tipo = TipoRifugio.valueOf(rifugioJson.get("tipo").asString),
                    regione = if (rifugioJson.has("regione") && !rifugioJson.get("regione").isJsonNull) {
                        rifugioJson.get("regione").asString
                    } else null
                )
                rifugiList.add(rifugio)
            }

            rifugiList
        } catch (e: IOException) {
            emptyList()
        }
    }

    /**
     * Aggiorna le statistiche di un rifugio dopo il salvataggio/rimozione
     */
    private suspend fun updateRifugioStats(rifugioId: Int, isSaved: Boolean) {
        try {
            val statsRef = firestore.collection("rifugio_stats").document(rifugioId.toString())

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(statsRef)
                val currentStats = snapshot.toObject(RifugioStats::class.java) ?: RifugioStats(rifugioId = rifugioId)

                val newTotalSaves = if (isSaved) currentStats.totalSaves + 1 else currentStats.totalSaves - 1
                val updatedStats = currentStats.copy(
                    totalSaves = newTotalSaves,
                    lastUpdated = com.google.firebase.Timestamp.now()
                )

                transaction.set(statsRef, updatedStats)
            }.await()
        } catch (e: Exception) {
            // Ignora errori nelle statistiche
        }
    }

    /**
     * Aggiorna le statistiche di un rifugio dopo una recensione
     */
    private suspend fun updateRifugioStatsAfterReview(rifugioId: Int) {
        try {
            val reviews = getReviewsForRifugio(rifugioId)
            val averageRating = if (reviews.isNotEmpty()) {
                reviews.map { it.rating }.average().toFloat()
            } else 0f

            val stats = RifugioStats(
                rifugioId = rifugioId,
                averageRating = averageRating,
                totalReviews = reviews.size,
                lastUpdated = com.google.firebase.Timestamp.now()
            )

            firestore.collection("rifugio_stats")
                .document(rifugioId.toString())
                .set(stats)
                .await()
        } catch (e: Exception) {
            // Ignora errori nelle statistiche
        }
    }
}