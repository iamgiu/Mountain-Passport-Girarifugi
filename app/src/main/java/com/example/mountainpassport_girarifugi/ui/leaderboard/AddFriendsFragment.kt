package com.example.mountainpassport_girarifugi.ui.leaderboard

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
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.databinding.FragmentAddFriendsBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AddFriendsFragment : Fragment() {

    private var _binding: FragmentAddFriendsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddFriendsViewModel by viewModels()

    private lateinit var usersAdapter: AddFriendsAdapter

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

        setupRecyclerView()
        setupSearchFunctionality()
        observeViewModel()

        viewModel.searchUsersFromFirebase("")

        val fabForwardLeaderboard =
            view.findViewById<FloatingActionButton>(R.id.fabForwardLeaderboard)
        fabForwardLeaderboard.setOnClickListener {
            findNavController().navigate(R.id.action_addfriendsFragment_to_leaderboardfragment)
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
            isNestedScrollingEnabled = true
            setHasFixedSize(false)
            itemAnimator = null
        }
    }

    private fun setupSearchFunctionality() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                performSearch(query)
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                performSearch(newText)
                return true
            }
        })
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { users ->
            usersAdapter.submitList(users) {
                if (users.isNotEmpty()) {
                    binding.recyclerViewUsers.scrollToPosition(0)
                }
            }

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

    private fun performSearch(query: String) {
        viewModel.searchUsersFromFirebase(query)
    }

    private fun onUserProfileClicked(user: AddFriendUser) {
        val bundle = Bundle().apply {
            putString("TYPE", "USER")
            putString("USER_ID", user.id)
            putString("USER_NAME", user.name)
            putString("USER_USERNAME", user.username)
            putInt("USER_POINTS", user.points)
            putInt("USER_REFUGES", user.refugesCount)
            putInt("USER_AVATAR", user.avatarResource)
            putString("USER_PROFILE_IMAGE_URL", user.profileImageUrl)
        }

        findNavController().navigate(
            R.id.action_addfriendsFragment_to_profileFragment,
            bundle
        )
    }

    private fun onAddFriendClicked(user: AddFriendUser) {
        viewModel.addFriend(user)
        Toast.makeText(
            requireContext(),
            "Richiesta di amicizia inviata a ${user.name}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showNoResultsMessage() {
        binding.textViewNoResults.visibility = View.VISIBLE
        binding.textViewNoResults.text = "Nessun utente trovato"
    }

    private fun hideNoResultsMessage() {
        binding.textViewNoResults.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
