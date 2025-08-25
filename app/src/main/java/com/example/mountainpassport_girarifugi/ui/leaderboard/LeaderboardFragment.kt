package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import android.animation.ObjectAnimator
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.databinding.FragmentLeaderboardBinding

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

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

        // Imposta "Amici" come tab selezionato di default
        selectTab(0)
    }

    private fun setupViewPager() {
        val adapter = LeaderboardPagerAdapter(this)
        binding.viewPagerLeaderboard.adapter = adapter

        // Sincronizza ViewPager con i tab
        binding.viewPagerLeaderboard.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                selectTab(position)
            }
        })
    }

    private fun setupTabs() {
        binding.btnTabGroups.setOnClickListener {
            selectTab(0)
            binding.viewPagerLeaderboard.currentItem = 0
        }
        binding.btnTabFriends.setOnClickListener {
            selectTab(1)
            binding.viewPagerLeaderboard.currentItem = 1
        }
        binding.btnTabGlobal.setOnClickListener {
            selectTab(2)
            binding.viewPagerLeaderboard.currentItem = 2
        }
    }

    private fun selectTab(position: Int) {
        // Reset all tabs
        resetAllTabs()

        when (position) {
            0 -> {
                binding.btnTabGroups.setBackgroundResource(R.drawable.rounded_button_selected)
                binding.btnTabGroups.setTextColor(ContextCompat.getColor(requireContext(), R.color.light_green))
                // Aggiungi l'animazione di ingrandimento
                animateTabSize(binding.btnTabGroups, 1.15f)
            }
            1 -> {
                binding.btnTabFriends.setBackgroundResource(R.drawable.rounded_button_selected)
                binding.btnTabFriends.setTextColor(ContextCompat.getColor(requireContext(), R.color.light_green))
                // Aggiungi l'animazione di ingrandimento
                animateTabSize(binding.btnTabFriends, 1.15f)
            }
            2 -> {
                binding.btnTabGlobal.setBackgroundResource(R.drawable.rounded_button_selected)
                binding.btnTabGlobal.setTextColor(ContextCompat.getColor(requireContext(), R.color.light_green))
                // Aggiungi l'animazione di ingrandimento
                animateTabSize(binding.btnTabGlobal, 1.15f)
            }
        }
    }

    private fun animateTabSize(view: View, scale: Float) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", scale)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", scale)
        scaleX.duration = 200 // Durata dell'animazione in millisecondi
        scaleY.duration = 200
        scaleX.start()
        scaleY.start()
    }

    private fun resetAllTabs() {
        binding.btnTabGroups.setBackgroundResource(R.drawable.rounded_button_unselected)
        binding.btnTabGroups.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_black))
        binding.btnTabGroups.scaleX = 1.0f
        binding.btnTabGroups.scaleY = 1.0f

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

    // Adapter per il ViewPager2
    private inner class LeaderboardPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GroupsLeaderboardFragment()
                1 -> FriendsLeaderboardFragment()
                2 -> GlobalLeaderboardFragment()
                else -> FriendsLeaderboardFragment()
            }
        }
    }
}