package com.example.mountainpassport_girarifugi.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.google.android.material.appbar.MaterialToolbar

class FriendRequestsFragment : Fragment() {

    private val viewModel: FriendRequestsViewModel by viewModels()
    private lateinit var adapter: FriendRequestsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friend_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(view)
        setupRecyclerView(view)
        observeViewModel()
    }

    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewFriendRequests)

        adapter = FriendRequestsAdapter(
            onAcceptClick = { request ->
                viewModel.acceptFriendRequest(request.id)
            },
            onDeclineClick = { request ->
                viewModel.declineFriendRequest(request.id)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FriendRequestsFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.friendRequests.observe(viewLifecycleOwner) { requests ->
            adapter.submitList(requests)

            // Show/hide empty state
            val emptyView = view?.findViewById<View>(R.id.emptyStateView)
            emptyView?.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val progressBar = view?.findViewById<View>(R.id.progressBar)
            progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.actionResult.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearActionResult()
            }
        }
    }
}