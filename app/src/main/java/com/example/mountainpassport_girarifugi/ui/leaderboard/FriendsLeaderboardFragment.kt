package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mountainpassport_girarifugi.databinding.FragmentFriendsLeaderboardBinding

class FriendsLeaderboardFragment : Fragment() {

    private var _binding: FragmentFriendsLeaderboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var friendsAdapter: FriendsAdapter
    private val friendsList = mutableListOf<Friend>()
    private val filteredFriendsList = mutableListOf<Friend>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        setupFab()
        loadFriendsData()
    }

    private fun setupRecyclerView() {
        friendsAdapter = FriendsAdapter(filteredFriendsList)
        binding.recyclerViewFriends.apply {
            adapter = friendsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearchView() {
        binding.searchViewFriends.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterFriends(newText ?: "")
                return true
            }
        })
    }

    private fun setupFab() {
        binding.fabAddFriend.setOnClickListener {
            // Qui implementerai la logica per aggiungere un nuovo amico
            Toast.makeText(requireContext(), "Aggiungi nuovo amico", Toast.LENGTH_SHORT).show()
            // Per ora aggiungiamo un amico di esempio
            addRandomFriend()
        }
    }

    private fun loadFriendsData() {
        // Dati di esempio
        val sampleFriends = listOf(
            Friend("Marco Rossi", 2850, 45, "https://example.com/avatar1.jpg"),
            Friend("Giulia Verdi", 3120, 52, "https://example.com/avatar2.jpg"),
            Friend("Luca Bianchi", 1980, 35, "https://example.com/avatar3.jpg"),
            Friend("Anna Neri", 4250, 67, "https://example.com/avatar4.jpg"),
            Friend("Francesco Bruno", 2150, 38, "https://example.com/avatar5.jpg"),
            Friend("Elena Gialli", 3890, 58, "https://example.com/avatar6.jpg"),
            Friend("Alessandro Viola", 1750, 28, "https://example.com/avatar7.jpg"),
            Friend("Chiara Rosa", 5200, 78, "https://example.com/avatar8.jpg"),
            Friend("Davide Azzurri", 2680, 42, "https://example.com/avatar9.jpg"),
            Friend("Federica Grigi", 3450, 55, "https://example.com/avatar10.jpg")
        )

        friendsList.clear()
        friendsList.addAll(sampleFriends.sortedByDescending { it.points })
        filteredFriendsList.clear()
        filteredFriendsList.addAll(friendsList)
        friendsAdapter.notifyDataSetChanged()
    }

    private fun filterFriends(query: String) {
        filteredFriendsList.clear()
        if (query.isEmpty()) {
            filteredFriendsList.addAll(friendsList)
        } else {
            filteredFriendsList.addAll(
                friendsList.filter {
                    it.name.lowercase().contains(query.lowercase())
                }
            )
        }
        friendsAdapter.notifyDataSetChanged()
    }

    private fun addRandomFriend() {
        val randomNames = listOf(
            "Mario Fantini", "Laura Celeste", "Simone Marrone", "Valentina Oro",
            "Matteo Argento", "Sara Perla", "Andrea Corallo", "Francesca Turchese"
        )
        val randomName = randomNames.random()
        val randomPoints = (1000..5000).random()
        val randomRefuges = (20..80).random()

        val newFriend = Friend(randomName, randomPoints, randomRefuges, "")
        friendsList.add(newFriend)
        friendsList.sortByDescending { it.points }

        // Aggiorna anche la lista filtrata se non c'è ricerca attiva
        if (binding.searchViewFriends.query.isEmpty()) {
            filteredFriendsList.clear()
            filteredFriendsList.addAll(friendsList)
            friendsAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Data class per rappresentare un amico
data class Friend(
    val name: String,
    val points: Int,
    val refugesVisited: Int,
    val avatarUrl: String
)