package com.example.mountainpassport_girarifugi.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.mountainpassport_girarifugi.MainActivity
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.ui.notifications.NotificationsViewModel

/**
 * Helper class per gestire le notifiche locali dell'app
 */
object NotificationHelper {

    private const val CHANNEL_ID = "mountain_passport_channel"
    private const val CHANNEL_NAME = "Mountain Passport Notifiche"
    private const val CHANNEL_DESCRIPTION = "Notifiche per l'app Mountain Passport"

    /**
     * Crea il canale di notifica per Android 8.0+
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
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

    /**
     * Verifica se l'app ha i permessi per inviare notifiche
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Per versioni precedenti ad Android 13, i permessi sono automatici
        }
    }

    /**
     * Mostra una notifica locale
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = 0,
        intent: Intent? = null
    ) {
        android.util.Log.d("NotificationHelper", "Tentativo di mostrare notifica: $title - $message")
        
        // Forza sempre la creazione del canale
        createNotificationChannel(context)
        
        if (!hasNotificationPermission(context)) {
            android.util.Log.w("NotificationHelper", "Permessi notifica non concessi")
            return
        }

        val notificationIntent = intent ?: Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            notificationIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibrazione personalizzata
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        try {
            notificationManager.notify(notificationId, notificationBuilder.build())
            android.util.Log.d("NotificationHelper", "Notifica inviata con successo - ID: $notificationId")
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Errore nell'invio notifica: ${e.message}")
        }
    }

    /**
     * Mostra una notifica per una richiesta di amicizia
     */
    fun showFriendRequestNotification(
        context: Context,
        senderName: String,
        notificationId: Int = 1
    ) {
        val title = "Nuova richiesta di amicizia"
        val message = "$senderName ti ha inviato una richiesta di amicizia"
        
        showNotification(context, title, message, notificationId)
    }

    /**
     * Mostra una notifica per punti guadagnati
     */
    fun showPointsEarnedNotification(
        context: Context,
        points: Int,
        rifugioName: String? = null,
        notificationId: Int = 2
    ) {
        val title = "Punti guadagnati!"
        val message = if (rifugioName != null) {
            "Hai guadagnato $points punti visitando $rifugioName"
        } else {
            "Hai guadagnato $points punti!"
        }
        
        showNotification(context, title, message, notificationId)
    }

    /**
     * Mostra una notifica per un nuovo rifugio
     */
    fun showNewRifugioNotification(
        context: Context,
        rifugioName: String,
        notificationId: Int = 3
    ) {
        val title = "Nuovo rifugio disponibile"
        val message = "$rifugioName è ora disponibile per la visita!"
        
        showNotification(context, title, message, notificationId)
    }

    /**
     * Mostra una notifica per un achievement sbloccato
     */
    fun showAchievementNotification(
        context: Context,
        achievementName: String,
        notificationId: Int = 4
    ) {
        val title = "Achievement sbloccato!"
        val message = "Hai sbloccato: $achievementName"
        
        showNotification(context, title, message, notificationId)
    }

    /**
     * Cancella una notifica specifica
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancella tutte le notifiche
     */
    fun cancelAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
}
