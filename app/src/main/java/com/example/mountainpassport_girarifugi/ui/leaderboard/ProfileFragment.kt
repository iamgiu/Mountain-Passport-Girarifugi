package com.example.mountainpassport_girarifugi.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mountainpassport_girarifugi.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfileFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val type = arguments?.getString("TYPE") ?: "USER"

        return if (type == "USER") {
            inflater.inflate(R.layout.fragment_profile_leaderboard, container, false)
        } else {
            inflater.inflate(R.layout.fragment_profile_group_leaderboard, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = arguments?.getString("USER_NAME") ?: "Nome sconosciuto"
        val username = arguments?.getString("USER_USERNAME") ?: ""
        val points = arguments?.getInt("USER_POINTS") ?: 0
        val refuges = arguments?.getInt("USER_REFUGES") ?: 0
        val avatar = arguments?.getInt("USER_AVATAR") ?: R.drawable.avatar_sara

        // Popola le view
        view.findViewById<TextView>(R.id.fullNameTextView).text = name
        view.findViewById<TextView>(R.id.usernameTextView).text = username
        view.findViewById<TextView>(R.id.monthlyScoreTextView).text = "$points"
        view.findViewById<TextView>(R.id.visitedRefugesTextView).text = "$refuges"
        view.findViewById<ImageView>(R.id.profileImageView).setImageResource(avatar)

        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        // Back button - usa Navigation Component
        view.findViewById<FloatingActionButton>(R.id.fabBack).setOnClickListener {
            findNavController().navigateUp()
        }
    }

    companion object {
        fun newInstanceUser(
            name: String,
            username: String,
            points: Int,
            refuges: Int,
            avatar: Int
        ): ProfileFragment {
            val fragment = ProfileFragment()
            val args = Bundle().apply {
                putString("TYPE", "USER")
                putString("USER_NAME", name)
                putString("USER_USERNAME", username)
                putInt("USER_POINTS", points)
                putInt("USER_REFUGES", refuges)
                putInt("USER_AVATAR", avatar)
            }
            fragment.arguments = args
            return fragment
        }

        fun newInstanceGroup(
            name: String,
            username: String,
            points: Int,
            refuges: Int,
            avatar: Int
        ): ProfileFragment {
            val fragment = ProfileFragment()
            val args = Bundle().apply {
                putString("TYPE", "GROUP")
                putString("USER_NAME", name)
                putString("USER_USERNAME", username)
                putInt("USER_POINTS", points)
                putInt("USER_REFUGES", refuges)
                putInt("USER_AVATAR", avatar)
            }
            fragment.arguments = args
            return fragment
        }
    }
}