package com.example.mountainpassport_girarifugi.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mountainpassport_girarifugi.databinding.FragmentLeaderboardBinding
import com.google.android.material.tabs.TabLayoutMediator

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
        setupTabButtons()
    }

    private fun setupViewPager() {
        val adapter = LeaderboardPagerAdapter(this)
        binding.viewPagerLeaderboard.adapter = adapter

        // Callback per aggiornare i tab quando si cambia pagina
        binding.viewPagerLeaderboard.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTabSelection(position)
            }
        })
    }

    private fun setupTabButtons() {
        binding.btnTabGeneral.setOnClickListener {
            binding.viewPagerLeaderboard.currentItem = 0
        }

        binding.btnTabMonthly.setOnClickListener {
            binding.viewPagerLeaderboard.currentItem = 1
        }

        binding.btnTabWeekly.setOnClickListener {
            binding.viewPagerLeaderboard.currentItem = 2
        }

        binding.btnTabFriends.setOnClickListener {
            binding.viewPagerLeaderboard.currentItem = 3
        }

        // Imposta il primo tab come selezionato
        updateTabSelection(0)
    }

    private fun updateTabSelection(position: Int) {
        // Reset di tutti i tab
        binding.btnTabGeneral.alpha = 0.6f
        binding.btnTabMonthly.alpha = 0.6f
        binding.btnTabWeekly.alpha = 0.6f
        binding.btnTabFriends.alpha = 0.6f

        // Evidenzia il tab selezionato
        when (position) {
            0 -> binding.btnTabGeneral.alpha = 1.0f
            1 -> binding.btnTabMonthly.alpha = 1.0f
            2 -> binding.btnTabWeekly.alpha = 1.0f
            3 -> binding.btnTabFriends.alpha = 1.0f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Adapter per il ViewPager
    private class LeaderboardPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FriendsLeaderboardFragment()
                1 -> GroupsLeaderboardFragment()
                2 -> RegionalLeaderboardFragment()
                3 -> GlobalLeaderboardFragment()
                else -> FriendsLeaderboardFragment()
            }
        }
    }
}