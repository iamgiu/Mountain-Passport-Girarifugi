package com.example.mountainpassport_girarifugi.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.repository.PointsRepository
import com.example.mountainpassport_girarifugi.data.repository.RifugioRepository
import com.example.mountainpassport_girarifugi.ui.profile.Stamp
import com.example.mountainpassport_girarifugi.ui.profile.StampsAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ProfileFriendFragment : Fragment() {

    private lateinit var stampsRecyclerView: RecyclerView
    private lateinit var stampsAdapter: StampsAdapter

    private val pointsRepository by lazy { PointsRepository(requireContext()) }
    private val rifugioRepository by lazy { RifugioRepository(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        val profileImageUrl = arguments?.getString("USER_PROFILE_IMAGE_URL")
        val friendId = arguments?.getString("USER_ID")

        // Popola le view
        view.findViewById<TextView>(R.id.fullNameTextView).text = name
        view.findViewById<TextView>(R.id.usernameTextView).text = username
        view.findViewById<TextView>(R.id.monthlyScoreTextView).text = "$points"
        view.findViewById<TextView>(R.id.visitedRefugesTextView).text = "$refuges"

        val profileImageView = view.findViewById<ImageView>(R.id.profileImageView)
        setProfileImage(profileImageView, profileImageUrl, avatar)

        // Setup RecyclerView timbri
        stampsRecyclerView = view.findViewById(R.id.stampsRecyclerView)
        stampsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        stampsAdapter = StampsAdapter(emptyList())
        stampsRecyclerView.adapter = stampsAdapter

        // Carica i timbri dell’amico
        if (!friendId.isNullOrEmpty()) {
            loadFriendStamps(friendId)
        }

        setupClickListeners(view)
    }

    private fun setProfileImage(imageView: ImageView, profileImageUrl: String?, defaultAvatar: Int) {
        if (!profileImageUrl.isNullOrBlank()) {
            try {
                val base64Data = when {
                    profileImageUrl.startsWith("data:image") ->
                        profileImageUrl.substringAfter("base64,")
                    profileImageUrl.startsWith("/9j/") || profileImageUrl.startsWith("iVBORw0KGgo") ->
                        profileImageUrl
                    else -> null
                }

                if (base64Data != null) {
                    val decodedBytes =
                        android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                        decodedBytes,
                        0,
                        decodedBytes.size
                    )
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        imageView.setImageResource(defaultAvatar)
                    }
                } else {
                    imageView.setImageResource(defaultAvatar)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                imageView.setImageResource(defaultAvatar)
            }
        } else {
            imageView.setImageResource(defaultAvatar)
        }
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<FloatingActionButton>(R.id.fabBack).setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadFriendStamps(friendId: String) {
        lifecycleScope.launch {
            val visits = withContext(Dispatchers.IO) {
                pointsRepository.getUserVisits(friendId, 100)
            }

            val stamps = visits.mapNotNull { visit ->
                val rifugio = withContext(Dispatchers.IO) {
                    rifugioRepository.getRifugioById(visit.rifugioId)
                }
                rifugio?.let {
                    Stamp(
                        refugeName = it.nome,
                        date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(visit.visitDate.toDate()),
                        altitude = "${it.altitudine} m",
                        region = it.regione ?: it.localita,
                        imageResId = R.drawable.stamps
                    )
                }
            }
            stampsAdapter.updateStamps(stamps)
        }
    }

    companion object {
        fun newInstanceUser(
            userId: String,
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
                putString("USER_ID", userId)
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
            groupId: String,
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
                putString("USER_ID", groupId)
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
