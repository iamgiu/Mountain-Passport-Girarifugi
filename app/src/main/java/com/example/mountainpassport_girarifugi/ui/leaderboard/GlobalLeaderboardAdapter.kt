package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

class GlobalLeaderboardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TOP3 = 0
        private const val VIEW_TYPE_ROW = 1
    }

    private var global: List<LeaderboardUser> = emptyList()

    fun submitList(newGlobal: List<LeaderboardUser>) {
        // I dati dovrebbero arrivare già ordinati e con le posizioni dal ViewModel
        global = newGlobal
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_TOP3 else VIEW_TYPE_ROW
    }

    override fun getItemCount(): Int {
        return if (global.isEmpty()) {
            0
        } else {
            1 + maxOf(0, global.size - 3)
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
                val top3 = global.take(3)
                holder.bind(
                    top3.getOrNull(0) ?: LeaderboardUser.empty(),
                    top3.getOrNull(1) ?: LeaderboardUser.empty(),
                    top3.getOrNull(2) ?: LeaderboardUser.empty()
                )
            }
            is RowViewHolder -> {
                // CORREZIONE: position 1 deve mostrare il 4° utente (index 3)
                val userIndex = position + 2

                if (userIndex < global.size) {
                    holder.bind(global[userIndex])
                }
            }
        }
    }

    // ViewHolder per il podio (top 3)
    class Top3ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(first: LeaderboardUser, second: LeaderboardUser, third: LeaderboardUser) {
            // Primo posto (centro)
            bindUser(
                itemView.findViewById(R.id.imageFirstPlace),
                itemView.findViewById(R.id.textFirstPlaceName),
                itemView.findViewById(R.id.textFirstPlaceScore),
                first
            )

            // Secondo posto (sinistra)
            bindUser(
                itemView.findViewById(R.id.imageSecondPlace),
                itemView.findViewById(R.id.textSecondPlaceName),
                itemView.findViewById(R.id.textSecondPlaceScore),
                second
            )

            // Terzo posto (destra)
            bindUser(
                itemView.findViewById(R.id.imageThirdPlace),
                itemView.findViewById(R.id.textThirdPlaceName),
                itemView.findViewById(R.id.textThirdPlaceScore),
                third
            )
        }

        private fun bindUser(imageView: ImageView, nameView: TextView, scoreView: TextView, user: LeaderboardUser) {
            nameView.text = user.name
            scoreView.text = user.points.toString()
            setProfileImage(imageView, user)
        }

        private fun setProfileImage(imageView: ImageView, user: LeaderboardUser) {
            val profileData = user.profileImageUrl
            if (!profileData.isNullOrBlank()) {
                try {
                    val base64Data = when {
                        profileData.startsWith("data:image") -> profileData.substringAfter("base64,")
                        profileData.startsWith("/9j/") || profileData.startsWith("iVBORw0KGgo") -> profileData
                        else -> null
                    }

                    if (base64Data != null) {
                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                        } else {
                            imageView.setImageResource(user.avatarResource)
                        }
                    } else {
                        imageView.setImageResource(user.avatarResource)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    imageView.setImageResource(user.avatarResource)
                }
            } else {
                imageView.setImageResource(user.avatarResource)
            }
        }
    }

    // ViewHolder per le righe normali (dal 4° posto in poi)
    class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: LeaderboardUser) {
            itemView.findViewById<TextView>(R.id.textViewPosition).text = "${user.position}°"
            itemView.findViewById<TextView>(R.id.textViewFriendName).text = user.name
            itemView.findViewById<TextView>(R.id.textViewPoints).text = "${user.points} pt"
            itemView.findViewById<TextView>(R.id.textViewRefuges).text = "${user.refugesCount} rifugi"

            val avatarImageView = itemView.findViewById<ImageView>(R.id.imageSecondPlace)
            avatarImageView?.let { setProfileImage(it, user) }
        }

        private fun setProfileImage(imageView: ImageView, user: LeaderboardUser) {
            val profileData = user.profileImageUrl
            if (!profileData.isNullOrBlank()) {
                try {
                    val base64Data = when {
                        profileData.startsWith("data:image") -> profileData.substringAfter("base64,")
                        profileData.startsWith("/9j/") || profileData.startsWith("iVBORw0KGgo") -> profileData
                        else -> null
                    }

                    if (base64Data != null) {
                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                        } else {
                            imageView.setImageResource(user.avatarResource)
                        }
                    } else {
                        imageView.setImageResource(user.avatarResource)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    imageView.setImageResource(user.avatarResource)
                }
            } else {
                imageView.setImageResource(user.avatarResource)
            }
        }
    }
}