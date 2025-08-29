# 🚀 Sistema di Notifiche - Mountain Passport

## 📋 Panoramica

Il sistema di notifiche dell'app Mountain Passport è stato completamente implementato con supporto per:

- ✅ **Notifiche In-App** (Completo)
- ✅ **Notifiche Locali** (Completo)
- ✅ **Push Notifications** (Base implementata)

## 🏗️ Architettura

### 1. **Notifiche In-App** (`NotificationsRepository.kt`)
- Gestione completa delle notifiche nel database Firebase
- Real-time updates con Firestore listener
- Filtri per categoria (Tutte, Rifugi, Amici)
- Separazione notifiche recenti/precedenti
- Azioni contestuali (accetta/rifiuta amicizia)

### 2. **Notifiche Locali** (`NotificationHelper.kt`)
- Gestione canali di notifica per Android 8.0+
- Verifica permessi notifiche per Android 13+
- Notifiche tipizzate per diversi eventi
- Integrazione con il sistema esistente

### 3. **Push Notifications** (`FirebaseMessagingService.kt`)
- Servizio Firebase Cloud Messaging
- Gestione token FCM
- Salvataggio automatico nel database
- Creazione notifiche locali da push

## 📁 File Implementati

```
app/src/main/java/com/example/mountainpassport_girarifugi/
├── data/repository/
│   ├── NotificationsRepository.kt          # ✅ Completo
│   ├── PushNotificationRepository.kt       # ✅ Nuovo
│   └── PointsRepository.kt                 # ✅ Aggiornato con notifiche
├── ui/notifications/
│   ├── NotificationsFragment.kt            # ✅ Completo
│   ├── NotificationsViewModel.kt           # ✅ Completo
│   └── NotificationAdapter.kt              # ✅ Completo
├── utils/
│   ├── NotificationHelper.kt               # ✅ Nuovo
│   ├── FirebaseMessagingService.kt         # ✅ Nuovo
│   └── UserManager.kt                      # ✅ Esistente
└── MainActivity.kt                         # ✅ Aggiornato
```

## 🔧 Configurazione

### Dipendenze Aggiunte
```kotlin
// build.gradle.kts
implementation(libs.firebase.messaging)
```

### Permessi AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
```

### Servizio FCM Registrato
```xml
<service android:name=".utils.MountainPassportFirebaseMessagingService">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

## 🎯 Tipi di Notifiche Supportati

### 1. **Richieste di Amicizia** ✅
- **Quando:** Immediatamente quando un utente invia una richiesta di amicizia
- **Dove:** `FriendRepository.kt` - metodo `sendFriendRequest()`
- **Notifica:** In-app + locale
- **Azioni:** Accetta/Rifiuta
- **Navigazione:** Al profilo utente

### 2. **Punti Guadagnati** ✅
- **Quando:** Dopo aver registrato una visita (tramite QR code o bottone "Registra Visita")
- **Dove:** `PointsRepository.kt` - metodo `recordVisit()`
- **Notifica:** Locale con punti e nome rifugio
- **Navigazione:** Alla sezione punti

### 3. **Nuovi Rifugi** ❌
- **Stato:** Non implementato
- **Quando:** Quando un nuovo rifugio diventa disponibile
- **Notifica:** Locale + in-app
- **Navigazione:** Al dettaglio rifugio

### 4. **Achievements** ❌
- **Stato:** Non implementato
- **Quando:** Quando un utente sblocca un achievement
- **Notifica:** Locale + in-app
- **Navigazione:** Alla sezione achievements

### 5. **Sfide e Eventi** ❌
- **Stato:** Non implementato
- **Quando:** Inizio/fine sfide mensili, eventi speciali
- **Notifica:** Locale + in-app
- **Navigazione:** Alla sezione sfide

## 🚀 Come Utilizzare

### 1. **Inizializzazione**
```kotlin
// MainActivity.kt - già implementato
private fun initializeNotifications() {
    NotificationHelper.createNotificationChannel(this)
}
```

### 2. **Mostrare Notifica Locale**
```kotlin
// Esempio: Richiesta amicizia
NotificationHelper.showFriendRequestNotification(
    context = this,
    senderName = "Mario Rossi"
)

// Esempio: Punti guadagnati
NotificationHelper.showPointsEarnedNotification(
    context = this,
    points = 100,
    rifugioName = "Rifugio Monte Bianco"
)
```

### 3. **Creare Notifica In-App**
```kotlin
val repository = NotificationsRepository()
repository.createNotification(
    userId = "user_id",
    titolo = "Titolo notifica",
    descrizione = "Descrizione notifica",
    tipo = NotificationsViewModel.TipoNotifica.RICHIESTA_AMICIZIA,
    categoria = "amici"
)
```

### 4. **Preparare Push Notification**
```kotlin
val pushRepo = PushNotificationRepository()
pushRepo.sendFriendRequestPushNotification(
    receiverId = "user_id",
    senderName = "Mario Rossi"
)
```

## 🔄 Integrazione con Sistema Esistente

### FriendRepository ✅
- Integrato con notifiche locali
- Salvataggio automatico nel database
- Notifiche push preparate

### PointsRepository ✅
- **NUOVO:** Integrato con notifiche locali per punti guadagnati
- Notifica automatica dopo registrazione visita
- Mostra punti e nome rifugio

### NotificationsViewModel ✅
- Gestione stato UI
- Filtri e navigazione
- Azioni contestuali

### MainActivity ✅
- Inizializzazione canali notifica
- Gestione permessi

## 📱 UI/UX

