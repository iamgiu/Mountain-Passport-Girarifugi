package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import androidx.navigation.fragment.findNavController
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.databinding.FragmentLeaderboardBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.util.Log

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeaderboardViewModel by activityViewModels()

    private var isSearchVisible = false

    companion object {
        private const val TAG = "LeaderboardFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupTabs()

        selectTab(0)

        val fabAddFriend = view.findViewById<FloatingActionButton>(R.id.fabAddFriend)
        fabAddFriend.setOnClickListener {
            findNavController().navigate(R.id.action_leaderboardfragment_to_addfriendsFragment)
        }

        Log.d(TAG, "Fragment creato, caricando dati...")
        viewModel.refreshFriendsLeaderboard()
        viewModel.refreshGlobalLeaderboard()
    }

    private fun setupViewPager() {
        val adapter = LeaderboardPagerAdapter(this)
        binding.viewPagerLeaderboard.adapter = adapter

        binding.viewPagerLeaderboard.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                selectTab(position)
                Log.d(TAG, "Tab selezionato: $position")
            }
        })
    }

    /**
     * Setup delle tab "Amici" e "Globale"
     */
    private fun setupTabs() {

        binding.btnTabFriends.setOnClickListener {
            selectTab(0)
            binding.viewPagerLeaderboard.currentItem = 0
        }
        binding.btnTabGlobal.setOnClickListener {
            selectTab(1)
            binding.viewPagerLeaderboard.currentItem = 1
        }
    }

    /**
     * Seleziona la tab corrispondente
     */
    private fun selectTab(position: Int) {
        resetAllTabs()

        when (position) {
            0 -> {
                binding.btnTabFriends.setBackgroundResource(R.drawable.rounded_button_selected)
                binding.btnTabFriends.setTextColor(ContextCompat.getColor(requireContext(), R.color.light_green))
                animateTabSize(binding.btnTabFriends, 1.15f)
            }
            1 -> {
                binding.btnTabGlobal.setBackgroundResource(R.drawable.rounded_button_selected)
                binding.btnTabGlobal.setTextColor(ContextCompat.getColor(requireContext(), R.color.light_green))
                animateTabSize(binding.btnTabGlobal, 1.15f)
            }
        }
    }

    private fun animateTabSize(view: View, scale: Float) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", scale)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", scale)
        scaleX.duration = 200
        scaleY.duration = 200
        scaleX.start()
        scaleY.start()
    }

    private fun resetAllTabs() {
        binding.btnTabFriends.setBackgroundResource(R.drawable.rounded_button_unselected)
        binding.btnTabFriends.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_black))
        binding.btnTabFriends.scaleX = 1.0f
        binding.btnTabFriends.scaleY = 1.0f

        binding.btnTabGlobal.setBackgroundResource(R.drawable.rounded_button_unselected)
        binding.btnTabGlobal.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_black))
        binding.btnTabGlobal.scaleX = 1.0f
        binding.btnTabGlobal.scaleY = 1.0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class LeaderboardPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FriendsLeaderboardFragment()
                1 -> GlobalLeaderboardFragment()
                else -> FriendsLeaderboardFragment()
            }
        }
    }
}