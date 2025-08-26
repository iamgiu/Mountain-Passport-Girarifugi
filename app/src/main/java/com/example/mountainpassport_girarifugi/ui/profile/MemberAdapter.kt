package com.example.mountainpassport_girarifugi.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

class MembersAdapter(
    private var members: List<Member>,
    private val onMemberClickListener: (Member) -> Unit
) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memberNameTextView: TextView = itemView.findViewById(R.id.memberNameTextView)
        val memberImageView: ImageView = itemView.findViewById(R.id.memberImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]

        holder.memberNameTextView.text = member.fullName

        // Imposta l'immagine del profilo (per ora usa un'immagine di default)
        holder.memberImageView.setImageResource(R.drawable.avatar_mario)

        // Gestisci il click sull'item
        holder.itemView.setOnClickListener {
            onMemberClickListener(member)
        }
    }

    override fun getItemCount(): Int = members.size

    fun updateMembers(newMembers: List<Member>) {
        members = newMembers
        notifyDataSetChanged()
    }
}