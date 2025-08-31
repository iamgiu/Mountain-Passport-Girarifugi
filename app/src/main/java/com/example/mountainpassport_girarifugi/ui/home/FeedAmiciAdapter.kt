package com.example.mountainpassport_girarifugi.ui.home

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.repository.FriendActivity
import com.example.mountainpassport_girarifugi.data.repository.ActivityType

class FeedAmiciAdapter(
    private val feedItems: List<FriendActivity>,
    private val onRifugioClick: ((String) -> Unit)? = null // rifugioId come parametro
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_RIFUGIO_VISITATO = 1
        private const val TYPE_GENERIC = 0
    }

    override fun getItemViewType(position: Int): Int {
        val item = feedItems[position]
        return if (item.activityType == ActivityType.RIFUGIO_VISITATO && item.rifugioId != null) {
            TYPE_RIFUGIO_VISITATO
        } else {
            TYPE_GENERIC
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_RIFUGIO_VISITATO) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_feed_rifugio_visitato, parent, false)
            RifugioVisitatoViewHolder(view, onRifugioClick)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_feed_amici, parent, false)
            GenericFeedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = feedItems[position]
        when (holder) {
            is RifugioVisitatoViewHolder -> holder.bind(item)
            is GenericFeedViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = feedItems.size

    // ViewHolder generico - SEMPLIFICATO
    class GenericFeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarImage: ImageView = itemView.findViewById(R.id.imageSecondPlace)
        private val textNotifica: TextView = itemView.findViewById(R.id.textNotifica)
        private val textTempo: TextView = itemView.findViewById(R.id.textTempo)

        fun bind(item: FriendActivity) {
            // Carica avatar utente con metodo semplificato
            loadUserAvatar(item.userAvatarUrl)

            // Imposta il testo dell'attività
            textNotifica.text = "${item.username} ${getActivityText(item)}"
            textTempo.text = item.timeAgo
        }

        private fun getActivityText(item: FriendActivity): String {
            return when (item.activityType) {
                ActivityType.RIFUGIO_VISITATO -> "ha visitato un rifugio"
                ActivityType.ACHIEVEMENT -> "ha ottenuto un achievement"
                ActivityType.PUNTI_GUADAGNATI -> "ha guadagnato ${item.pointsEarned} punti"
                ActivityType.RECENSIONE -> "ha lasciato una recensione"
                else -> item.title
            }
        }

        // METODO SEMPLIFICATO come in AddFriendsAdapter
        private fun loadUserAvatar(avatarUrl: String?) {
            if (!avatarUrl.isNullOrBlank()) {
                try {
                    val base64Data = when {
                        avatarUrl.startsWith("data:image") -> {
                            // Ha il prefisso MIME completo
                            avatarUrl.substringAfter("base64,")
                        }
                        avatarUrl.startsWith("/9j/") || avatarUrl.startsWith("iVBORw0KGgo") -> {
                            // È già Base64 puro (JPEG inizia con /9j/, PNG con iVBORw0KGgo)
                            avatarUrl
                        }
                        avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://") -> {
                            // URL remoto - usa Glide con crop circolare
                            Glide.with(itemView.context)
                                .load(avatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_account_circle_24)
                                .error(R.drawable.ic_account_circle_24)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .timeout(10000)
                                .into(avatarImage)
                            return
                        }
                        else -> null
                    }

                    if (base64Data != null) {
                        val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        if (bitmap != null) {
                            // Applica il crop circolare direttamente al bitmap
                            val circularBitmap = createCircularBitmap(bitmap)
                            avatarImage.setImageBitmap(circularBitmap)
                        } else {
                            avatarImage.setImageResource(R.drawable.ic_account_circle_24)
                        }
                    } else {
                        avatarImage.setImageResource(R.drawable.ic_account_circle_24)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FeedAmiciAdapter", "Errore caricamento avatar", e)
                    avatarImage.setImageResource(R.drawable.ic_account_circle_24)
                }
            } else {
                avatarImage.setImageResource(R.drawable.ic_account_circle_24)
            }
        }

        private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
            val size = minOf(bitmap.width, bitmap.height)
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

    // ViewHolder rifugio visitato
    class RifugioVisitatoViewHolder(
        itemView: View,
        private val onRifugioClick: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        private val avatarImage: ImageView = itemView.findViewById(R.id.imageSecondPlace)
        private val textNotifica: TextView = itemView.findViewById(R.id.textNotifica)
        private val textTempo: TextView = itemView.findViewById(R.id.textTempo)
        private val nomeRifugio: TextView = itemView.findViewById(R.id.nomeRifugio)
        private val localitaRifugio: TextView = itemView.findViewById(R.id.localitaRifugio)
        private val altitudineRifugio: TextView = itemView.findViewById(R.id.altitudineRifugio)
        private val textPunti: TextView = itemView.findViewById(R.id.textPunti)
        private val imageRifugio: ImageView = itemView.findViewById(R.id.imageRifugio)
        private val rifugioDetailsContainer: LinearLayout? = itemView.findViewById(R.id.rifugioDetailsContainer)

        fun bind(item: FriendActivity) {
            // Carica avatar utente con metodo semplificato
            loadUserAvatar(item.userAvatarUrl)

            // Setup header
            textNotifica.text = "${item.username} ha visitato un rifugio"
            textTempo.text = item.timeAgo

            // Setup dati rifugio
            nomeRifugio.text = item.rifugioName ?: "Nome non disponibile"
            localitaRifugio.text = item.rifugioLocation ?: "Località non disponibile"
            altitudineRifugio.text = item.rifugioAltitude?.let {
                if (it.contains("m")) "Altitudine: $it" else "Altitudine: ${it}m"
            } ?: "Altitudine non disponibile"
            textPunti.text = "+${item.pointsEarned} punti"

            // Carica immagine rifugio con metodo semplificato
            loadRifugioImage(item.rifugioImageUrl, item.rifugioName)

            // Imposta click listener
            item.rifugioId?.let { rifugioId ->
                rifugioDetailsContainer?.setOnClickListener {
                    onRifugioClick?.invoke(rifugioId)
                }
                rifugioDetailsContainer?.isClickable = true
                rifugioDetailsContainer?.isFocusable = true
            }
        }

        // METODO SEMPLIFICATO come in AddFriendsAdapter
        private fun loadUserAvatar(avatarUrl: String?) {
            if (!avatarUrl.isNullOrBlank()) {
                try {
                    val base64Data = when {
                        avatarUrl.startsWith("data:image") -> {
                            avatarUrl.substringAfter("base64,")
                        }
                        avatarUrl.startsWith("/9j/") || avatarUrl.startsWith("iVBORw0KGgo") -> {
                            avatarUrl
                        }
                        avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://") -> {
                            Glide.with(itemView.context)
                                .load(avatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_account_circle_24)
                                .error(R.drawable.ic_account_circle_24)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .timeout(10000)
                                .into(avatarImage)
                            return
                        }
                        else -> null
                    }

                    if (base64Data != null) {
                        val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        if (bitmap != null) {
                            val circularBitmap = createCircularBitmap(bitmap)
                            avatarImage.setImageBitmap(circularBitmap)
                        } else {
                            avatarImage.setImageResource(R.drawable.ic_account_circle_24)
                        }
                    } else {
                        avatarImage.setImageResource(R.drawable.ic_account_circle_24)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FeedAmiciAdapter", "Errore caricamento avatar", e)
                    avatarImage.setImageResource(R.drawable.ic_account_circle_24)
                }
            } else {
                avatarImage.setImageResource(R.drawable.ic_account_circle_24)
            }
        }

        private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
            val size = minOf(bitmap.width, bitmap.height)
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

        // METODO MIGLIORATO per immagini rifugio
        private fun loadRifugioImage(rifugioImageUrl: String?, rifugioName: String?) {
            if (!rifugioImageUrl.isNullOrBlank()) {
                try {
                    val base64Data = when {
                        rifugioImageUrl.startsWith("data:image") -> {
                            rifugioImageUrl.substringAfter("base64,")
                        }
                        rifugioImageUrl.startsWith("/9j/") || rifugioImageUrl.startsWith("iVBORw0KGgo") -> {
                            rifugioImageUrl
                        }
                        rifugioImageUrl.startsWith("http://") || rifugioImageUrl.startsWith("https://") -> {
                            // 🔹 Caso URL remoto
                            Glide.with(itemView.context)
                                .load(rifugioImageUrl)
                                .centerCrop()
                                .placeholder(R.drawable.mountain_background)
                                .error(getLocalRifugioImage(rifugioName))
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .timeout(10000)
                                .into(imageRifugio)
                            return
                        }
                        else -> {
                            // 🔹 Caso nome drawable locale (es. "rifugio_contrin")
                            val context = itemView.context
                            val resId = context.resources.getIdentifier(
                                rifugioImageUrl.lowercase(),
                                "drawable",
                                context.packageName
                            )
                            if (resId != 0) {
                                imageRifugio.setImageResource(resId)
                            } else {
                                val localImageResource = getLocalRifugioImage(rifugioName)
                                imageRifugio.setImageResource(localImageResource)
                            }
                            return
                        }
                    }

                    // 🔹 Caso base64
                    if (base64Data != null) {
                        val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        if (bitmap != null) {
                            imageRifugio.setImageBitmap(bitmap)
                        } else {
                            val localImageResource = getLocalRifugioImage(rifugioName)
                            imageRifugio.setImageResource(localImageResource)
                        }
                    } else {
                        val localImageResource = getLocalRifugioImage(rifugioName)
                        imageRifugio.setImageResource(localImageResource)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FeedAmiciAdapter", "Errore caricamento immagine rifugio", e)
                    val localImageResource = getLocalRifugioImage(rifugioName)
                    imageRifugio.setImageResource(localImageResource)
                }
            } else {
                // 🔹 Nessuna immagine disponibile → fallback
                val localImageResource = getLocalRifugioImage(rifugioName)
                imageRifugio.setImageResource(localImageResource)
            }
        }


        private fun getLocalRifugioImage(rifugioName: String?): Int {
            if (rifugioName.isNullOrBlank()) return R.drawable.mountain_background

            val imageMap = mapOf(
                "laghi gemelli" to "rifugio_laghi_gemelli",
                "monte bianco" to "rifugio_monte_bianco",
                "margherita" to "capanna_margherita",
                "torino" to "rifugio_torino",
                "laghi verdi" to "rifugio_laghi_verdi"
            )

            val normalizedName = rifugioName.lowercase()
            val imageName = imageMap.entries.find { normalizedName.contains(it.key) }?.value

            return if (imageName != null) {
                val resourceId = itemView.context.resources.getIdentifier(
                    imageName,
                    "drawable",
                    itemView.context.packageName
                )
                if (resourceId != 0) resourceId else R.drawable.mountain_background
            } else {
                R.drawable.mountain_background
            }
        }
    }
}