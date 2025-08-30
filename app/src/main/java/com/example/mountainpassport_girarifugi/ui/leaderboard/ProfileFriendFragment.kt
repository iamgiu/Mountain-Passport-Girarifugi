package com.example.mountainpassport_girarifugi.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mountainpassport_girarifugi.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfileFriendFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val type = arguments?.getString("TYPE") ?: "USER"

        return if (type == "USER") {
            inflater.inflate(R.layout.fragment_profile_leaderboard, container, false)
        } else {
            inflater.inflate(R.layout.fragment_profile_group_leaderboard, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = arguments?.getString("USER_NAME") ?: "Nome sconosciuto"
        val username = arguments?.getString("USER_USERNAME") ?: ""
        val points = arguments?.getInt("USER_POINTS") ?: 0
        val refuges = arguments?.getInt("USER_REFUGES") ?: 0
        val avatar = arguments?.getInt("USER_AVATAR") ?: R.drawable.avatar_sara
        val profileImageUrl = arguments?.getString("USER_PROFILE_IMAGE_URL") // AGGIUNGI QUESTA RIGA

        // Popola le view
        view.findViewById<TextView>(R.id.fullNameTextView).text = name
        view.findViewById<TextView>(R.id.usernameTextView).text = username
        view.findViewById<TextView>(R.id.monthlyScoreTextView).text = "$points"
        view.findViewById<TextView>(R.id.visitedRefugesTextView).text = "$refuges"

        // GESTIONE IMMAGINE PROFILO (come in AddFriendsAdapter)
        val profileImageView = view.findViewById<ImageView>(R.id.profileImageView)
        setProfileImage(profileImageView, profileImageUrl, avatar)

        setupClickListeners(view)
    }

    private fun setProfileImage(imageView: ImageView, profileImageUrl: String?, defaultAvatar: Int) {
        if (!profileImageUrl.isNullOrBlank()) {
            try {
                val base64Data = when {
                    profileImageUrl.startsWith("data:image") -> {
                        // Ha il prefisso MIME completo
                        profileImageUrl.substringAfter("base64,")
                    }
                    profileImageUrl.startsWith("/9j/") || profileImageUrl.startsWith("iVBORw0KGgo") -> {
                        // È già Base64 puro (JPEG inizia con /9j/, PNG con iVBORw0KGgo)
                        profileImageUrl
                    }
                    else -> {
                        // Caso URL remoto o altro formato
                        null
                    }
                }

                if (base64Data != null) {
                    val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        imageView.setImageResource(defaultAvatar)
                    }
                } else {
                    // URL remoto - qui potresti usare Glide se necessario
                    imageView.setImageResource(defaultAvatar)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                imageView.setImageResource(defaultAvatar)
            }
        } else {
            // Nessuna immagine, usa avatar locale
            imageView.setImageResource(defaultAvatar)
        }
    }

    private fun setupClickListeners(view: View) {
        // Back button - usa Navigation Component
        view.findViewById<FloatingActionButton>(R.id.fabBack).setOnClickListener {
            findNavController().navigateUp()
        }
    }

    companion object {
        fun newInstanceUser(
            name: String,
            username: String,
            points: Int,
            refuges: Int,
            avatar: Int,
            profileImageUrl: String?
        ): ProfileFriendFragment {
            val fragment = ProfileFriendFragment()
            val args = Bundle().apply {
                putString("TYPE", "USER")
                putString("USER_NAME", name)
                putString("USER_USERNAME", username)
                putInt("USER_POINTS", points)
                putInt("USER_REFUGES", refuges)
                putInt("USER_AVATAR", avatar)
                putString("USER_PROFILE_IMAGE_URL", profileImageUrl)
            }
            fragment.arguments = args
            return fragment
        }

        fun newInstanceGroup(
            name: String,
            username: String,
            points: Int,
            refuges: Int,
            avatar: Int,
            profileImageUrl: String?
        ): ProfileFriendFragment {
            val fragment = ProfileFriendFragment()
            val args = Bundle().apply {
                putString("TYPE", "GROUP")
                putString("USER_NAME", name)
                putString("USER_USERNAME", username)
                putInt("USER_POINTS", points)
                putInt("USER_REFUGES", refuges)
                putInt("USER_AVATAR", avatar)
                putString("USER_PROFILE_IMAGE_URL", profileImageUrl)
            }
            fragment.arguments = args
            return fragment
        }
    }
}