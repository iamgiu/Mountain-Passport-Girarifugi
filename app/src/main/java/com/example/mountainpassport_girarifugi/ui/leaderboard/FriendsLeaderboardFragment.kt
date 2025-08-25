package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mountainpassport_girarifugi.databinding.FragmentFriendsLeaderboardBinding

class FriendsLeaderboardFragment : Fragment() {

    private var _binding: FragmentFriendsLeaderboardBinding? = null
    private val binding get() = _binding!!

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

        // Per ora mostro solo un messaggio di test
        // Quando avrai il layout fragment_friends_leaderboard.xml,
        // puoi configurare qui il RecyclerView

        // Esempio di dati di test (rimuovi quando implementi i dati reali)
        setupTestData()
    }

    private fun setupTestData() {
        // Se hai un RecyclerView nel layout, puoi configurarlo così:
        /*
        binding.recyclerViewFriends.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = FriendsAdapter(getTestFriends())
        }
        */
    }

    // Dati di test (rimuovi quando implementi i dati reali)
    private fun getTestFriends(): List<Friend> {
        return listOf(
            Friend("Marco Rossi", "MR", 2850, 45),
            Friend("Anna Bianchi", "AB", 2340, 38),
            Friend("Luca Verdi", "LV", 1890, 29)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Classe di esempio per i dati (sostituisci con la tua classe)
data class Friend(
    val name: String,
    val initials: String,
    val points: Int,
    val refuges: Int
)