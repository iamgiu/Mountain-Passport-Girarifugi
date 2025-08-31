package com.example.mountainpassport_girarifugi.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

class StampsAdapter(private var stamps: List<Stamp>) : RecyclerView.Adapter<StampsAdapter.StampViewHolder>() {

    class StampViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val refugeNameTextView: TextView = itemView.findViewById(R.id.stampNameTextView)
        val stampImageView: ImageView = itemView.findViewById(R.id.stampImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StampViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stamp, parent, false)
        return StampViewHolder(view)
    }

    override fun onBindViewHolder(holder: StampViewHolder, position: Int) {
        val stamp = stamps[position]

        holder.refugeNameTextView.text = "${stamp.refugeName}\n${stamp.date}"
        holder.stampImageView.setImageResource(stamp.imageResId)
    }

    override fun getItemCount(): Int = stamps.size

    fun updateStamps(newStamps: List<Stamp>) {
        stamps = newStamps
        notifyDataSetChanged()
    }
}
