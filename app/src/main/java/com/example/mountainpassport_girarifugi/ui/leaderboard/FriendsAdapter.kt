package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.databinding.ItemFriendLeaderboardBinding

class FriendsAdapter(
    private val friendsList: List<Friend>
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(friendsList[position], position + 1)
    }

    override fun getItemCount(): Int = friendsList.size

    class FriendViewHolder(
        private val binding: ItemFriendLeaderboardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: Friend, position: Int) {
            binding.apply {
                // Posizione nella classifica
                textViewPosition.text = "$position°"

                // Nome dell'amico
                textViewFriendName.text = friend.name

                // Punti
                textViewPoints.text = "${friend.points} pt"

                // Numero di rifugi visitati
                textViewRefuges.text = "${friend.refugesVisited} rifugi"

                // Colore di sfondo basato sulla posizione
                when (position) {
                    1 -> {
                        cardViewFriend.setCardBackgroundColor(
                            itemView.context.getColor(android.R.color.holo_orange_light)
                        )
                        textViewPosition.setTextColor(
                            itemView.context.getColor(android.R.color.white)
                        )
                    }
                    2 -> {
                        cardViewFriend.setCardBackgroundColor(
                            itemView.context.getColor(android.R.color.darker_gray)
                        )
                        textViewPosition.setTextColor(
                            itemView.context.getColor(android.R.color.white)
                        )
                    }
                    3 -> {
                        cardViewFriend.setCardBackgroundColor(
                            itemView.context.getColor(android.R.color.holo_orange_dark)
                        )
                        textViewPosition.setTextColor(
                            itemView.context.getColor(android.R.color.white)
                        )
                    }
                    else -> {
                        cardViewFriend.setCardBackgroundColor(
                            itemView.context.getColor(android.R.color.white)
                        )
                        textViewPosition.setTextColor(
                            itemView.context.getColor(android.R.color.black)
                        )
                    }
                }

                // Avatar placeholder (iniziali del nome)
                val initials = friend.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")
                textViewAvatar.text = initials
            }
        }
    }
}