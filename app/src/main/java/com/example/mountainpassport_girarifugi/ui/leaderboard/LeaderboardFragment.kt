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
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import androidx.navigation.fragment.findNavController
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.databinding.FragmentLeaderboardBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LeaderboardViewModel by viewModels()
    private var isSearchVisible = false

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
        setupSearchFunctionality()

        // Imposta "Amici" come tab selezionato di default
        selectTab(1)

        val fabAddFriend = view.findViewById<FloatingActionButton>(R.id.fabAddFriend)
        fabAddFriend.setOnClickListener {
            findNavController().navigate(R.id.action_leaderboardfragment_to_addfriendsFragment)
        }
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

    private fun setupSearchFunctionality() {
        // Click listener per il FAB di ricerca
        binding.fabSearch.setOnClickListener {
            toggleSearchBar()
        }

        // Listener per la SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // Ricerca in tempo reale mentre l'utente digita
                performSearch(newText)
                return true
            }
        })

        // Listener per quando la SearchView viene chiusa
        binding.searchView.setOnCloseListener {
            hideSearchBar()
            false
        }
    }

    private fun toggleSearchBar() {
        if (isSearchVisible) {
            hideSearchBar()
        } else {
            showSearchBar()
        }
    }

    private fun showSearchBar() {
        binding.searchContainer.visibility = View.VISIBLE
        binding.searchView.requestFocus()
        isSearchVisible = true

        // Animazione di entrata - slide da destra verso sinistra
        val slideIn = ObjectAnimator.ofFloat(binding.searchContainer, "translationX", 300f, 0f)
        val fadeIn = ObjectAnimator.ofFloat(binding.searchContainer, "alpha", 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(binding.searchContainer, "scaleX", 0.8f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.searchContainer, "scaleY", 0.8f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(slideIn, fadeIn, scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.start()

        // Mostra la tastiera
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchView, InputMethodManager.SHOW_IMPLICIT)

        // Cambia l'icona del FAB
        binding.fabSearch.setImageResource(R.drawable.ic_close_24px)
    }

    private fun hideSearchBar() {
        // Animazione di uscita - slide verso destra
        val slideOut = ObjectAnimator.ofFloat(binding.searchContainer, "translationX", 0f, 300f)
        val fadeOut = ObjectAnimator.ofFloat(binding.searchContainer, "alpha", 1f, 0f)
        val scaleX = ObjectAnimator.ofFloat(binding.searchContainer, "scaleX", 1f, 0.8f)
        val scaleY = ObjectAnimator.ofFloat(binding.searchContainer, "scaleY", 1f, 0.8f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(slideOut, fadeOut, scaleX, scaleY)
        animatorSet.duration = 250

        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                binding.searchContainer.visibility = View.GONE
                binding.searchView.setQuery("", false)
                // Reset delle trasformazioni
                binding.searchContainer.translationX = 0f
                binding.searchContainer.alpha = 1f
                binding.searchContainer.scaleX = 1f
                binding.searchContainer.scaleY = 1f
            }
        })
        animatorSet.start()

        isSearchVisible = false

        // Nascondi la tastiera
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)

        // Ripristina l'icona del FAB
        binding.fabSearch.setImageResource(R.drawable.ic_search_24)

        // Reset della ricerca
        clearSearch()
    }

    private fun performSearch(query: String) {
        // Comunica la query di ricerca al ViewModel
        when (binding.viewPagerLeaderboard.currentItem) {
            0 -> viewModel.searchInGroups(query)
            1 -> viewModel.searchInFriends(query)
            2 -> viewModel.searchInGlobal(query)
        }
    }

    private fun clearSearch() {
        // Resetta i risultati di ricerca
        viewModel.clearSearch()
    }

    private fun selectTab(position: Int) {
        // Reset all tabs
        resetAllTabs()

        when (position) {
            0 -> {
                binding.btnTabGroups.setBackgroundResource(R.drawable.rounded_button_selected)
                binding.btnTabGroups.setTextColor(ContextCompat.getColor(requireContext(), R.color.light_green))
                animateTabSize(binding.btnTabGroups, 1.15f)
            }
            1 -> {
                binding.btnTabFriends.setBackgroundResource(R.drawable.rounded_button_selected)
                binding.btnTabFriends.setTextColor(ContextCompat.getColor(requireContext(), R.color.light_green))
                animateTabSize(binding.btnTabFriends, 1.15f)
            }
            2 -> {
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

    // Gestisce il tasto indietro quando la ricerca è aperta
    fun onBackPressed(): Boolean {
        return if (isSearchVisible) {
            hideSearchBar()
            true
        } else {
            false
        }
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