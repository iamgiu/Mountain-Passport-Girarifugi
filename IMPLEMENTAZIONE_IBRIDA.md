# Implementazione Approccio Ibrido: JSON + Firebase

## Panoramica

L'app Mountain Passport utilizza un approccio ibrido per la gestione dei dati:

- **Dati Statici (JSON)**: Informazioni base dei rifugi (nome, posizione, altitudine, etc.)
- **Dati Dinamici (Firebase)**: Recensioni, preferiti, statistiche, interazioni utente

## Struttura dei Dati

### Dati Statici (JSON)
File: `app/src/main/assets/rifugi.json`

```json
{
  "metadata": {
    "version": "1.0",
    "total_rifugi": 300,
    "regioni_coperte": [...]
  },
  "rifugi": [
    {
      "id": 1,
      "nome": "3A 14998",
      "localita": "Valli Antigorio e Formazza",
      "altitudine": 2910,
      "latitudine": 46.42375,
      "longitudine": 8.33514,
      "tipo": "BIVACCO",
      "regione": "Piemonte"
    }
  ]
}
```

### Dati Dinamici (Firebase)

#### Collection: `reviews`
```kotlin
data class Review(
    val id: String = "",
    val rifugioId: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
```

#### Collection: `rifugio_stats`
```kotlin
data class RifugioStats(
    val rifugioId: Int = 0,
    val averageRating: Float = 0f,
    val totalReviews: Int = 0,
    val totalVisits: Int = 0,
    val totalSaves: Int = 0,
    val lastUpdated: Timestamp = Timestamp.now()
)
```

#### Collection: `user_rifugio_interactions`
```kotlin
data class UserRifugioInteraction(
    val userId: String = "",
    val rifugioId: Int = 0,
    val isSaved: Boolean = false,
    val isVisited: Boolean = false,
    val rating: Float? = null,
    val reviewId: String? = null
)
```

## Architettura

### Repository Pattern
Il `RifugioRepository` gestisce l'accesso ai dati:

```kotlin
class RifugioRepository(private val context: Context) {
    // Dati statici dal JSON
    suspend fun getAllRifugi(): List<Rifugio>
    suspend fun getRifugioById(id: Int): Rifugio?
    suspend fun searchRifugi(query: String): List<Rifugio>
    
    // Dati dinamici da Firebase
    suspend fun getReviewsForRifugio(rifugioId: Int): List<Review>
    suspend fun getRifugioStats(rifugioId: Int): RifugioStats?
    suspend fun isRifugioSaved(userId: String, rifugioId: Int): Boolean
    suspend fun toggleSaveRifugio(userId: String, rifugioId: Int, isSaved: Boolean)
    suspend fun addReview(review: Review)
}
```

### ViewModel
Il `CabinViewModel` combina dati statici e dinamici:

```kotlin
class CabinViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RifugioRepository(application)
    
    // LiveData per i dati
    private val _rifugio = MutableLiveData<Rifugio>()
    private val _reviews = MutableLiveData<List<Review>>()
    private val _stats = MutableLiveData<RifugioStats?>()
    private val _isSaved = MutableLiveData<Boolean>()
    
    fun loadRifugio(rifugioId: Int) {
        // 1. Carica dati statici dal JSON
        // 2. Carica dati dinamici da Firebase
        // 3. Combina i risultati
    }
}
```

## Vantaggi dell'Approccio Ibrido

### 1. **Performance**
- Dati statici sempre disponibili offline
- Caricamento veloce delle informazioni base
- Aggiornamenti dinamici solo quando necessario

### 2. **Scalabilità**
- JSON leggero per dati che cambiano raramente
- Firebase per dati che cambiano frequentemente
- Possibilità di aggiornare il JSON periodicamente

### 3. **Costi**
- Riduzione delle chiamate a Firebase
- Dati statici non consumano quota Firebase
- Ottimizzazione dei costi di storage

### 4. **Manutenibilità**
- Separazione chiara tra dati statici e dinamici
- Facile aggiornamento del database rifugi
- Indipendenza tra i due sistemi

## Implementazione Tecnica

