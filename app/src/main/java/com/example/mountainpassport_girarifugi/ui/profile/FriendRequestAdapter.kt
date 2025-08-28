package com.example.mountainpassport_girarifugi.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

class FriendRequestsAdapter(
    private val onAcceptClick: (FriendRequest) -> Unit,
    private val onDeclineClick: (FriendRequest) -> Unit
) : ListAdapter<FriendRequest, FriendRequestsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewSenderName)
        private val nicknameTextView: TextView = itemView.findViewById(R.id.textViewSenderNickname)
        private val acceptButton: Button = itemView.findViewById(R.id.buttonAccept)
        private val declineButton: Button = itemView.findViewById(R.id.buttonDecline)

        fun bind(request: FriendRequest) {
            nameTextView.text = request.senderName
            nicknameTextView.text = "@${request.senderNickname}"

            acceptButton.setOnClickListener { onAcceptClick(request) }
            declineButton.setOnClickListener { onDeclineClick(request) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FriendRequest>() {
        override fun areItemsTheSame(oldItem: FriendRequest, newItem: FriendRequest): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FriendRequest, newItem: FriendRequest): Boolean {
            return oldItem == newItem
        }
    }
}