package com.example.mountainpassport_girarifugi.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

// Data class per le recensioni
data class Review(
    val id: String,
    val userName: String,
    val userAvatar: Int, // Resource ID per l'avatar
    val rating: Float,
    val reviewText: String,
    val date: String
)

class ReviewAdapter : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userAvatar: ImageView = itemView.findViewById(R.id.imageSecondPlace)
        private val userName: TextView = itemView.findViewById(R.id.userNameTextView)
        private val ratingStars: TextView = itemView.findViewById(R.id.ratingStarsTextView)
        private val ratingValue: TextView = itemView.findViewById(R.id.ratingValueTextView)
        private val reviewText: TextView = itemView.findViewById(R.id.reviewTextView)
        private val reviewDate: TextView = itemView.findViewById(R.id.reviewDateTextView)

        fun bind(review: Review) {
            // Imposta i dati della recensione
            userName.text = review.userName
            userAvatar.setImageResource(review.userAvatar)
            ratingValue.text = review.rating.toString()
            reviewText.text = review.reviewText
            reviewDate.text = review.date

            // Genera le stelle in base al rating
            ratingStars.text = generateStarsString(review.rating)
        }

        private fun generateStarsString(rating: Float): String {
            val fullStars = rating.toInt()
            val hasHalfStar = rating - fullStars >= 0.5f
            val emptyStars = 5 - fullStars - if (hasHalfStar) 1 else 0

            val stars = StringBuilder()

            // Stelle piene
            repeat(fullStars) {
                stars.append("★")
            }

            // Stella a metà (se necessaria)
            if (hasHalfStar) {
                stars.append("☆")
            }

            // Stelle vuote
            repeat(emptyStars) {
                stars.append("☆")
            }

            return stars.toString()
        }
    }

    class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem == newItem
        }
    }
}