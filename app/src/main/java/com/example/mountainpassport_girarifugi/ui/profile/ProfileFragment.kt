package com.example.mountainpassport_girarifugi.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import android.widget.ImageView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log

class ProfileFragment : Fragment() {

    private lateinit var stampsRecyclerView: RecyclerView
    private lateinit var stampsAdapter: StampsAdapter
    private lateinit var groupsRecyclerView: RecyclerView

    // Views del profilo
    private lateinit var fullNameTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var monthlyScoreTextView: TextView
    private lateinit var visitedRefugesTextView: TextView
    private lateinit var profileImageView: ImageView

    // ViewModel
    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(requireContext())
    }

    // Flag per evitare observer multipli
    private var observersSetup = false
    private var profileImageLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            Log.d("ProfileFragment", "onViewCreated started")

            // Inizializza le view
            initViews(view)

            // Configura la RecyclerView
            setupStampsRecyclerView()

            // Configura gli observer per il ViewModel (solo una volta)
            if (!observersSetup) {
                setupObservers()
                observersSetup = true
            }

            // Setup settings FAB
            setupSettingsButton(view)

            Log.d("ProfileFragment", "onViewCreated completed")
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error in onViewCreated", e)
            showError("Errore nell'inizializzazione del profilo: ${e.message}")
        }
    }

    private fun initViews(view: View) {
        try {
            stampsRecyclerView = view.findViewById(R.id.stampsRecyclerView)
            fullNameTextView = view.findViewById(R.id.fullNameTextView)
            usernameTextView = view.findViewById(R.id.usernameTextView)
            monthlyScoreTextView = view.findViewById(R.id.monthlyScoreTextView)
            visitedRefugesTextView = view.findViewById(R.id.visitedRefugesTextView)
            profileImageView = view.findViewById(R.id.profileImageView)

            Log.d("ProfileFragment", "Views initialized successfully")
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error initializing views", e)
            throw e
        }
    }

    private fun setupStampsRecyclerView() {
        try {
            val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            stampsRecyclerView.layoutManager = layoutManager
            stampsAdapter = StampsAdapter(emptyList())
            stampsRecyclerView.adapter = stampsAdapter

            Log.d("ProfileFragment", "RecyclerView setup completed")
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error setting up RecyclerView", e)
            showError("Errore nell'inizializzazione della lista timbri")
        }
    }

    private fun setupObservers() {
        try {
            Log.d("ProfileFragment", "Setting up observers")

            // Observer per i dati del profilo
            viewModel.profileData.observe(viewLifecycleOwner) { profileData ->
                try {
                    if (profileData != null) {
                        updateProfileUI(profileData)

                        // Carica l'immagine profilo solo una volta quando i dati sono pronti
                        if (!profileImageLoaded) {
                            loadSavedProfileImage()
                            profileImageLoaded = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error updating profile UI", e)
                    showError("Errore nell'aggiornamento del profilo")
                }
            }

            // Observer per i timbri
            viewModel.stamps.observe(viewLifecycleOwner) { stamps ->
                try {
                    if (stamps != null) {
                        stampsAdapter.updateStamps(stamps)
                        Log.d("ProfileFragment", "Stamps updated: ${stamps.size}")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error updating stamps", e)
                }
            }

            // Observer per gli errori di caricamento
            viewModel.loadingError.observe(viewLifecycleOwner) { errorMessage ->
                try {
                    if (!errorMessage.isNullOrBlank()) {
                        showError(errorMessage)
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error showing error message", e)
                }
            }

            // Observer per lo stato di caricamento
            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                try {
                    if (isLoading == true) {
                        showLoadingState()
                    } else {
                        hideLoadingState()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error updating loading state", e)
                }
            }

            Log.d("ProfileFragment", "Observers setup completed")
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error setting up observers", e)
            showError("Errore nell'inizializzazione degli observer")
        }
    }

    private fun updateProfileUI(profileData: ProfileData) {
        try {
            fullNameTextView.text = profileData.fullName
            usernameTextView.text = profileData.username
            monthlyScoreTextView.text = profileData.monthlyScore
            visitedRefugesTextView.text = profileData.visitedRefuges

            Log.d("ProfileFragment", "Profile UI updated successfully")
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error updating profile UI", e)
        }
    }

    private fun showLoadingState() {
        try {
            fullNameTextView.text = "Caricamento..."
            usernameTextView.text = "Caricamento..."
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error showing loading state", e)
        }
    }

    private fun hideLoadingState() {
        // Lo stato normale sarÃ  ripristinato dagli observer dei dati
    }

    private fun setupSettingsButton(view: View) {
        try {
            val fabSettings = view.findViewById<FloatingActionButton>(R.id.fabSettings)
            fabSettings?.setOnClickListener {
                try {
                    findNavController().navigate(R.id.action_profilefragment_to_settingsFragment)
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error navigating to settings", e)
                    showError("Errore nell'aprire le impostazioni")
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error setting up settings button", e)
        }
    }

    private fun loadSavedProfileImage() {
        try {
            val currentUser = viewModel.currentUser.value
            if (currentUser != null) {
                loadUserProfileImage(currentUser.uid)
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error loading profile image", e)
        }
    }

    private fun loadUserProfileImage(userId: String) {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        if (document != null && document.exists()) {
                            val user = document.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)
                            user?.profileImageUrl?.let { url ->
                                if (url.isNotEmpty()) {
                                    loadImageFromBase64(url)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileFragment", "Error processing profile image document", e)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Error loading profile image from Firestore", e)
                }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error in loadUserProfileImage", e)
        }
    }

    private fun loadImageFromBase64(base64String: String) {
        try {
            if (base64String.isNotEmpty() && context != null) {
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap != null) {
                    profileImageView.setImageBitmap(bitmap)
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error decoding profile image", e)
        }
    }

    private fun showError(message: String) {
        try {
            if (context != null && isAdded) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error showing toast", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            Log.d("ProfileFragment", "onResume called")
            // Ricarica i dati quando il fragment torna in primo piano
            viewModel.refreshData()
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error in onResume", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        observersSetup = false
        profileImageLoaded = false
    }

    // Metodo pubblico per ricaricare il profilo
    fun reloadProfile() {
        try {
            viewModel.reloadUserProfile()
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error reloading profile", e)
            showError("Errore nel ricaricamento del profilo")
        }
    }
}