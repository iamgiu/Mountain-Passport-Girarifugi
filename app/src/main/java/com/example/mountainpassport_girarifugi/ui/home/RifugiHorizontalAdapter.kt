package com.example.mountainpassport_girarifugi.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

class RifugiHorizontalAdapter(
    private val rifugi: List<HomeFragment.RifugioCard>,
    private val showBonusBadge: Boolean = false
) : RecyclerView.Adapter<RifugiHorizontalAdapter.RifugioViewHolder>() {

    private var onItemClickListener: ((HomeFragment.RifugioCard) -> Unit)? = null

    fun setOnItemClickListener(listener: (HomeFragment.RifugioCard) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RifugioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_rifugio, parent, false)
        return RifugioViewHolder(view)
    }

    override fun onBindViewHolder(holder: RifugioViewHolder, position: Int) {
        val rifugio = rifugi[position]
        holder.bind(rifugio, showBonusBadge)

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(rifugio)
        }
    }

    override fun getItemCount(): Int = rifugi.size

    class RifugioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageRifugio: ImageView = itemView.findViewById(R.id.imageRifugio)
        private val textNomeRifugio: TextView = itemView.findViewById(R.id.textNomeRifugio)
        private val textDistanza: TextView = itemView.findViewById(R.id.textDistanza)
        private val textAltitudine: TextView = itemView.findViewById(R.id.textAltitudine)
        private val textDifficolta: TextView = itemView.findViewById(R.id.textDifficolta)
        private val textTempo: TextView = itemView.findViewById(R.id.textTempo)
        private val badgeBonus: LinearLayout = itemView.findViewById(R.id.badgeBonus)
        private val textBonusPunti: TextView = itemView.findViewById(R.id.textBonusPunti)

        fun bind(rifugio: HomeFragment.RifugioCard, showBonusBadge: Boolean) {
            textNomeRifugio.text = rifugio.nome
            textDistanza.text = rifugio.distanza
            textAltitudine.text = rifugio.altitudine
            textDifficolta.text = rifugio.difficolta
            textTempo.text = rifugio.tempo

            // Gestione immagine rifugio
            val context = itemView.context
            val nomeRisorsa = rifugio.immagine.lowercase()
            val resId = context.resources.getIdentifier(nomeRisorsa, "drawable", context.packageName)

            if (resId != 0) {
                imageRifugio.setImageResource(resId)
            } else {
                // Immagine di fallback se non esiste
                imageRifugio.setImageResource(R.drawable.mountain_background)
            }

            // Gestione badge bonus punti
            if (showBonusBadge && rifugio.bonusPunti != null) {
                badgeBonus.visibility = View.VISIBLE
                textBonusPunti.text = "+${rifugio.bonusPunti}"
            } else {
                badgeBonus.visibility = View.GONE
            }
        }
    }
}