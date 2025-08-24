package com.example.mountainpassport_girarifugi.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.SignInActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfileFragment : Fragment() {

    private lateinit var stampsRecyclerView: RecyclerView
    private lateinit var stampsAdapter: StampsAdapter

    // Views del profilo
    private lateinit var fullNameTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var monthlyScoreTextView: TextView
    private lateinit var visitedRefugesTextView: TextView
    private lateinit var logoutButton: TextView

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

        // Setup logout button
        setupLogoutButton()

        val fabSettings = view.findViewById<FloatingActionButton>(R.id.fabSettings)
        fabSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profilefragment_to_settingsFragment)
        }
    }

    private fun initViews(view: View) {
        stampsRecyclerView = view.findViewById(R.id.stampsRecyclerView)
        fullNameTextView = view.findViewById(R.id.fullNameTextView)
        usernameTextView = view.findViewById(R.id.usernameTextView)
        monthlyScoreTextView = view.findViewById(R.id.monthlyScoreTextView)
        visitedRefugesTextView = view.findViewById(R.id.visitedRefugesTextView)
        logoutButton = view.findViewById(R.id.logoutButton)
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

        // Observer per l'evento di logout
        viewModel.logoutEvent.observe(viewLifecycleOwner) { shouldLogout ->
            if (shouldLogout) {
                navigateToSignIn()
                viewModel.onLogoutEventHandled()
            }
        }

        // Observer per l'utente corrente (opzionale)
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            // Puoi usare questo per aggiornare l'UI in base allo stato dell'utente
            if (user == null) {
                // L'utente non è loggato
                navigateToSignIn()
            }
        }
    }

    private fun updateProfileUI(profileData: ProfileData) {
        fullNameTextView.text = profileData.fullName
        usernameTextView.text = profileData.username
        monthlyScoreTextView.text = profileData.monthlyScore
        visitedRefugesTextView.text = profileData.visitedRefuges
    }

    private fun setupLogoutButton() {
        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Sei sicuro di voler uscire?")
            .setPositiveButton("Sì") { _, _ ->
                viewModel.performLogout()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun navigateToSignIn() {
        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}