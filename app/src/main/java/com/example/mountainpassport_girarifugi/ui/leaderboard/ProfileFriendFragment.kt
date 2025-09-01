package com.example.mountainpassport_girarifugi.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.ui.profile.StampsAdapter
import com.example.mountainpassport_girarifugi.ui.profile.ProfileFriendViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfileFriendFragment : Fragment() {

    companion object {
        private const val TAG = "ProfileFriendFragment"
    }

    private lateinit var stampsRecyclerView: RecyclerView
    private lateinit var stampsAdapter: StampsAdapter

    private val friendId by lazy {
        val id = arguments?.getString("USER_ID") ?: ""
        Log.d(TAG, "FriendId recuperato: '$id'")
        id
    }

    private val viewModel: ProfileFriendViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "Creando vista")
        return inflater.inflate(R.layout.fragment_profile_leaderboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "Vista creata, inizializzando")

        logArguments()

        populateUserData(view)

        setupRecyclerView(view)

        setupObservers()

        setupClickListeners(view)

        if (friendId.isNotEmpty()) {
            Log.d(TAG, "Caricando timbri per: $friendId")
            viewModel.loadStamps(friendId)
        } else {
            Log.e(TAG, "FriendId vuoto, impossibile caricare timbri")
            showError("ID utente non valido")
        }
    }

    private fun logArguments() {
        Log.d(TAG, "Argomenti ricevuti:")
        arguments?.let { args ->
            args.keySet().forEach { key ->
                Log.d(TAG, "  $key: ${args.get(key)}")
            }
        } ?: Log.d(TAG, "  Nessun argomento ricevuto")
    }

    private fun populateUserData(view: View) {
        val name = arguments?.getString("USER_NAME") ?: "Nome sconosciuto"
        val username = arguments?.getString("USER_USERNAME") ?: ""
        val points = arguments?.getInt("USER_POINTS") ?: 0
        val refuges = arguments?.getInt("USER_REFUGES") ?: 0
        val avatar = arguments?.getInt("USER_AVATAR") ?: R.drawable.ic_account_circle_24
        val profileImageUrl = arguments?.getString("USER_PROFILE_IMAGE_URL")

        Log.d(TAG, "Dati utente: name=$name, username=$username, points=$points, refuges=$refuges")

        view.findViewById<TextView>(R.id.fullNameTextView).text = name
        view.findViewById<TextView>(R.id.usernameTextView).text = username
        view.findViewById<TextView>(R.id.monthlyScoreTextView).text = "$points"
        view.findViewById<TextView>(R.id.visitedRefugesTextView).text = "$refuges"

        val profileImageView = view.findViewById<ImageView>(R.id.profileImageView)
        setProfileImage(profileImageView, profileImageUrl, avatar)
    }

    private fun setupRecyclerView(view: View) {
        Log.d(TAG, "Setup RecyclerView...")

        stampsRecyclerView = view.findViewById(R.id.stampsRecyclerView)
        stampsRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        stampsAdapter = StampsAdapter(emptyList())
        stampsRecyclerView.adapter = stampsAdapter

        Log.d(TAG, "RecyclerView configurata")
    }

    private fun setupObservers() {
        Log.d(TAG, "Setup observers...")

        // Observer per i timbri
        viewModel.stamps.observe(viewLifecycleOwner) { stamps ->
            Log.d(TAG, "Timbri ricevuti: ${stamps.size}")
            stamps.forEach { stamp ->
                Log.d(TAG, "  - ${stamp.rifugioName}")
            }
            stampsAdapter.updateStamps(stamps)

            if (stamps.isEmpty()) {
                showToast("Nessun timbro trovato per questo utente")
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "Loading: $isLoading")
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Log.e(TAG, "Errore: $errorMessage")
                showError(errorMessage)
            }
        }
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
                Log.e(TAG, "Errore caricamento immagine profilo: ${e.message}")
                imageView.setImageResource(defaultAvatar)
            }
        } else {
            imageView.setImageResource(defaultAvatar)
        }
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<FloatingActionButton>(R.id.fabBack)?.setOnClickListener {
            Log.d(TAG, "Tornando indietro...")
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), "Errore: $message", Toast.LENGTH_LONG).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}