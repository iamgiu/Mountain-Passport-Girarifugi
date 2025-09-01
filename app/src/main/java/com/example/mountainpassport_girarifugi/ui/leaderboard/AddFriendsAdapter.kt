package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.databinding.ItemProfileAddFriendBinding

class AddFriendsAdapter(
    private val onUserClick: (AddFriendUser) -> Unit,
    private val onAddFriendClick: (AddFriendUser) -> Unit
) : ListAdapter<AddFriendUser, AddFriendsAdapter.AddFriendViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddFriendViewHolder {
        val binding = ItemProfileAddFriendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AddFriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddFriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AddFriendViewHolder(
        private val binding: ItemProfileAddFriendBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(user: AddFriendUser) {
            binding.apply {
                textViewFriendName.text = user.name
                textViewRefuges.text = "${user.refugesCount} rifugi"
                textViewPoints.text = "${user.points} pt"

                val profileImageView = binding.imageViewProfile

                val profileData = user.profileImageUrl
                if (!profileData.isNullOrBlank()) {
                    try {
                        val base64Data = if (profileData.startsWith("data:image")) {

                            profileData.substringAfter("base64,")
                        } else if (profileData.startsWith("/9j/") || profileData.startsWith("iVBORw0KGgo")) {
                            profileData
                        } else {
                            null
                        }

                        if (base64Data != null) {
                            val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            if (bitmap != null) {
                                profileImageView.setImageBitmap(bitmap)
                            } else {
                                profileImageView.setImageResource(user.avatarResource)
                            }
                        } else {
                            profileImageView.setImageResource(user.avatarResource)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        profileImageView.setImageResource(user.avatarResource)
                    }
                } else {
                    profileImageView.setImageResource(user.avatarResource)
                }

                /**
                 * Cambia il tasto AddFriend
                 */
                when {
                    user.isAlreadyFriend -> {
                        btnAddFriend.text = "Già Amici"
                        btnAddFriend.isEnabled = false
                        btnAddFriend.backgroundTintList = android.content.res.ColorStateList.valueOf(itemView.context.getColor(R.color.gray_light))
                        btnAddFriend.setTextColor(itemView.context.getColor(R.color.gray))
                    }
                    user.isRequestSent -> {
                        btnAddFriend.text = "Inviata"
                        btnAddFriend.isEnabled = false
                        btnAddFriend.backgroundTintList = android.content.res.ColorStateList.valueOf(itemView.context.getColor(R.color.blue))
                        btnAddFriend.setTextColor(itemView.context.getColor(R.color.white))
                    }
                    else -> {
                        btnAddFriend.text = if (user.id.startsWith("g")) "Unisciti" else "Aggiungi"
                        btnAddFriend.isEnabled = true
                        btnAddFriend.backgroundTintList = android.content.res.ColorStateList.valueOf(itemView.context.getColor(R.color.green))
                        btnAddFriend.setTextColor(itemView.context.getColor(R.color.white))
                    }
                }

                cardViewFriend.setOnClickListener { onUserClick(user) }
                btnAddFriend.setOnClickListener {
                    if (!user.isAlreadyFriend && !user.isRequestSent) {
                        onAddFriendClick(user)
                    }
                }

                cardViewFriend.setOnClickListener { onUserClick(user) }
                btnAddFriend.setOnClickListener {
                    if (!user.isAlreadyFriend && !user.isRequestSent) {
                        onAddFriendClick(user)
                    }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AddFriendUser>() {
        override fun areItemsTheSame(oldItem: AddFriendUser, newItem: AddFriendUser): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AddFriendUser, newItem: AddFriendUser): Boolean {
            return oldItem == newItem
        }
    }
}