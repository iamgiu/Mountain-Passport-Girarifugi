package com.example.mountainpassport_girarifugi.ui.home

import android.content.Intent
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
    private val feedItems: List<HomeViewModel.FeedAmico>,
    private val onRifugioClick: (HomeViewModel.RifugioInfo) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_RIFUGIO_VISITATO = 1
        private const val TYPE_GENERIC = 0
    }

    override fun getItemViewType(position: Int): Int {
        val item = feedItems[position]
        return if (item.tipoAttivita == HomeViewModel.TipoAttivita.RIFUGIO_VISITATO && item.rifugioInfo != null) {
            TYPE_RIFUGIO_VISITATO
        } else {
            TYPE_GENERIC
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_RIFUGIO_VISITATO) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_feed_rifugio_visitato, parent, false)
            RifugioVisitatoViewHolder(view, onRifugioClick)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_feed_amici, parent, false)
            GenericFeedViewHolder(view)
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

    // ViewHolder generico
    class GenericFeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarContainer: LinearLayout = itemView.findViewById(R.id.avatarContainer)
        private val avatarInitials: TextView = itemView.findViewById(R.id.avatarInitials)
        private val textNotifica: TextView = itemView.findViewById(R.id.textNotifica)
        private val textTempo: TextView = itemView.findViewById(R.id.textTempo)

        fun bind(item: HomeViewModel.FeedAmico) {
            val initials = getInitials(item.nomeUtente)
            avatarInitials.text = initials

            val colors = listOf("#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#F44336", "#00BCD4")
            val colorIndex = Math.abs(item.nomeUtente.hashCode() % colors.size)
            avatarContainer.setBackgroundColor(Color.parseColor(colors[colorIndex]))

            textNotifica.text = "${item.nomeUtente} ${item.testoAttivita}"
            textTempo.text = item.tempo
        }

        private fun getInitials(name: String): String {
            val parts = name.split(" ")
            return when {
                parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercase()}${parts[1].firstOrNull()?.uppercase()}"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "NA"
            }
        }
    }

    // ViewHolder rifugio visitato
    class RifugioVisitatoViewHolder(
        itemView: View,
        private val onRifugioClick: (HomeViewModel.RifugioInfo) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val avatarContainer: LinearLayout = itemView.findViewById(R.id.avatarContainer)
        private val avatarInitials: TextView = itemView.findViewById(R.id.avatarInitials)
        private val textNotifica: TextView = itemView.findViewById(R.id.textNotifica)
        private val textTempo: TextView = itemView.findViewById(R.id.textTempo)
        private val nomeRifugio: TextView = itemView.findViewById(R.id.nomeRifugio)
        private val localitaRifugio: TextView = itemView.findViewById(R.id.localitaRifugio)
        private val altitudineRifugio: TextView = itemView.findViewById(R.id.altitudineRifugio)
        private val textPunti: TextView = itemView.findViewById(R.id.textPunti)
        private val imageRifugio: ImageView = itemView.findViewById(R.id.imageRifugio)
        private val rifugioDetailsContainer: LinearLayout? = itemView.findViewById(R.id.rifugioDetailsContainer)

        fun bind(item: HomeViewModel.FeedAmico) {
            // Setup avatar e header
            val initials = getInitials(item.nomeUtente)
            avatarInitials.text = initials

            val colors = listOf("#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#F44336", "#00BCD4")
            val colorIndex = Math.abs(item.nomeUtente.hashCode() % colors.size)
            avatarContainer.setBackgroundColor(Color.parseColor(colors[colorIndex]))

            textNotifica.text = "${item.nomeUtente} ${item.testoAttivita}"
            textTempo.text = item.tempo

            // Setup dati rifugio se disponibili
            item.rifugioInfo?.let { rifugioData ->
                nomeRifugio.text = rifugioData.nome
                localitaRifugio.text = rifugioData.localita
                altitudineRifugio.text = "Altitudine: ${rifugioData.altitudine} m"
                textPunti.text = "+${rifugioData.puntiGuadagnati} punti"

                val resId = rifugioData.immagine?.let { name ->
                    val id = itemView.context.resources.getIdentifier(name, "drawable", itemView.context.packageName)
                    if (id != 0) id else R.drawable.mountain_background
                } ?: R.drawable.mountain_background
                imageRifugio.setImageResource(resId)

                // Imposta click listener sulla sezione rifugio
                val clickableContainer = rifugioDetailsContainer ?: itemView
                clickableContainer.setOnClickListener {
                    onRifugioClick(rifugioData)
                }
                clickableContainer.isClickable = true
                clickableContainer.isFocusable = true
            }
        }

        private fun getInitials(name: String): String {
            val parts = name.split(" ")
            return when {
                parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercase()}${parts[1].firstOrNull()?.uppercase()}"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "NA"
            }
        }
    }
}