### Parsing JSON
```kotlin
private fun loadRifugiFromJson(): List<Rifugio> {
    val jsonString = context.assets.open("rifugi.json").bufferedReader().use { it.readText() }
    val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
    val rifugiArray = jsonObject.getAsJsonArray("rifugi")
    
    return rifugiArray.map { element ->
        val rifugioJson = element.asJsonObject
        Rifugio(
            id = rifugioJson.get("id").asInt,
            nome = rifugioJson.get("nome").asString,
            // ... altri campi
        )
    }
}
```

### Gestione Firebase
```kotlin
suspend fun getReviewsForRifugio(rifugioId: Int): List<Review> {
    val snapshot = firestore.collection("reviews")
        .whereEqualTo("rifugioId", rifugioId)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get()
        .await()
    
    return snapshot.documents.mapNotNull { doc ->
        doc.toObject(Review::class.java)?.copy(id = doc.id)
    }
}
```

### Aggiornamento Statistiche
```kotlin
private suspend fun updateRifugioStatsAfterReview(rifugioId: Int) {
    val reviews = getReviewsForRifugio(rifugioId)
    val averageRating = if (reviews.isNotEmpty()) {
        reviews.map { it.rating }.average().toFloat()
    } else 0f
    
    val stats = RifugioStats(
        rifugioId = rifugioId,
        averageRating = averageRating,
        totalReviews = reviews.size
    )
    
    firestore.collection("rifugio_stats")
        .document(rifugioId.toString())
        .set(stats)
        .await()
}
```

## Utilizzo nell'App

### CabinFragment
```kotlin
class CabinFragment : Fragment() {
    private lateinit var viewModel: CabinViewModel
    private lateinit var reviewsAdapter: ReviewAdapter
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Inizializza ViewModel e Adapter
        viewModel = ViewModelProvider(this)[CabinViewModel::class.java]
        reviewsAdapter = ReviewAdapter()
        
        // Configura observer per dati combinati
        setupObservers()
        
        // Carica dati del rifugio
        loadRifugioData()
    }
    
    private fun setupObservers() {
        // Dati statici
        viewModel.rifugio.observe(viewLifecycleOwner) { rifugio ->
            populateUI(rifugio)
        }
        
        // Dati dinamici
        viewModel.reviews.observe(viewLifecycleOwner) { reviews ->
            reviewsAdapter.updateReviews(reviews)
        }
        
        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            updateStatsUI(stats)
        }
    }
}
```

## Configurazione Firebase

### 1. Aggiungi le dipendenze
```kotlin
// build.gradle.kts
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-firestore")
implementation("com.google.code.gson:gson:2.10.1")
```

### 2. Configura Firestore
```kotlin
// google-services.json già presente nel progetto
// Le regole Firestore devono permettere lettura/scrittura per utenti autenticati
```

### 3. Struttura Firestore
```
firestore/
├── reviews/
│   ├── {reviewId}/
│   │   ├── rifugioId: number
│   │   ├── userId: string
│   │   ├── rating: number
│   │   └── comment: string
├── rifugio_stats/
│   ├── {rifugioId}/
│   │   ├── averageRating: number
│   │   ├── totalReviews: number
│   │   └── totalSaves: number
└── user_rifugio_interactions/
    ├── {userId}_{rifugioId}/
    │   ├── isSaved: boolean
    │   ├── isVisited: boolean
    │   └── rating: number
```

## Demo e Test

### Aggiungere Recensioni di Test
```kotlin
// Nel CabinViewModel
fun addTestReviews() {
    viewModelScope.launch {
        val rifugioId = _rifugio.value?.id ?: return@launch
        repository.addTestReviews(rifugioId)
        loadDynamicData(rifugioId)
    }
}
```

### Pulsante Demo
Nel layout `fragment_cabin.xml` è presente un pulsante per aggiungere recensioni di test:
```xml
<MaterialButton
    android:id="@+id/addTestReviewsButton"
    android:text="Aggiungi Recensioni Test"
    app:backgroundTint="@color/green" />
```

## Estensioni Future

### 1. **Sincronizzazione**
- Aggiornamento periodico del JSON da server
- Cache intelligente dei dati dinamici
- Sincronizzazione offline

### 2. **Analytics**
- Tracciamento visite rifugi
- Statistiche di utilizzo
- Report personalizzati

