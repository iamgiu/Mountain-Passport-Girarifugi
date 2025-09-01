package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mountainpassport_girarifugi.databinding.FragmentGlobalLeaderboardBinding
import com.google.android.material.snackbar.Snackbar
import android.util.Log

class GlobalLeaderboardFragment : Fragment() {

    private var _binding: FragmentGlobalLeaderboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeaderboardViewModel by activityViewModels()

    private lateinit var adapter: GlobalLeaderboardAdapter

    companion object {
        private const val TAG = "GlobalLeaderboardFrag"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGlobalLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        viewModel.refreshGlobalLeaderboard()
    }

    private fun setupRecyclerView() {
        adapter = GlobalLeaderboardAdapter()
        binding.recyclerViewGlobal.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewGlobal.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.globalLeaderboard.observe(viewLifecycleOwner) { global ->
            Log.d(TAG, "Ricevuti ${global.size} utenti dal ViewModel")
            global.forEach { user ->
                Log.d(TAG, "Utente: ${user.name}, Punti: ${user.points}, Pos: ${user.position}")
            }

            adapter.submitList(global)
            updateEmptyState(global.isEmpty())
        }
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
            binding.emptyTextLayout?.text = "Nessun utente trovato nella classifica globale."
            binding.recyclerViewGlobal.visibility = View.GONE
        } else {
            binding.emptyStateLayout?.visibility = View.GONE
            binding.recyclerViewGlobal.visibility = View.VISIBLE
        }
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Riprova") {
                viewModel.refreshGlobalLeaderboard()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Fragment resumed, refreshing global leaderboard")
        viewModel.refreshGlobalLeaderboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
