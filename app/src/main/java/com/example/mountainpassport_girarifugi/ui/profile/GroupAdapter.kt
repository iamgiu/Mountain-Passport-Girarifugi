package com.example.mountainpassport_girarifugi.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

class GroupsAdapter(
    private var groups: List<Group>,
    private val onGroupClickListener: (Group) -> Unit
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupNameTextView: TextView = itemView.findViewById(R.id.groupNameTextView)
        val groupMembersTextView: TextView = itemView.findViewById(R.id.groupMembersTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]

        holder.groupNameTextView.text = group.name
        holder.groupMembersTextView.text = "${group.memberCount} membri"

        // Gestisci il click sull'item
        holder.itemView.setOnClickListener {
            onGroupClickListener(group)
        }
    }

    override fun getItemCount(): Int = groups.size

    fun updateGroups(newGroups: List<Group>) {
        groups = newGroups
        notifyDataSetChanged()
    }
}