package com.example.mountainpassport_girarifugi.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

class NotificationAdapter(
    private val notifiche: List<NotificationsViewModel.Notifica>,
    private val onNotificaClick: (NotificationsViewModel.Notifica) -> Unit,
    private val onAcceptFriendRequest: (String, String) -> Unit = { _, _ -> },
    private val onDeclineFriendRequest: (String, String) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_FRIEND_REQUEST = 1
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
        private val imageViewSenderAvatar: ImageView = itemView.findViewById(R.id.imageViewSenderAvatar)

        fun bind(notifica: NotificationsViewModel.Notifica) {
            textViewSenderName.text = notifica.titolo
            textViewSenderNickname.text = notifica.descrizione

            // Regola l'opacity se già letta
            itemView.alpha = if (notifica.isLetta) 0.7f else 1.0f

            buttonAccept.setOnClickListener {
                notifica.utenteId?.let { senderId ->
                    onAcceptFriendRequest(senderId, notifica.id)
                }
            }

            buttonDecline.setOnClickListener {
                notifica.utenteId?.let { senderId ->
                    onDeclineFriendRequest(senderId, notifica.id)
                }
            }

            // Click listener per l'intera card
            itemView.setOnClickListener {
                onNotificaClick(notifica)
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
                onNotificaClick(notifica)
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