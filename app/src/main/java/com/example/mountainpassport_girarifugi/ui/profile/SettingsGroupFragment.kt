package com.example.mountainpassport_girarifugi.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.databinding.FragmentSettingsGroupBinding
import com.example.mountainpassport_girarifugi.ui.profile.settings.SettingsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
class SettingsGroupFragment : Fragment() {

    private var _binding: FragmentSettingsGroupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup settings FAB
        setupForwardButton(view)
    }

    private fun setupForwardButton(view: View) {
        val fabForward = view.findViewById<FloatingActionButton>(R.id.fabForward)
        fabForward.setOnClickListener {
            findNavController().navigate(R.id.action_settingsGroupFragment_to_groupsFragment)
        }
    }
}