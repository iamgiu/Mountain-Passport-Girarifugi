package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
                // Imposta i dati dell'utente
                textViewFriendName.text = user.name
                textViewRefuges.text = "${user.refugesCount} rifugi"
                textViewPoints.text = "${user.points} pt"

                // AGGIORNATO: Carica l'immagine profilo reale se disponibile
                if (!user.profileImageUrl.isNullOrBlank()) {
                    Glide.with(itemView.context)
                        .load(user.profileImageUrl)
                        .placeholder(user.avatarResource)
                        .error(user.avatarResource)
                        .circleCrop()
                        .into(imageSecondPlace)
                } else {
                    imageSecondPlace.setImageResource(user.avatarResource)
                }

                // AGGIORNATO: Gestisce lo stato del bottone in base alle tre condizioni
                when {
                    user.isAlreadyFriend -> {
                        // Caso 1: Sono già amici
                        btnAddFriend.text = "Già Amici"
                        btnAddFriend.isEnabled = false
                        btnAddFriend.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            itemView.context.getColor(R.color.gray_light)
                        )
                        btnAddFriend.setTextColor(
                            itemView.context.getColor(R.color.gray)
                        )
                    }
                    user.isRequestSent -> {
                        // Caso 2: Richiesta già inviata
                        btnAddFriend.text = "Inviata"
                        btnAddFriend.isEnabled = false
                        btnAddFriend.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            itemView.context.getColor(R.color.blue)
                        )
                        btnAddFriend.setTextColor(
                            itemView.context.getColor(R.color.white)
                        )
                    }
                    else -> {
                        // Caso 3: Può inviare richiesta
                        btnAddFriend.text = if (user.id.startsWith("g")) "Unisciti" else "Aggiungi"
                        btnAddFriend.isEnabled = true
                        btnAddFriend.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            itemView.context.getColor(R.color.green)
                        )
                        btnAddFriend.setTextColor(
                            itemView.context.getColor(R.color.white)
                        )
                    }
                }

                // Click listener per la card (apre il profilo)
                cardViewFriend.setOnClickListener {
                    onUserClick(user)
                }

                // Click listener per il bottone (invia richiesta solo se abilitato)
                btnAddFriend.setOnClickListener {
                    if (!user.isAlreadyFriend && !user.isRequestSent) {
                        onAddFriendClick(user)
                    }
                }

                // Animazione di click per la card
                cardViewFriend.setOnTouchListener { view, motionEvent ->
                    when (motionEvent.action) {
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            view.animate().scaleX(1.0f).scaleY(1.0f).duration = 100
                        }
                    }
                    false
                }

                // Animazione di click per il bottone (solo se abilitato)
                btnAddFriend.setOnTouchListener { view, motionEvent ->
                    if (!user.isAlreadyFriend && !user.isRequestSent) {
                        when (motionEvent.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                view.animate().scaleX(0.95f).scaleY(0.95f).duration = 80
                            }
                            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                view.animate().scaleX(1.0f).scaleY(1.0f).duration = 80
                            }
                        }
                    }
                    false
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