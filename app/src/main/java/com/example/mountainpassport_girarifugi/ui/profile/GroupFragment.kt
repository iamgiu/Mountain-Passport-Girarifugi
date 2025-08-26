package com.example.mountainpassport_girarifugi.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GroupsFragment : Fragment() {

    // RecyclerView per i membri del gruppo
    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var membersAdapter: MembersAdapter

    // Views del profilo gruppo
    private lateinit var groupNameTextView: TextView
    private lateinit var groupUsernameTextView: TextView
    private lateinit var monthlyScoreTextView: TextView
    private lateinit var visitedRefugesTextView: TextView

    // ViewModel
    private val viewModel: GroupsViewModel by viewModels()

    // Parametri ricevuti dalla navigazione
    private var groupId: String? = null
    private var groupName: String? = null
    private var memberCount: Int = 0
    private var description: String? = null
    private var isAdmin: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recupera gli argomenti dalla navigazione
        retrieveArguments()

        // Inizializza le view
        initViews(view)

        // Configura la RecyclerView per i membri
        setupMembersRecyclerView()

        // Configura gli observer per il ViewModel
        setupObservers()

        // Setup forward FAB per il profilo
        setupGroupForwardButton(view)

        // Setup settings FAB per il gruppo
        setupGroupSettingsButton(view)

        // Carica i dati del gruppo
        loadGroupData()
    }

    private fun retrieveArguments() {
        arguments?.let {
            groupId = it.getString("groupId")
            groupName = it.getString("groupName")
            memberCount = it.getInt("memberCount", 0)
            description = it.getString("description")
            isAdmin = it.getBoolean("isAdmin", false)
        }
    }

    private fun initViews(view: View) {
        membersRecyclerView = view.findViewById(R.id.groupsRecyclerView) // Riutilizza la RecyclerView per i membri
        groupNameTextView = view.findViewById(R.id.fullNameTextView)
        groupUsernameTextView = view.findViewById(R.id.usernameTextView)
        monthlyScoreTextView = view.findViewById(R.id.monthlyScoreTextView)
        visitedRefugesTextView = view.findViewById(R.id.visitedRefugesTextView)
    }

    private fun setupMembersRecyclerView() {
        // Configura il layout manager VERTICALE per i membri
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        membersRecyclerView.layoutManager = layoutManager

        // Inizializza l'adapter per i membri
        membersAdapter = MembersAdapter(emptyList()) { member ->
            // Gestisci il click su un membro (opzionale)
            onMemberClick(member)
        }
        membersRecyclerView.adapter = membersAdapter
    }

    private fun setupObservers() {
        // Observer per i dati del gruppo
        viewModel.groupData.observe(viewLifecycleOwner) { groupData ->
            updateGroupUI(groupData)
        }

        // Observer per i membri del gruppo
        viewModel.members.observe(viewLifecycleOwner) { members ->
            membersAdapter.updateMembers(members)
        }

        // Observer per gli errori di caricamento
        viewModel.loadingError.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotBlank()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        // Observer per lo stato di caricamento
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showLoadingState()
            } else {
                hideLoadingState()
            }
        }
    }

    private fun updateGroupUI(groupData: GroupData) {
        groupNameTextView.text = groupData.name
        groupUsernameTextView.text = groupData.username
        monthlyScoreTextView.text = groupData.monthlyScore
        visitedRefugesTextView.text = groupData.visitedRefuges
    }

    private fun showLoadingState() {
        groupNameTextView.text = "Caricamento..."
        groupUsernameTextView.text = "Caricamento..."
    }

    private fun hideLoadingState() {
        // Lo stato normale sarà ripristinato dagli observer dei dati
    }

    private fun setupGroupForwardButton(view: View) {
        val fabForward = view.findViewById<FloatingActionButton>(R.id.fabForward)
        fabForward.setOnClickListener {
            findNavController().navigate(R.id.action_groupsFragment_to_profileFragment)
        }
    }

    private fun setupGroupSettingsButton(view: View) {
        val fabSettingsGroup = view.findViewById<FloatingActionButton>(R.id.fabSettingsGroup)
        fabSettingsGroup.setOnClickListener {
            findNavController().navigate(R.id.action_groupsFragment_to_settingsGroupFragment)
        }
    }

    private fun loadGroupData() {
        groupId?.let { id ->
            // Carica i dati del gruppo tramite ViewModel
            viewModel.loadGroupData(id)

            // Per ora, usa i dati passati come argomenti
            val groupData = GroupData(
                name = groupName ?: "Gruppo",
                username = "@${groupName?.toLowerCase()?.replace(" ", "_") ?: "gruppo"}",
                monthlyScore = "1,245", // Esempio
                visitedRefuges = "23" // Esempio
            )
            viewModel.setGroupData(groupData)
        }
    }

    private fun onMemberClick(member: Member) {
        // Gestisci il click su un membro (ad esempio, naviga al suo profilo)
        Toast.makeText(requireContext(), "Profilo: ${member.fullName}", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToGroupSettings() {
        val bundle = Bundle().apply {
            putString("groupId", groupId)
            putString("groupName", groupName)
        }

        try {
            // Naviga alle impostazioni del gruppo (quando avrai creato il fragment)
            findNavController().navigate(R.id.action_groupsFragment_to_settingsGroupFragment, bundle)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Impostazioni gruppo: $groupName", Toast.LENGTH_SHORT).show()
        }
    }
}

// Data class per i dati del profilo del gruppo
data class GroupData(
    val name: String,
    val username: String,
    val monthlyScore: String,
    val visitedRefuges: String
)

// Data class per rappresentare un membro del gruppo
data class Member(
    val id: String,
    val fullName: String,
    val profileImageUrl: String? = null,
)