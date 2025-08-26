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
import android.net.Uri
import android.widget.ImageView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.File

class ProfileFragment : Fragment() {

    private lateinit var stampsRecyclerView: RecyclerView
    private lateinit var stampsAdapter: StampsAdapter

    // Aggiungi RecyclerView e Adapter per i gruppi
    private lateinit var groupsRecyclerView: RecyclerView
    private lateinit var groupsAdapter: GroupsAdapter

    // Views del profilo
    private lateinit var fullNameTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var monthlyScoreTextView: TextView
    private lateinit var visitedRefugesTextView: TextView

    // ViewModel
    private val viewModel: ProfileViewModel by viewModels()

    // Profile image view reference
    private lateinit var profileImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza le view
        initViews(view)

        // Configura la RecyclerView
        setupStampsRecyclerView()
        setupGroupsRecyclerView()

        // Configura gli observer per il ViewModel
        setupObservers()

        // Setup settings FAB
        setupSettingsButton(view)
    }

    private fun initViews(view: View) {
        stampsRecyclerView = view.findViewById(R.id.stampsRecyclerView)
        groupsRecyclerView = view.findViewById(R.id.groupsRecyclerView)
        fullNameTextView = view.findViewById(R.id.fullNameTextView)
        usernameTextView = view.findViewById(R.id.usernameTextView)
        monthlyScoreTextView = view.findViewById(R.id.monthlyScoreTextView)
        visitedRefugesTextView = view.findViewById(R.id.visitedRefugesTextView)
        profileImageView = view.findViewById(R.id.profileImageView)
    }

    private fun setupStampsRecyclerView() {
        // Configura il layout manager per lo scorrimento ORIZZONTALE
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        stampsRecyclerView.layoutManager = layoutManager

        // Inizializza l'adapter
        stampsAdapter = StampsAdapter(emptyList())
        stampsRecyclerView.adapter = stampsAdapter
    }

    private fun setupGroupsRecyclerView() {
        // Configura il layout manager orizzontale per i gruppi
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        groupsRecyclerView.layoutManager = layoutManager

        // Inizializza l'adapter con il listener per i click
        groupsAdapter = GroupsAdapter(emptyList()) { group ->
            // Navigazione al profilo del gruppo
            navigateToGroupProfile(group)
        }
        groupsRecyclerView.adapter = groupsAdapter
    }

    private fun setupObservers() {
        // Observer per i dati del profilo
        viewModel.profileData.observe(viewLifecycleOwner) { profileData ->
            updateProfileUI(profileData)
        }

        // Observer per i timbri
        viewModel.stamps.observe(viewLifecycleOwner) { stamps ->
            stampsAdapter.updateStamps(stamps)
        }

        // Observer per i gruppi
        viewModel.groups.observe(viewLifecycleOwner) { groups ->
            groupsAdapter.updateGroups(groups)
        }

        // Observer per gli errori di caricamento
        viewModel.loadingError.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotBlank()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        // Observer per lo stato di caricamento
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showLoadingState()
            } else {
                hideLoadingState()
            }
        }
        loadSavedProfileImage()
    }

    private fun updateProfileUI(profileData: ProfileData) {
        fullNameTextView.text = profileData.fullName
        usernameTextView.text = profileData.username
        monthlyScoreTextView.text = profileData.monthlyScore
        visitedRefugesTextView.text = profileData.visitedRefuges
    }

    private fun showLoadingState() {
        fullNameTextView.text = "Caricamento..."
        usernameTextView.text = "Caricamento..."
    }

    private fun hideLoadingState() {
        // Lo stato normale sarÃ  ripristinato dagli observer dei dati
    }

    private fun setupSettingsButton(view: View) {
        val fabSettings = view.findViewById<FloatingActionButton>(R.id.fabSettings)
        fabSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profilefragment_to_settingsFragment)
        }
    }

    private fun loadSavedProfileImage() {
        // Observe profile data to get the image
        viewModel.profileData.observe(viewLifecycleOwner) { profileData ->
            // Load from Firebase if available
            val currentUser = viewModel.currentUser.value
            if (currentUser != null) {
                loadUserProfileImage(currentUser.uid)
            }
        }
    }

    private fun loadUserProfileImage(userId: String) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(com.example.mountainpassport_girarifugi.user.User::class.java)
                    user?.let {
                        if (it.profileImageUrl.isNotEmpty()) {
                            loadImageFromBase64(it.profileImageUrl)
                        }
                    }
                }
            }
    }

    private fun loadImageFromBase64(base64String: String) {
        try {
            if (base64String.isNotEmpty()) {
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                profileImageView.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Metodo per navigare al profilo del gruppo
    private fun navigateToGroupProfile(group: Group) {
        // Crea il bundle con i dati del gruppo
        val bundle = Bundle().apply {
            putString("groupId", group.id)
            putString("groupName", group.name)
            putInt("memberCount", group.memberCount)
            putString("description", group.description)
        }

        try {
            // Naviga al fragment del profilo del gruppo
            // Assicurati che questa action esista nel tuo navigation graph
            findNavController().navigate(R.id.action_profileFragment_to_groupsFragment, bundle)
        } catch (e: Exception) {
            // Se la navigazione fallisce, mostra un messaggio di fallback
            Toast.makeText(requireContext(), "Apertura gruppo: ${group.name}", Toast.LENGTH_SHORT).show()

            // In alternativa, puoi logare l'errore per il debug
            android.util.Log.e("ProfileFragment", "Errore navigazione: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Ricarica i dati quando il fragment torna in primo piano
        viewModel.refreshData()
        // Reload profile image in case it was changed in settings
        loadSavedProfileImage()
    }

    // Metodo pubblico per ricaricare il profilo
    fun reloadProfile() {
        viewModel.reloadUserProfile()
    }
}