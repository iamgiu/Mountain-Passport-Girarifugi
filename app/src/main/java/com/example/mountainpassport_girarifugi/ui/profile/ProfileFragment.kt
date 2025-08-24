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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfileFragment : Fragment() {

    private lateinit var stampsRecyclerView: RecyclerView
    private lateinit var stampsAdapter: StampsAdapter

    // Views del profilo
    private lateinit var fullNameTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var monthlyScoreTextView: TextView
    private lateinit var visitedRefugesTextView: TextView

    // ViewModel
    private val viewModel: ProfileViewModel by viewModels()

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

        // Configura gli observer per il ViewModel
        setupObservers()

        // Setup settings FAB
        setupSettingsButton(view)
    }

    private fun initViews(view: View) {
        stampsRecyclerView = view.findViewById(R.id.stampsRecyclerView)
        fullNameTextView = view.findViewById(R.id.fullNameTextView)
        usernameTextView = view.findViewById(R.id.usernameTextView)
        monthlyScoreTextView = view.findViewById(R.id.monthlyScoreTextView)
        visitedRefugesTextView = view.findViewById(R.id.visitedRefugesTextView)
    }

    private fun setupStampsRecyclerView() {
        // Configura il layout manager per una griglia di 2 colonne
        stampsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Inizializza l'adapter
        stampsAdapter = StampsAdapter(emptyList())
        stampsRecyclerView.adapter = stampsAdapter
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

        // Observer per gli errori di caricamento
        viewModel.loadingError.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotBlank()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        // Observer per lo stato di caricamento
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Qui potresti mostrare/nascondere un indicatore di caricamento
            // Ad esempio un ProgressBar
            if (isLoading) {
                // Mostra loading indicator
                showLoadingState()
            } else {
                // Nascondi loading indicator
                hideLoadingState()
            }
        }
    }

    private fun updateProfileUI(profileData: ProfileData) {
        fullNameTextView.text = profileData.fullName
        usernameTextView.text = profileData.username
        monthlyScoreTextView.text = profileData.monthlyScore
        visitedRefugesTextView.text = profileData.visitedRefuges
    }

    private fun showLoadingState() {
        // Opzionale: Mostra uno stato di caricamento
        fullNameTextView.text = "Caricamento..."
        usernameTextView.text = "Caricamento..."
    }

    private fun hideLoadingState() {
        // Lo stato normale sarà ripristinato dagli observer dei dati
    }

    private fun setupSettingsButton(view: View) {
        val fabSettings = view.findViewById<FloatingActionButton>(R.id.fabSettings)
        fabSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profilefragment_to_settingsFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        // Ricarica i dati quando il fragment torna in primo piano
        viewModel.refreshData()
    }

    // Metodo pubblico per ricaricare il profilo (utile se chiamato da altre parti dell'app)
    fun reloadProfile() {
        viewModel.reloadUserProfile()
    }
}