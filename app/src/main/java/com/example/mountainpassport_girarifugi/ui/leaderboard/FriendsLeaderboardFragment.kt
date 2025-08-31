package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.Observer
import com.example.mountainpassport_girarifugi.databinding.FragmentFriendsLeaderboardBinding
import com.google.android.material.snackbar.Snackbar
import android.util.Log

class FriendsLeaderboardFragment : Fragment() {

    private var _binding: FragmentFriendsLeaderboardBinding? = null
    private val binding get() = _binding!!

    // Usa activityViewModels per condividere con il parent
    private val viewModel: LeaderboardViewModel by activityViewModels()

    private lateinit var adapter: FriendsLeaderboardAdapter

    companion object {
        private const val TAG = "FriendsLeaderboardFrag"
    }

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
        observeViewModel()

        // Forza un refresh quando il fragment diventa visibile
        viewModel.refreshFriendsLeaderboard()
    }

    private fun setupRecyclerView() {
        adapter = FriendsLeaderboardAdapter()
        binding.recyclerViewFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewFriends.adapter = adapter
    }


    private fun observeViewModel() {
        // Osserva i dati degli amici
        viewModel.friendsLeaderboard.observe(viewLifecycleOwner) { friends ->
            Log.d(TAG, "Ricevuti ${friends.size} amici dal ViewModel")
            friends.forEach { friend ->
                Log.d(TAG, "Amico: ${friend.name}, Punti: ${friend.points}, Pos: ${friend.position}")
            }

            adapter.submitList(friends)
            updateEmptyState(friends.isEmpty())
        }

        // Osserva gli errori
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e(TAG, "Errore ricevuto: $it")
                showErrorMessage(it)
                viewModel.clearError()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout?.visibility = View.VISIBLE
            binding.emptyTextLayout?.text = "Non hai ancora amici.\nAggiungi amici per vedere la classifica!"
            binding.recyclerViewFriends.visibility = View.GONE
        } else {
            binding.emptyStateLayout?.visibility = View.GONE
            binding.recyclerViewFriends.visibility = View.VISIBLE
        }
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Riprova") {
                viewModel.refreshFriendsLeaderboard()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh dei dati quando il fragment torna visibile
        Log.d(TAG, "Fragment resumed, refreshing data")
        viewModel.refreshFriendsLeaderboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}