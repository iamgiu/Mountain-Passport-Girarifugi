package com.example.mountainpassport_girarifugi.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.example.mountainpassport_girarifugi.data.model.TipoRifugio

class SearchCabinAdapter(
    private val onRifugioClick: (Rifugio) -> Unit,
    private val getDistance: (Rifugio) -> String
) : ListAdapter<Rifugio, SearchCabinAdapter.RifugioViewHolder>(RifugioDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RifugioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cabin, parent, false)
        return RifugioViewHolder(view)
    }

    override fun onBindViewHolder(holder: RifugioViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RifugioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardViewCabin)
        private val imageRifugio: ImageView = itemView.findViewById(R.id.imageRifugio)
        private val nomeRifugio: TextView = itemView.findViewById(R.id.nomeRifugio)
        private val localitaRifugio: TextView = itemView.findViewById(R.id.localitaRifugio)
        private val altitudineRifugio: TextView = itemView.findViewById(R.id.altitudineRifugio)
        private val textPunti: TextView = itemView.findViewById(R.id.textPunti)
        private val arrowDetail: ImageView = itemView.findViewById(R.id.arrowDetail)

        fun bind(rifugio: Rifugio) {
            nomeRifugio.text = rifugio.nome
            localitaRifugio.text = rifugio.localita

            // Mostra altitudine e distanza se disponibile
            val distance = getDistance(rifugio)
            altitudineRifugio.text = if (distance.isNotEmpty()) {
                "${rifugio.altitudine} m s.l.m. • $distance"
            } else {
                "${rifugio.altitudine} m s.l.m."
            }

            // Imposta l'icona in base al tipo di rifugio
            setRifugioIcon(rifugio.tipo)

            // Gestisce il click
            cardView.setOnClickListener {
                onRifugioClick(rifugio)
            }

            // Animazione di click
            cardView.setOnTouchListener { _, _ ->
                // Aggiunge un effetto visivo al tocco
                false
            }
        }

        private fun setRifugioIcon(tipo: TipoRifugio) {
            val (imageRes, colorRes) = when (tipo) {
                TipoRifugio.RIFUGIO -> Pair(R.drawable.rifugio_torino, R.color.brown)
                TipoRifugio.BIVACCO -> Pair(R.drawable.rifugio_torino, R.color.blue_black)
                TipoRifugio.CAPANNA -> Pair(R.drawable.rifugio_torino, R.color.green)
            }

            imageRifugio.setImageResource(imageRes)
            // Se vuoi colorare l'immagine:
            // imageRifugio.setColorFilter(ContextCompat.getColor(itemView.context, colorRes))
        }
    }

    class RifugioDiffCallback : DiffUtil.ItemCallback<Rifugio>() {
        override fun areItemsTheSame(oldItem: Rifugio, newItem: Rifugio): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Rifugio, newItem: Rifugio): Boolean {
            return oldItem == newItem
        }
    }
}