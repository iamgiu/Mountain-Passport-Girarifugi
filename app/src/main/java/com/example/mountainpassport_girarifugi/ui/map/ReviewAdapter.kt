package com.example.mountainpassport_girarifugi.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
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

    /**
     * Mette i dati dell'utente corrispondente nel item_review
     */
    class ReviewViewHolder(private val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            with(binding) {
                userNameTextView.text = if (review.userName.isNotBlank() && review.userName != "Utente Anonimo") {
                    review.userName
                } else {
                    "Utente ${review.userId.take(6)}"
                }

                ratingTextView.text = "⭐ ${review.rating}"

                commentTextView.text = review.comment

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateTextView.text = dateFormat.format(review.timestamp.toDate())

                loadUserAvatar(review.userAvatar)

                android.util.Log.d("ReviewAdapter", "Review bound - User: '${review.userName}', Avatar: '${review.userAvatar}'")
            }
        }

        /**
         * Caricamente immagine profilo dell'utente
         */
        private fun loadUserAvatar(avatarUrl: String?) {
            if (!avatarUrl.isNullOrBlank()) {
                try {
                    val base64Data = when {
                        avatarUrl.startsWith("data:image") -> {
                            // Ha il prefisso MIME completo
                            avatarUrl.substringAfter("base64,")
                        }
                        avatarUrl.startsWith("/9j/") || avatarUrl.startsWith("iVBORw0KGgo") -> {
                            avatarUrl
                        }
                        avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://") -> {
                            com.bumptech.glide.Glide.with(itemView.context)
                                .load(avatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_person_24)
                                .error(R.drawable.ic_person_24)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .timeout(10000)
                                .into(binding.userAvatarImageView)
                            return
                        }
                        else -> null
                    }

                    if (base64Data != null) {
                        val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        if (bitmap != null) {
                            val circularBitmap = createCircularBitmap(bitmap)
                            binding.userAvatarImageView.setImageBitmap(circularBitmap)
                        } else {
                            binding.userAvatarImageView.setImageResource(R.drawable.ic_person_24)
                        }
                    } else {
                        binding.userAvatarImageView.setImageResource(R.drawable.ic_person_24)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ReviewAdapter", "Errore caricamento avatar", e)
                    binding.userAvatarImageView.setImageResource(R.drawable.ic_person_24)
                }
            } else {
                binding.userAvatarImageView.setImageResource(R.drawable.ic_person_24)
            }
        }

        private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
            val size = kotlin.math.min(bitmap.width, bitmap.height)
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = Rect(0, 0, size, size)

            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)

            return output
        }
    }
}
