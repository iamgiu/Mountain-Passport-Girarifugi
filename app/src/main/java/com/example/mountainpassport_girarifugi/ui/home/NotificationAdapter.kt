package com.example.mountainpassport_girarifugi.ui.notifications

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mountainpassport_girarifugi.R
import com.google.firebase.firestore.FirebaseFirestore

class NotificationAdapter(
    private val notifiche: List<NotificationsViewModel.Notifica>,
    private val onNotificaClick: (NotificationsViewModel.Notifica) -> Unit,
    private val onAcceptFriendRequest: (String, String) -> Unit = { _, _ -> },
    private val onDeclineFriendRequest: (String, String) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_FRIEND_REQUEST = 1
        private const val TAG = "NotificationAdapter"
    }

    override fun getItemViewType(position: Int): Int {
        return if (notifiche[position].tipo == NotificationsViewModel.TipoNotifica.RICHIESTA_AMICIZIA) {
            TYPE_FRIEND_REQUEST
        } else {
            TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FRIEND_REQUEST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_friend_request, parent, false)
                FriendRequestViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notifications, parent, false)
                NotificationViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FriendRequestViewHolder -> holder.bind(notifiche[position])
            is NotificationViewHolder -> holder.bind(notifiche[position])
        }
    }

    override fun getItemCount(): Int = notifiche.size

    // ViewHolder per richieste di amicizia
    inner class FriendRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewSenderName: TextView = itemView.findViewById(R.id.textViewSenderName)
        private val textViewSenderNickname: TextView = itemView.findViewById(R.id.textViewSenderNickname)
        private val buttonAccept: Button = itemView.findViewById(R.id.buttonAccept)
        private val buttonDecline: Button = itemView.findViewById(R.id.buttonDecline)
        private val imageViewSenderAvatar: ImageView? = itemView.findViewById(R.id.imageViewSenderAvatar)

        fun bind(notifica: NotificationsViewModel.Notifica) {
            // PROTEZIONE CONTRO STRINGHE NULL
            textViewSenderName.text = notifica.titolo.takeIf { it.isNotBlank() } ?: "Utente sconosciuto"
            textViewSenderNickname.text = notifica.descrizione.takeIf { it.isNotBlank() } ?: "Richiesta di amicizia"

            // Regola l'opacity se già letta
            itemView.alpha = if (notifica.isLetta) 0.7f else 1.0f

            // LOG per debug
            Log.d(TAG, "Binding friend request:")
            Log.d(TAG, "- Notification ID: ${notifica.id}")
            Log.d(TAG, "- Sender ID (utenteId): ${notifica.utenteId}")
            Log.d(TAG, "- Title: ${notifica.titolo}")
            Log.d(TAG, "- Description: ${notifica.descrizione}")
            Log.d(TAG, "- Avatar URL: ${notifica.avatarUrl}")

            // CARICA IMMAGINE PROFILO DELL'UTENTE
            loadUserProfileImage(notifica.avatarUrl, notifica.utenteId)

            // VALIDAZIONE ROBUSTA del senderId
            val senderId = notifica.utenteId
            val isValidSenderId = !senderId.isNullOrBlank() && senderId != "null"

            Log.d(TAG, "- Valid sender ID: $isValidSenderId")

            if (!isValidSenderId) {
                Log.e(TAG, "ERROR: SenderId is null/empty/invalid for notification ${notifica.id}")
                showErrorState("Richiesta non valida")
                return
            }

            // VERIFICA CHE NON SIA UNA RICHIESTA A SE STESSO
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (senderId == currentUserId) {
                Log.e(TAG, "ERROR: Friend request from self detected!")
                showErrorState("Errore: richiesta da se stesso")
                return
            }

            // STATO NORMALE - Abilita i bottoni
            resetButtonStates()

            // CLICK LISTENERS CON PROTEZIONE DOPPIO-CLICK
            var acceptClicked = false
            var declineClicked = false

            buttonAccept.setOnClickListener {
                Log.d(TAG, "Accept button clicked")

                if (acceptClicked) {
                    Log.w(TAG, "Accept already clicked, ignoring")
                    return@setOnClickListener
                }

                if (!isValidSenderId) {
                    Log.e(TAG, "Accept clicked but senderId is invalid")
                    return@setOnClickListener
                }

                acceptClicked = true

                Log.d(TAG, "- Calling onAcceptFriendRequest with senderId: $senderId, notificationId: ${notifica.id}")

                try {
                    // Disabilita immediatamente i bottoni
                    disableButtonsWithFeedback("Accettando...")

                    onAcceptFriendRequest(senderId!!, notifica.id)
                    Log.d(TAG, "onAcceptFriendRequest called successfully")

                } catch (e: Exception) {
                    Log.e(TAG, "Error in accept button click", e)
                    acceptClicked = false
                    resetButtonStates()
                }
            }

            buttonDecline.setOnClickListener {
                Log.d(TAG, "Decline button clicked")

                if (declineClicked) {
                    Log.w(TAG, "Decline already clicked, ignoring")
                    return@setOnClickListener
                }

                if (!isValidSenderId) {
                    Log.e(TAG, "Decline clicked but senderId is invalid")
                    return@setOnClickListener
                }

                declineClicked = true

                Log.d(TAG, "- Calling onDeclineFriendRequest with senderId: $senderId, notificationId: ${notifica.id}")

                try {
                    // Disabilita immediatamente i bottoni
                    disableButtonsWithFeedback("Rifiutando...")

                    onDeclineFriendRequest(senderId!!, notifica.id)
                    Log.d(TAG, "onDeclineFriendRequest called successfully")

                } catch (e: Exception) {
                    Log.e(TAG, "Error in decline button click", e)
                    declineClicked = false
                    resetButtonStates()
                }
            }

            // Click listener per l'intera card (CON PROTEZIONE)
            itemView.setOnClickListener {
                try {
                    if (!acceptClicked && !declineClicked) {
                        onNotificaClick(notifica)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling notification click", e)
                }
            }
        }

        // NUOVA FUNZIONE: Carica l'immagine profilo dell'utente
        private fun loadUserProfileImage(avatarUrl: String?, userId: String?) {
            imageViewSenderAvatar?.let { imageView ->
                if (!avatarUrl.isNullOrBlank()) {
                    // Carica l'immagine dall'URL
                    Glide.with(itemView.context)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_person_24)
                        .error(R.drawable.ic_person_24)
                        .circleCrop()
                        .into(imageView)
                } else if (!userId.isNullOrBlank()) {
                    // Fallback: carica l'immagine dall'utente Firebase
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            try {
                                val user = document.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)
                                val profileImageUrl = user?.profileImageUrl

                                if (!profileImageUrl.isNullOrBlank()) {
                                    // Usa Glide per caricare l'immagine
                                    Glide.with(itemView.context)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.ic_person_24)
                                        .error(R.drawable.ic_person_24)
                                        .circleCrop()
                                        .into(imageView)
                                } else {
                                    // Usa immagine di default
                                    imageView.setImageResource(R.drawable.ic_person_24)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error loading user profile image", e)
                                imageView.setImageResource(R.drawable.ic_person_24)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error fetching user document for profile image", e)
                            imageView.setImageResource(R.drawable.ic_person_24)
                        }
                } else {
                    // UserId non valido, usa immagine di default
                    imageView.setImageResource(R.drawable.ic_person_24)
                }
            }
        }

        private fun resetButtonStates() {
            buttonAccept.apply {
                isEnabled = true
                alpha = 1.0f
                text = "Accetta"
            }

            buttonDecline.apply {
                isEnabled = true
                alpha = 1.0f
                text = "Rifiuta"
            }
        }

        private fun disableButtonsWithFeedback(message: String) {
            buttonAccept.isEnabled = false
            buttonDecline.isEnabled = false

            if (message.contains("Accettando")) {
                buttonAccept.apply {
                    text = message
                    alpha = 0.7f
                    setBackgroundColor(ContextCompat.getColor(context, R.color.gray_light))
                }
                buttonDecline.apply {
                    alpha = 0.5f
                    setBackgroundColor(ContextCompat.getColor(context, R.color.gray_light))
                }
            } else {
                buttonDecline.apply {
                    text = message
                    alpha = 0.7f
                    setBackgroundColor(ContextCompat.getColor(context, R.color.gray_light))
                }
                buttonAccept.apply {
                    alpha = 0.5f
                    setBackgroundColor(ContextCompat.getColor(context, R.color.gray_light))
                }
            }
        }

        private fun showErrorState(errorMsg: String) {
            buttonAccept.apply {
                isEnabled = false
                alpha = 0.5f
                text = errorMsg
                setBackgroundColor(ContextCompat.getColor(context, R.color.gray_light))
            }

            buttonDecline.apply {
                isEnabled = false
                alpha = 0.5f
                text = "Non disponibile"
                setBackgroundColor(ContextCompat.getColor(context, R.color.gray_light))
            }
        }
    }

    // ViewHolder normale per altre notifiche
    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconNotification: ImageView = itemView.findViewById(R.id.iconNotification)
        private val textTitolo: TextView = itemView.findViewById(R.id.textTitoloNotifica)
        private val textDescrizione: TextView = itemView.findViewById(R.id.textDescrizioneNotifica)
        private val textTempo: TextView = itemView.findViewById(R.id.textTempo)
        private val badgeNonLetto: View = itemView.findViewById(R.id.badgeNonLetto)

        fun bind(notifica: NotificationsViewModel.Notifica) {
            textTitolo.text = notifica.titolo
            textDescrizione.text = notifica.descrizione
            textTempo.text = notifica.tempo

            // Mostra/nascondi badge non letto
            badgeNonLetto.visibility = if (!notifica.isLetta) View.VISIBLE else View.GONE

            // Imposta l'icona in base al tipo di notifica
            val iconResource = getIconResource(notifica.tipo)
            iconNotification.setImageResource(iconResource)

            // Regola l'opacity se già letta
            itemView.alpha = if (notifica.isLetta) 0.7f else 1.0f

            // Click listener
            itemView.setOnClickListener {
                try {
                    onNotificaClick(notifica)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling notification click", e)
                }
            }
        }

        private fun getIconResource(tipo: NotificationsViewModel.TipoNotifica): Int {
            return when (tipo) {
                NotificationsViewModel.TipoNotifica.NUOVO_MEMBRO_GRUPPO -> R.drawable.ic_cabin_24
                NotificationsViewModel.TipoNotifica.RICHIESTA_AMICIZIA -> R.drawable.ic_person_add_24
                NotificationsViewModel.TipoNotifica.NUOVA_SFIDA_MESE -> R.drawable.ic_leaderboard_24
                NotificationsViewModel.TipoNotifica.DOPPIO_PUNTI_RIFUGI -> R.drawable.ic_electric_bolt_24px
                NotificationsViewModel.TipoNotifica.SFIDA_COMPLETATA -> R.drawable.ic_cabin_24
                NotificationsViewModel.TipoNotifica.PUNTI_OTTENUTI -> R.drawable.ic_leaderboard_24
                NotificationsViewModel.TipoNotifica.TIMBRO_OTTENUTO -> R.drawable.ic_footprint_24
                NotificationsViewModel.TipoNotifica.SISTEMA -> R.drawable.ic_notifications_black_24dp
            }
        }
    }
}