package com.example.mountainpassport_girarifugi.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

class FeedAmiciAdapter(
    private val feedItems: List<HomeViewModel.FeedAmico>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_RIFUGIO_VISITATO = 1
        private const val TYPE_GENERIC = 0
    }

    override fun getItemViewType(position: Int): Int {
        val item = feedItems[position]
        return when {
            item.testoAttivita.contains("visitato") -> TYPE_RIFUGIO_VISITATO
            else -> TYPE_GENERIC
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RIFUGIO_VISITATO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_feed_rifugio_visitato, parent, false)
                RifugioVisitatoViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_feed_amici, parent, false)
                GenericFeedViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = feedItems[position]
        when (holder) {
            is RifugioVisitatoViewHolder -> holder.bind(item)
            is GenericFeedViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = feedItems.size

    // ViewHolder per card generiche (achievement, punti, ecc.)
    class GenericFeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarContainer: LinearLayout = itemView.findViewById(R.id.avatarContainer)
        private val avatarInitials: TextView = itemView.findViewById(R.id.avatarInitials)
        private val textNotifica: TextView = itemView.findViewById(R.id.textNotifica)
        private val textTempo: TextView = itemView.findViewById(R.id.textTempo)

        fun bind(item: HomeViewModel.FeedAmico) {
            // Genera iniziali dal nome
            val initials = getInitials(item.nomeUtente)
            avatarInitials.text = initials

            // Colore avatar basato sul nome (per varietà)
            val colors = listOf("#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#F44336", "#00BCD4")
            val colorIndex = item.nomeUtente.hashCode() % colors.size
            val backgroundColor = Color.parseColor(colors[Math.abs(colorIndex)])
            avatarContainer.setBackgroundColor(backgroundColor)

            textNotifica.text = "${item.nomeUtente} ${item.testoAttivita}"
            textTempo.text = item.tempo
        }

        private fun getInitials(name: String): String {
            val parts = name.split(" ")
            return when {
                parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercase()}${parts[1].firstOrNull()?.uppercase()}"
                parts.size == 1 -> "${parts[0].take(2).uppercase()}"
                else -> "NA"
            }
        }
    }

    // ViewHolder per rifugi visitati (con dettagli rifugio)
    class RifugioVisitatoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarContainer: LinearLayout = itemView.findViewById(R.id.avatarContainer)
        private val avatarInitials: TextView = itemView.findViewById(R.id.avatarInitials)
        private val textNotifica: TextView = itemView.findViewById(R.id.textNotifica)
        private val textTempo: TextView = itemView.findViewById(R.id.textTempo)
        private val imageRifugio: ImageView = itemView.findViewById(R.id.imageRifugio)
        private val nomeRifugio: TextView = itemView.findViewById(R.id.nomeRifugio)
        private val localitaRifugio: TextView = itemView.findViewById(R.id.localitaRifugio)
        private val altitudineRifugio: TextView = itemView.findViewById(R.id.altitudineRifugio)
        private val textPunti: TextView = itemView.findViewById(R.id.textPunti)

        fun bind(item: HomeViewModel.FeedAmico) {
            // Setup avatar
            val initials = getInitials(item.nomeUtente)
            avatarInitials.text = initials

            val colors = listOf("#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#F44336", "#00BCD4")
            val colorIndex = item.nomeUtente.hashCode() % colors.size
            val backgroundColor = Color.parseColor(colors[Math.abs(colorIndex)])
            avatarContainer.setBackgroundColor(backgroundColor)

            textNotifica.text = "${item.nomeUtente} ha visitato un rifugio"
            textTempo.text = item.tempo

            // Dati rifugio (potresti passarli tramite RifugioInfo in FeedAmico)
            val rifugi = listOf(
                Triple("Rifugio Laghi Verdi", "Val d'Aosta", 75),
                Triple("Rifugio Monte Bianco", "Courmayeur", 90),
                Triple("Capanna Margherita", "Monte Rosa", 120),
                Triple("Rifugio Torino", "Val Ferret", 65)
            )

            val rifugioData = rifugi[Math.abs(item.nomeUtente.hashCode()) % rifugi.size]

            nomeRifugio.text = rifugioData.first
            localitaRifugio.text = rifugioData.second
            altitudineRifugio.text = "Altitudine: ${(1500..2500).random()} m"
            textPunti.text = "+${rifugioData.third} punti"

            // Immagine rifugio
            imageRifugio.setImageResource(R.drawable.mountain_background)
        }

        private fun getInitials(name: String): String {
            val parts = name.split(" ")
            return when {
                parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercase()}${parts[1].firstOrNull()?.uppercase()}"
                parts.size == 1 -> "${parts[0].take(2).uppercase()}"
                else -> "NA"
            }
        }
    }
}