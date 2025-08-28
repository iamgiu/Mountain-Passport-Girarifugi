package com.example.mountainpassport_girarifugi.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.data.model.Review
import com.example.mountainpassport_girarifugi.databinding.ItemReviewBinding
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {
    
    private var reviews: List<Review> = emptyList()
    
    fun updateReviews(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }
    
    override fun getItemCount(): Int = reviews.size
    
    class ReviewViewHolder(private val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(review: Review) {
            with(binding) {
                // Nome utente
                userNameTextView.text = review.userName
                
                // Rating
                ratingTextView.text = "⭐ ${review.rating}"
                
                // Commento
                commentTextView.text = review.comment
                
                // Data
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateTextView.text = dateFormat.format(review.timestamp.toDate())
                
                // Avatar (placeholder per ora)
                // userAvatarImageView.setImageResource(R.drawable.default_avatar)
            }
        }
    }
}