### 3. **Social Features**
- Condivisione recensioni
- Foto dei rifugi
- Itinerari personalizzati

### 4. **Notifiche**
- Aggiornamenti rifugi
- Nuove recensioni
- Promemoria visite

## Conclusioni

L'approccio ibrido offre il meglio di entrambi i mondi:
- **Affidabilità** dei dati statici
- **Dinamicità** dei dati in tempo reale
- **Efficienza** nell'utilizzo delle risorse
- **Scalabilità** per future espansioni

Questa architettura permette all'app di funzionare anche offline per i dati base, mentre mantiene la possibilità di aggiornamenti in tempo reale per le funzionalità social e dinamiche.


Problema, non funziona:
# Soluzione Problemi: Rifugi nella Mappa e Firebase

## Problemi Identificati e Risolti

### 1. **Problema: Rifugi non visualizzati nella mappa**

**Causa:** Il `MapFragment` utilizzava una lista hardcoded di rifugi di esempio invece di caricare i dati dal file JSON `rifugi.json`.

**Soluzione Implementata:**

#### Modifiche al `MapFragment.kt`:
- **Rimosso:** Lista hardcoded `rifugiEsempio`
- **Aggiunto:** Repository per caricare i dati dal JSON
- **Implementato:** Caricamento asincrono dei rifugi dal file JSON
- **Aggiunto:** Metodo `loadRifugiAndAddMarkers()` per caricare e visualizzare i rifugi

```kotlin
// Prima (dati hardcoded)
private val rifugiEsempio = listOf(
    Rifugio(id = 1, nome = "Rifugio Torino", ...)
)

// Dopo (caricamento dal JSON)
private var rifugiList: List<Rifugio> = emptyList()
private lateinit var rifugioRepository: RifugioRepository

private fun loadRifugiAndAddMarkers() {
    CoroutineScope(Dispatchers.Main).launch {
        val rifugi = withContext(Dispatchers.IO) {
            rifugioRepository.getAllRifugi()
        }
        rifugiList = rifugi
        addRifugiMarkers()
    }
}
```

#### Modifiche al `SearchCabinViewModel.kt`:
- **Rimosso:** Dati hardcoded
- **Aggiunto:** Repository per caricare i rifugi dal JSON
- **Implementato:** Caricamento asincrono con gestione errori

### 2. **Problema: Raccolte Firebase non create**

**Causa:** Firebase Firestore non crea automaticamente le raccolte finché non vengono inseriti i primi dati.

**Soluzione Implementata:**

#### Creato `FirebaseInitializer.kt`:
- **Funzione:** Inizializza Firebase creando le raccolte necessarie
- **Raccolte create:**
  - `reviews` - Recensioni dei rifugi
  - `rifugio_stats` - Statistiche dei rifugi
  - `user_rifugio_interactions` - Interazioni utente-rifugio
- **Dati di esempio:** Inserisce dati di test per verificare il funzionamento

#### Modifiche al `MainActivity.kt`:
- **Aggiunto:** Inizializzazione Firebase all'avvio dell'app
- **Implementato:** Controllo se Firebase è già inizializzato

```kotlin
private fun initializeFirebase() {
    CoroutineScope(Dispatchers.IO).launch {
        if (!firebaseInitializer.isFirebaseInitialized()) {
            firebaseInitializer.initializeFirebase()
        }
    }
}
```

## Struttura Dati Firebase Creata

