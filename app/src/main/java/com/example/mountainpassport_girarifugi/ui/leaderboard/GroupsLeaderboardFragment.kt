package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.Observer
import com.example.mountainpassport_girarifugi.databinding.FragmentGroupsLeaderboardBinding
import com.google.android.material.snackbar.Snackbar

class GroupsLeaderboardFragment : Fragment() {

    private var _binding: FragmentGroupsLeaderboardBinding? = null
    private val binding get() = _binding!!

    // Condividi il ViewModel con il Fragment padre
    private val viewModel: LeaderboardViewModel by activityViewModels()

    private lateinit var adapter: GroupsLeaderboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupsLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = GroupsLeaderboardAdapter()
        binding.recyclerViewGroups.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewGroups.adapter = adapter
    }

    private fun observeViewModel() {
        // Osserva i dati degli amici
        viewModel.groupsLeaderboard.observe(viewLifecycleOwner, Observer { groups ->
            adapter.submitList(groups)
        })
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Riprova") {
                viewModel.loadGroupsLeaderboard()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}