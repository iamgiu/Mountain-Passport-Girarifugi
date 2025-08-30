package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.databinding.FragmentAddFriendsBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AddFriendsFragment : Fragment() {

    private var _binding: FragmentAddFriendsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddFriendsViewModel by viewModels()

    private lateinit var usersAdapter: AddFriendsAdapter
    private var currentTab = 0 // 0 = Utenti, 1 = Gruppi

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupRecyclerView()
        setupSearchFunctionality()
        observeViewModel()

        // Imposta "Utenti" come tab selezionato di default
        selectTab(0)

        val fabForwardLeaderboard = view.findViewById<FloatingActionButton>(R.id.fabForwardLeaderboard)
        fabForwardLeaderboard.setOnClickListener {
            findNavController().navigate(R.id.action_addfriendsFragment_to_leaderboardfragment)
        }
    }

    private fun setupTabs() {
        binding.btnTabPersone.setOnClickListener {
            selectTab(0)
        }

        binding.btnTabGroups.setOnClickListener {
            selectTab(1)
        }
    }

    private fun setupRecyclerView() {
        usersAdapter = AddFriendsAdapter(
            onUserClick = { user -> onUserProfileClicked(user) },
            onAddFriendClick = { user -> onAddFriendClicked(user) }
        )

        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = usersAdapter

            // RIMUOVI IL PADDING ECCESSIVO E MIGLIORA LO SCROLLING
            // setPadding(0, 0, 0, 100) // RIMUOVI QUESTA RIGA
            // clipToPadding = false // RIMUOVI QUESTA RIGA

            // AGGIUNGI QUESTE CONFIGURAZIONI PER MIGLIORARE LO SCROLLING
            isNestedScrollingEnabled = true
            setHasFixedSize(false) // Cambia a false per contenuto dinamico

            // PADDING E CLIPPING GIÀ GESTITI NEL LAYOUT XML

            // MIGLIORA LA PERFORMANCE DELLO SCROLLING
            itemAnimator = null // Disabilita animazioni se causano problemi
        }
    }

    private fun setupSearchFunctionality() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                performSearch(query)
                // AGGIUNGI QUESTO PER NASCONDERE LA TASTIERA
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // Ricerca in tempo reale mentre l'utente digita
                performSearch(newText)
                return true
            }
        })

        // RIMUOVI IL FOCUS AUTOMATICO CHE PUÒ CAUSARE PROBLEMI
        // binding.searchView.requestFocus() // COMMENTA O RIMUOVI QUESTA RIGA
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { users ->
            usersAdapter.submitList(users) {
                // CALLBACK ESEGUITO DOPO L'AGGIORNAMENTO DELLA LISTA
                // SCROLL AL TOP DOPO L'AGGIORNAMENTO (OPZIONALE)
                if (users.isNotEmpty()) {
                    binding.recyclerViewUsers.scrollToPosition(0)
                }
            }

            // Mostra/nasconde il messaggio di "nessun risultato"
            if (users.isEmpty() && binding.searchView.query.isNotEmpty()) {
                showNoResultsMessage()
            } else {
                hideNoResultsMessage()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Ricerca tramite Firebase
    private fun performSearch(query: String) {
        when (currentTab) {
            0 -> viewModel.searchUsersFromFirebase(query)
            1 -> viewModel.searchGroups(query)
        }
    }

    private fun selectTab(position: Int) {
        currentTab = position

        when (position) {
            0 -> {
                // Tab Utenti selezionato
                binding.btnTabPersone.setBackgroundResource(R.drawable.rounded_button_selected)
                binding.btnTabPersone.setTextColor(resources.getColor(R.color.light_green, null))

                binding.btnTabGroups.setBackgroundResource(R.drawable.rounded_button_unselected)
                binding.btnTabGroups.setTextColor(resources.getColor(R.color.green_black, null))

                binding.searchView.queryHint = "Cerca utenti..."
            }
            1 -> {
                // Tab Gruppi selezionato
                binding.btnTabGroups.setBackgroundResource(R.drawable.rounded_button_selected)
                binding.btnTabGroups.setTextColor(resources.getColor(R.color.light_green, null))

                binding.btnTabPersone.setBackgroundResource(R.drawable.rounded_button_unselected)
                binding.btnTabPersone.setTextColor(resources.getColor(R.color.green_black, null))

                binding.searchView.queryHint = "Cerca gruppi..."
            }
        }

        // Ricerca tramite Firebase
        val currentQuery = binding.searchView.query.toString()
        if (currentQuery.isNotEmpty()) {
            performSearch(currentQuery)
        } else {
            when (currentTab) {
                0 -> viewModel.searchUsersFromFirebase("") // Load all users
                1 -> viewModel.loadDefaultGroups()
            }
        }
    }

    private fun onUserProfileClicked(user: AddFriendUser) {
        val bundle = Bundle().apply {
            putString("TYPE", if (currentTab == 0) "USER" else "GROUP")
            putString("USER_NAME", user.name)
            putString("USER_USERNAME", user.username)
            putInt("USER_POINTS", user.points)
            putInt("USER_REFUGES", user.refugesCount)
            putInt("USER_AVATAR", user.avatarResource)
            putString("USER_PROFILE_IMAGE_URL", user.profileImageUrl)
        }

        findNavController().navigate(R.id.action_addfriendsFragment_to_profileFragment, bundle)
    }

    private fun onAddFriendClicked(user: AddFriendUser) {
        // Gestisce la richiesta di amicizia o di unione al gruppo
        when (currentTab) {
            0 -> {
                // Logica per aggiungere un utente come amico
                viewModel.addFriend(user)
                Toast.makeText(
                    requireContext(),
                    "Richiesta di amicizia inviata a ${user.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            1 -> {
                // Logica per unirsi a un gruppo
                viewModel.joinGroup(user)
                Toast.makeText(
                    requireContext(),
                    "Richiesta di accesso inviata al gruppo ${user.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showNoResultsMessage() {
        binding.textViewNoResults.visibility = View.VISIBLE
        binding.textViewNoResults.text = when (currentTab) {
            0 -> "Nessun utente trovato"
            1 -> "Nessun gruppo trovato"
            else -> "Nessun risultato trovato"
        }
    }

    private fun hideNoResultsMessage() {
        binding.textViewNoResults.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}