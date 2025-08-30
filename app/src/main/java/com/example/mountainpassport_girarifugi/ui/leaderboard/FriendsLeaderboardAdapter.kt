package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FriendsLeaderboardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TOP3 = 0
        private const val VIEW_TYPE_ROW = 1
    }

    private var friends: List<LeaderboardUser> = emptyList()

    fun submitList(newFriends: List<LeaderboardUser>) {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedFriends = newFriends.map { user ->
                try {
                    val stats = getUserStatsFromPoints(user.id)
                    user.copy(
                        points = stats.first,
                        refugesCount = stats.second
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    user
                }
            }.sortedByDescending { it.points } // <-- ordina dal punteggio più alto

            // Aggiorna posizione (1°, 2°, 3°, ...)
            val withPosition = updatedFriends.mapIndexed { index, user ->
                user.copy(position = index + 1)
            }

            withContext(Dispatchers.Main) {
                friends = withPosition
                notifyDataSetChanged()
            }
        }
    }

    // Funzione sospesa per ottenere i dati reali
    private suspend fun getUserStatsFromPoints(userId: String): Pair<Int, Int> {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val userStatsDoc = firestore.collection("user_points_stats")
                .document(userId)
                .get()
                .await()

            val userStats = userStatsDoc.toObject(com.example.mountainpassport_girarifugi.data.model.UserPointsStats::class.java)
            if (userStats != null) {
                Pair(userStats.totalPoints, userStats.totalVisits)
            } else {
                Pair(0, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(0, 0)
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (position == 0 && friends.size >= 3) VIEW_TYPE_TOP3 else VIEW_TYPE_ROW
    }

    override fun getItemCount(): Int {
        return if (friends.isEmpty()) {
            0
        } else if (friends.size >= 3) {
            1 + (friends.size - 3) // 1 per top3 + resto
        } else {
            1 // Mostra comunque un “top” anche se ci sono meno di 3 utenti
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
                // Prendi i primi 3 utenti per il podio (se ci sono almeno 3)
                val top3 = friends.take(3)
                if (top3.size == 3) {
                    holder.bind(top3[0], top3[1], top3[2])
                } else {
                    // In caso ci siano meno di 3 utenti, mostra solo quelli disponibili
                    val first = top3.getOrNull(0)
                    val second = top3.getOrNull(1)
                    val third = top3.getOrNull(2)
                    holder.bind(
                        first ?: LeaderboardUser.empty(),
                        second ?: LeaderboardUser.empty(),
                        third ?: LeaderboardUser.empty()
                    )
                }
            }

            is RowViewHolder -> {
                val userIndex = if (friends.size >= 3) {
                    position + 3 // Skip dei primi 3 per il podio
                } else {
                    position // Se meno di 3 utenti, usa la posizione direttamente
                }

                if (userIndex < friends.size) {
                    holder.bind(friends[userIndex])
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
            setProfileImage(itemView.findViewById<ImageView>(R.id.imageFirstPlace), first)

            // Secondo posto (sinistra)
            itemView.findViewById<TextView>(R.id.textSecondPlaceName).text = second.name
            itemView.findViewById<TextView>(R.id.textSecondPlaceScore).text = second.points.toString()
            setProfileImage(itemView.findViewById<ImageView>(R.id.imageSecondPlace), second)

            // Terzo posto (destra)
            itemView.findViewById<TextView>(R.id.textThirdPlaceName).text = third.name
            itemView.findViewById<TextView>(R.id.textThirdPlaceScore).text = third.points.toString()
            setProfileImage(itemView.findViewById<ImageView>(R.id.imageThirdPlace), third)
        }

        private fun setProfileImage(imageView: ImageView, user: LeaderboardUser) {
            val profileData = user.profileImageUrl
            if (!profileData.isNullOrBlank()) {
                try {
                    val base64Data = when {
                        profileData.startsWith("data:image") -> {
                            profileData.substringAfter("base64,")
                        }
                        profileData.startsWith("/9j/") || profileData.startsWith("iVBORw0KGgo") -> {
                            profileData
                        }
                        else -> null
                    }

                    if (base64Data != null) {
                        val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
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

    // ViewHolder per le righe normali (dal 4° posto in poi o tutti se meno di 3)
    class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(user: LeaderboardUser) {
            itemView.findViewById<TextView>(R.id.textViewPosition).text = "${user.position}"
            itemView.findViewById<TextView>(R.id.textViewFriendName).text = user.name
            itemView.findViewById<TextView>(R.id.textViewPoints).text = "${user.points}"
            itemView.findViewById<TextView>(R.id.textViewRefuges).text = "${user.refugesCount} rifugi"

            // Prova diversi ID possibili per l'avatar
            val avatarImageView = itemView.findViewById<ImageView>(R.id.imageSecondPlace)

            avatarImageView?.let { setProfileImage(it, user) }
        }

        private fun setProfileImage(imageView: ImageView, user: LeaderboardUser) {
            val profileData = user.profileImageUrl
            if (!profileData.isNullOrBlank()) {
                try {
                    val base64Data = when {
                        profileData.startsWith("data:image") -> {
                            profileData.substringAfter("base64,")
                        }
                        profileData.startsWith("/9j/") || profileData.startsWith("iVBORw0KGgo") -> {
                            profileData
                        }
                        else -> null
                    }

                    if (base64Data != null) {
                        val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
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