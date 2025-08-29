package com.example.mountainpassport_girarifugi.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mountainpassport_girarifugi.MainActivity
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.repository.NotificationsRepository
import com.example.mountainpassport_girarifugi.ui.notifications.NotificationsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MountainPassportFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FirebaseMsgService"
        private const val CHANNEL_ID = "mountain_passport_channel"
        private const val CHANNEL_NAME = "Mountain Passport Notifiche"
        private const val CHANNEL_DESCRIPTION = "Notifiche per l'app Mountain Passport"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuovo token FCM: $token")
        
        // Salva il token nel database
        saveTokenToDatabase(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Messaggio ricevuto da: ${remoteMessage.from}")

        // Gestisci i dati del messaggio
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Dati del messaggio: ${remoteMessage.data}")
            
            // Crea notifica locale
            val title = remoteMessage.data["title"] ?: "Mountain Passport"
            val message = remoteMessage.data["message"] ?: "Nuova notifica"
            val type = remoteMessage.data["type"] ?: "SISTEMA"
            val category = remoteMessage.data["category"] ?: "generale"
            val rifugioId = remoteMessage.data["rifugioId"]
            val utenteId = remoteMessage.data["utenteId"]
            val achievementId = remoteMessage.data["achievementId"]
            val puntiGuadagnati = remoteMessage.data["puntiGuadagnati"]?.toIntOrNull()

            // Salva la notifica nel database locale
            saveNotificationToDatabase(
                title = title,
                message = message,
                type = type,
                category = category,
                rifugioId = rifugioId,
                utenteId = utenteId,
                achievementId = achievementId,
                puntiGuadagnati = puntiGuadagnati
            )

            // Mostra la notifica locale
            sendNotification(title, message)
        }

        // Gestisci anche il payload di notifica se presente
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notifica ricevuta: ${notification.title} - ${notification.body}")
            sendNotification(
                notification.title ?: "Mountain Passport",
                notification.body ?: "Nuova notifica"
            )
        }
    }

    private fun saveTokenToDatabase(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "Token FCM salvato con successo")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Errore nel salvare il token FCM: ${e.message}")
                }
        }
    }

    private fun saveNotificationToDatabase(
        title: String,
        message: String,
        type: String,
        category: String,
        rifugioId: String?,
        utenteId: String?,
        achievementId: String?,
        puntiGuadagnati: Int?
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = NotificationsRepository()
                    val tipoNotifica = try {
                        NotificationsViewModel.TipoNotifica.valueOf(type)
                    } catch (e: IllegalArgumentException) {
                        NotificationsViewModel.TipoNotifica.SISTEMA
                    }

                    repository.createNotification(
                        userId = currentUser.uid,
                        titolo = title,
                        descrizione = message,
                        tipo = tipoNotifica,
                        categoria = category,
                        rifugioId = rifugioId,
                        utenteId = utenteId,
                        achievementId = achievementId,
                        puntiGuadagnati = puntiGuadagnati
                    )
                    Log.d(TAG, "Notifica salvata nel database locale")
                } catch (e: Exception) {
                    Log.e(TAG, "Errore nel salvare la notifica: ${e.message}")
                }
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crea il canale di notifica per Android 8.0+
        createNotificationChannel(notificationManager)

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
