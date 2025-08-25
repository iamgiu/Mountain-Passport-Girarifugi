package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

class FriendsLeaderboardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TOP3 = 0
        private const val VIEW_TYPE_ROW = 1
    }

    private var friends: List<LeaderboardUser> = emptyList()

    fun submitList(newFriends: List<LeaderboardUser>) {
        friends = newFriends
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_TOP3 else VIEW_TYPE_ROW
    }

    override fun getItemCount(): Int {
        return if (friends.isEmpty()) 0 else {
            // Se ci sono almeno 3 elementi, mostra top3 + il resto
            // Se ci sono meno di 3, mostra solo le righe normali
            if (friends.size >= 3) {
                1 + (friends.size - 3) // 1 per top3 + resto
            } else {
                friends.size // Solo righe normali
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TOP3 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_top3_leaderboard, parent, false)
                Top3ViewHolder(view)
            }
            VIEW_TYPE_ROW -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_leaderboard_row, parent, false)
                RowViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is Top3ViewHolder -> {
                // Prendi i primi 3 utenti per il podio
                val top3 = friends.take(3)
                if (top3.size >= 3) {
                    holder.bind(top3[0], top3[1], top3[2]) // 1°, 2°, 3°
                }
            }
            is RowViewHolder -> {
                // Per gli altri utenti (dal 4° in poi)
                val userIndex = position + 2 // +2 perché i primi 3 sono nel podio
                if (userIndex < friends.size) {
                    val user = friends[userIndex]
                    holder.bind(user)
                }
            }
        }
    }

    // ViewHolder per il podio (top 3)
    class Top3ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(first: LeaderboardUser, second: LeaderboardUser, third: LeaderboardUser) {
            // Primo posto (centro)
            itemView.findViewById<TextView>(R.id.textFirstPlaceName).text = first.name
            itemView.findViewById<TextView>(R.id.textFirstPlaceScore).text = first.points.toString()
            itemView.findViewById<ImageView>(R.id.imageFirstPlace).setImageResource(first.avatarResource)

            // Secondo posto (sinistra)
            itemView.findViewById<TextView>(R.id.textSecondPlaceName).text = second.name
            itemView.findViewById<TextView>(R.id.textSecondPlaceScore).text = second.points.toString()
            itemView.findViewById<ImageView>(R.id.imageSecondPlace).setImageResource(second.avatarResource)

            // Terzo posto (destra)
            itemView.findViewById<TextView>(R.id.textThirdPlaceName).text = third.name
            itemView.findViewById<TextView>(R.id.textThirdPlaceScore).text = third.points.toString()
            itemView.findViewById<ImageView>(R.id.imageThirdPlace).setImageResource(third.avatarResource)
        }
    }

    // ViewHolder per le righe normali (dal 4° posto in poi)
    class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(user: LeaderboardUser) {
            itemView.findViewById<TextView>(R.id.textViewPosition).text = "${user.position}"
            itemView.findViewById<TextView>(R.id.textViewFriendName).text = user.name
            itemView.findViewById<TextView>(R.id.textViewPoints).text = "${user.points}"
            itemView.findViewById<TextView>(R.id.textViewRefuges).text = "${user.refugesCount} rifugi"

            // Imposta l'avatar corretto
            itemView.findViewById<ImageView>(R.id.imageSecondPlace)?.setImageResource(user.avatarResource)
        }
    }
}