package com.example.mountainpassport_girarifugi.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mountainpassport_girarifugi.R

class RifugiSalvatiAdapter(
    private val rifugi: List<HomeViewModel.RifugioCard>,
    private val onRifugioClick: (HomeViewModel.RifugioCard) -> Unit
) : RecyclerView.Adapter<RifugiSalvatiAdapter.RifugioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RifugioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_rifugio_salvato, parent, false)
        return RifugioViewHolder(view)
    }

    override fun onBindViewHolder(holder: RifugioViewHolder, position: Int) {
        val rifugio = rifugi[position]
        holder.bind(rifugio)
        holder.itemView.setOnClickListener { onRifugioClick(rifugio) }
    }

    override fun getItemCount(): Int = rifugi.size

    class RifugioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textNomeRifugio: TextView = itemView.findViewById(R.id.textNomeRifugio)
        private val textDistanza: TextView = itemView.findViewById(R.id.textDistanza)
        private val textAltitudine: TextView = itemView.findViewById(R.id.textAltitudine)
        private val backgroundImage: ImageView = itemView.findViewById(R.id.backgroundImage)

        fun bind(rifugio: HomeViewModel.RifugioCard) {
            textNomeRifugio.text = rifugio.nome
            textDistanza.text = rifugio.distanza
            textAltitudine.text = rifugio.altitudine

            if (rifugio.immagine.startsWith("http")) {
                Glide.with(itemView.context)
                    .load(rifugio.immagine)
                    .placeholder(R.drawable.mountain_background)
                    .error(R.drawable.mountain_background)
                    .centerCrop()
                    .into(backgroundImage)
            } else {
                val context = itemView.context
                val resId = context.resources.getIdentifier(
                    rifugio.immagine.lowercase(),
                    "drawable",
                    context.packageName
                )
                if (resId != 0) {
                    backgroundImage.setImageResource(resId)
                } else {
                    backgroundImage.setImageResource(R.drawable.mountain_background)
                }
            }
        }
    }
}