### Collection: `reviews`
```json
{
  "id": "auto-generated",
  "rifugioId": 1,
  "userId": "user_123",
  "userName": "Mario Rossi",
  "rating": 4.5,
  "comment": "Rifugio fantastico con vista mozzafiato!",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

### Collection: `rifugio_stats`
```json
{
  "rifugioId": 1,
  "averageRating": 4.25,
  "totalReviews": 2,
  "totalVisits": 15,
  "totalSaves": 8,
  "lastUpdated": "2024-01-01T10:00:00Z"
}
```

### Collection: `user_rifugio_interactions`
```json
{
  "userId_rifugioId": "user_123_1",
  "userId": "user_123",
  "rifugioId": 1,
  "isSaved": true,
  "isVisited": true,
  "rating": 4.5,
  "reviewId": "review_1"
}
```

## Funzionalità Aggiunte

### 1. **Caricamento Rifugi nella Mappa**
- ✅ Caricamento asincrono dal file JSON
- ✅ Visualizzazione di tutti i rifugi disponibili
- ✅ Gestione errori con messaggi informativi
- ✅ Navigazione al dettaglio del rifugio

### 2. **Inizializzazione Firebase**
- ✅ Creazione automatica delle raccolte
- ✅ Inserimento dati di esempio
- ✅ Controllo per evitare duplicazioni
- ✅ Gestione errori di connessione

### 3. **Ricerca Rifugi**
- ✅ Caricamento dati dal JSON
- ✅ Ricerca per nome, località e regione
- ✅ Ordinamento per distanza
- ✅ Gestione stati di caricamento


## File Modificati

1. `app/src/main/java/com/example/mountainpassport_girarifugi/ui/map/MapFragment.kt`
2. `app/src/main/java/com/example/mountainpassport_girarifugi/ui/map/SearchCabinViewModel.kt`
3. `app/src/main/java/com/example/mountainpassport_girarifugi/MainActivity.kt`
4. `app/src/main/java/com/example/mountainpassport_girarifugi/data/repository/FirebaseInitializer.kt` (nuovo)







GESTIONE RIGUGI
Come vengono generate le informazioni:
**1. Servizi (Services)**
**Periodo di apertura: Basato sul tipo del rifugio**
RIFUGIO → "Stagionale (Giugno - Settembre)"
BIVACCO → "Sempre aperto"
CAPANNA → "Estivo (Luglio - Agosto)"
**Posti letto: Basato sull'altitudine**
0-2000m → "50 posti letto"
2001-3000m → "30 posti letto"
>3000m → "15 posti letto"
**2. Come Arrivare (How to Get There)**
Distanza: Basata sull'altitudine
0-2000m → "3.2 km"
2001-3000m → "5.8 km"
>3000m → "8.5 km"
**Dislivello: Basato sull'altitudine**
0-2000m → "800m"
2001-3000m → "1200m"
>3000m → "1800m"
**Tempo: Basato sull'altitudine**
0-2000m → "2h 30m"
2001-3000m → "4h 15m"
>3000m → "6h 00m"
**Difficoltà: Basata sull'altitudine**
0-2000m → "Escursionisti [E]"
2001-3000m → "Escursionisti Esperti [EE]"
>3000m → "Escursionisti Esperti Attrezzati [EEA]"

**SERVIZI**
hasHotWater() - Solo i RIFUGI hanno acqua calda
hasShowers() - Solo i RIFUGI hanno docce
hasElectricity() - RIFUGI sempre, CAPANNE solo sotto 2500m
hasRestaurant() - RIFUGI sempre, CAPANNE solo sotto 2000m

// to do -> nel json sono tutti BIVACCHI, bisogna cambiare qualcuno
es Arben Biwak è CAPANNA




Per implementare immagini da url utilizzato Glide

Collegamento account:
1. Creata Classe UserManager
File: app/src/main/java/com/example/mountainpassport_girarifugi/utils/UserManager.kt
2. Modificato CabinViewModel
Rimosso: ID utente fisso "user_123"
Aggiunto: Metodo getCurrentUserId() che usa UserManager
Aggiornato: recordVisit() per usare l'utente autenticato
3. Modificato CabinFragment
Semplificato: recordVisit() senza parametri
Automatico: Usa l'utente autenticato
4. Modificato ProfileViewModel
Aggiunto: Context nel costruttore
Creato: ProfileViewModelFactory per passare il Context
Implementato: loadUserPointsStats() per caricare statistiche reali
Implementato: loadUserVisits() per caricare timbri reali
5. Modificato ProfileFragment
Aggiornato: Usa ProfileViewModelFactory con Context
📱 Funzionalità Implementate
Sincronizzazione Reale
Statistiche: Caricate dal database Firebase per l'utente autenticato
Timbri: Basati sulle visite reali dell'utente
Punti: Aggiornati in tempo reale
Gestione Utenti
Autenticato: Mostra dati reali dell'utente
Non autenticato: Mostra dati di default (0 punti, 0 visite)
Sicurezza
ID utente: Sempre sincronizzato con Firebase Auth
Dati isolati: Ogni utente vede solo i propri dati