### Schermata Notifiche
- Design coerente con il resto dell'app
- Filtri per categoria
- Separazione recenti/precedenti
- Badge per notifiche non lette
- Azioni contestuali per richieste amicizia

### Notifiche Sistema
- Icona personalizzata
- Suono e vibrazione
- Tap per aprire app
- Priorità alta per importanza

## 📅 **Quando Vengono Inviate le Notifiche**

### ✅ **Notifiche Automatiche (Implementate)**

#### 1. **Richieste di Amicizia**
- **Trigger:** Invio richiesta amicizia
- **File:** `FriendRepository.kt` - `sendFriendRequest()`
- **Notifiche:** In-app + locale
- **Messaggio:** "[Nome] ti ha inviato una richiesta di amicizia"

#### 2. **Punti Guadagnati**
- **Trigger:** Registrazione visita (QR code o bottone)
- **File:** `PointsRepository.kt` - `recordVisit()`
- **Notifiche:** Locale
- **Messaggio:** "Hai guadagnato X punti visitando [Nome Rifugio]!"

#### 3. **Push Notifications**
- **Trigger:** Ricezione da Firebase Cloud Messaging
- **File:** `FirebaseMessagingService.kt` - `onMessageReceived()`
- **Notifiche:** Locale + salvataggio in database
- **Messaggio:** Personalizzato dal server

### ❌ **Notifiche NON Implementate**

#### 1. **Nuovi Rifugi**
- **Stato:** Mancante
- **Trigger:** Aggiunta nuovo rifugio al database
- **Implementazione:** Richiede sistema di gestione rifugi

#### 2. **Achievements**
- **Stato:** Mancante
- **Trigger:** Sblocco achievement
- **Implementazione:** Richiede sistema achievement

#### 3. **Sfide Mensili**
- **Stato:** Mancante
- **Trigger:** Inizio/fine sfida
- **Implementazione:** Richiede sistema sfide

#### 4. **Eventi Speciali**
- **Stato:** Mancante
- **Trigger:** Eventi (doppio punti, etc.)
- **Implementazione:** Richiede sistema eventi

## 🔮 Prossimi Passi

### 1. **Firebase Cloud Functions** (Opzionale)
Per l'invio effettivo di push notifications, implementare:
```javascript
// functions/index.js
exports.sendPushNotification = functions.firestore
    .document('pushNotifications/{notificationId}')
    .onCreate(async (snap, context) => {
        // Logica per inviare FCM
    });
```

### 2. **Sistema Achievement** (Priorità Alta)
```kotlin
// Esempio di implementazione
fun checkAndAwardAchievement(userId: String, achievementType: AchievementType) {
    // Logica per verificare e assegnare achievement
    if (achievementUnlocked) {
        NotificationHelper.showAchievementNotification(context, achievementName)
        notificationsRepository.createAchievementNotification(...)
    }
}
```

### 3. **Sistema Sfide** (Priorità Media)
```kotlin
// Esempio di implementazione
fun startMonthlyChallenge() {
    // Notifica inizio sfida
    NotificationHelper.showChallengeNotification(context, "Sfida Mensile Iniziata!")
}
```

### 4. **Impostazioni Notifiche** (Priorità Bassa)
- Permettere all'utente di disabilitare tipi specifici
- Orari silenziosi
- Frequenza notifiche

### 5. **Analytics** (Priorità Bassa)
- Tracciamento apertura notifiche
- Metriche di engagement
- A/B testing messaggi

## ✅ Stato Attuale

- **Notifiche In-App**: ✅ 100% Completo
- **Notifiche Locali**: ✅ 100% Completo  
- **Push Notifications**: ✅ Base implementata
- **UI/UX**: ✅ 100% Completo
- **Integrazione**: ✅ 100% Completo
- **Richieste Amicizia**: ✅ Notifiche implementate
- **Punti Guadagnati**: ✅ Notifiche implementate
- **Sistema Achievement**: ❌ Non implementato
- **Sistema Sfide**: ❌ Non implementato

## 🎯 **Riassunto: Quando Vengono Inviate**

1. **✅ Richiesta Amicizia** → Notifica immediata (in-app + locale)
2. **✅ Registrazione Visita** → Notifica punti guadagnati (locale)
3. **❌ Nuovo Rifugio** → Non implementato
4. **❌ Achievement** → Non implementato
5. **❌ Sfide** → Non implementato
6. **✅ Push FCM** → Quando ricevute dal server

Il sistema è **funzionale** per le notifiche principali (amicizie e punti)! 🎉

## 🔧 **Debug e Risoluzione Problemi**

### Se le notifiche non funzionano:

1. **Vai alla schermata di un rifugio**
2. **Clicca il bottone "Test Notifica"** (arancione) per testare
3. **Tieni premuto il bottone "Test Notifica"** per aprire il debug completo
4. **Controlla i log** in Android Studio per vedere gli errori

### Possibili cause:

1. **Permessi non concessi** (Android 13+)
   - Vai in Impostazioni > App > Mountain Passport > Notifiche
   - Abilita le notifiche

2. **Canale non creato**
   - Il debug forzerà la creazione del canale

3. **App in foreground**
   - Le notifiche potrebbero non apparire se l'app è aperta
   - Prova a minimizzare l'app

4. **Errore nel codice**
   - Controlla i log per errori specifici

### Test Rapido:

```kotlin
// Nel CabinFragment, clicca "Test Notifica"
// Se funziona, il problema è nel PointsRepository
// Se non funziona, il problema è nella configurazione generale
```
