package com.example.mountainpassport_girarifugi.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

class NotificationAdapter(
    private val notifiche: List<NotificationsViewModel.Notifica>,
    private val onNotificaClick: (NotificationsViewModel.Notifica) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notifications, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifiche[position])
    }

    override fun getItemCount(): Int = notifiche.size

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

            // Regola l'opacity se la notifica è già letta
            itemView.alpha = if (notifica.isLetta) 0.7f else 1.0f

            // Click listener
            itemView.setOnClickListener {
                onNotificaClick(notifica)
            }
        }

        private fun getIconResource(tipo: NotificationsViewModel.TipoNotifica): Int {
            return when (tipo) {
                NotificationsViewModel.TipoNotifica.RIFUGIO_NUOVO -> R.drawable.ic_cabin_24
                NotificationsViewModel.TipoNotifica.RIFUGIO_VISITATO -> R.drawable.ic_cabin_24
                NotificationsViewModel.TipoNotifica.ACHIEVEMENT -> R.drawable.ic_route_24
                NotificationsViewModel.TipoNotifica.SFIDA -> R.drawable.ic_leaderboard_24
                NotificationsViewModel.TipoNotifica.NUOVO_AMICO -> R.drawable.ic_person_add_24
                NotificationsViewModel.TipoNotifica.RECENSIONE -> R.drawable.ic_location_24
                NotificationsViewModel.TipoNotifica.PROMEMORIA -> R.drawable.ic_schedule_24
                NotificationsViewModel.TipoNotifica.PUNTI_GUADAGNATI -> R.drawable.ic_leaderboard_24
                NotificationsViewModel.TipoNotifica.SISTEMA -> R.drawable.ic_notifications_black_24dp
            }
        }
    }
}