package com.example.mountainpassport_girarifugi.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.SignInActivity
import com.google.firebase.auth.FirebaseAuth

// Data class per rappresentare un timbro
data class Stamp(
    val refugeName: String,
    val date: String,
    val altitude: String,
    val region: String
)

class ProfileFragment : Fragment() {

    private lateinit var stampsRecyclerView: RecyclerView
    private lateinit var stampsAdapter: StampsAdapter
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Inizializza le view
        initViews(view)

        // Configura la RecyclerView dei timbri
        setupStampsRecyclerView()

        // Carica i dati di esempio
        loadSampleData()

        // Setup logout button
        setupLogoutButton(view)
    }

    private fun initViews(view: View) {
        stampsRecyclerView = view.findViewById(R.id.stampsRecyclerView)

        // Puoi anche impostare i dati del profilo qui se vuoi
        val fullNameTextView = view.findViewById<TextView>(R.id.fullNameTextView)
        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)
        val monthlyScoreTextView = view.findViewById<TextView>(R.id.monthlyScoreTextView)
        val visitedRefugesTextView = view.findViewById<TextView>(R.id.visitedRefugesTextView)

        // Esempio di dati del profilo
        fullNameTextView.text = "Marco Rossi"
        usernameTextView.text = "marcorossi_explorer"
        monthlyScoreTextView.text = "1,245"
        visitedRefugesTextView.text = "23"
    }

    private fun setupStampsRecyclerView() {
        // Configura il layout manager per una griglia di 2 colonne
        stampsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Inizializza l'adapter
        stampsAdapter = StampsAdapter(emptyList())
        stampsRecyclerView.adapter = stampsAdapter
    }

    private fun loadSampleData() {
        // Crea alcuni timbri di esempio
        val sampleStamps = listOf(
            Stamp("Rifugio Città di Milano", "15/05/2024", "2581m", "Lombardia"),
            Stamp("Rifugio Branca", "22/05/2024", "2486m", "Lombardia"),
            Stamp("Rifugio Carate Brianza", "28/05/2024", "2636m", "Lombardia"),
            Stamp("Rifugio Pizzini", "05/06/2024", "2706m", "Lombardia"),
            Stamp("Rifugio Belviso", "12/06/2024", "2234m", "Lombardia"),
            Stamp("Rifugio Bonetta", "19/06/2024", "2458m", "Piemonte"),
            Stamp("Rifugio Schiena d'Asino", "26/06/2024", "2445m", "Piemonte"),
            Stamp("Rifugio Bertacchi", "03/07/2024", "2175m", "Lombardia")
        )

        // Aggiorna l'adapter con i dati
        stampsAdapter.updateStamps(sampleStamps)
    }

    private fun setupLogoutButton(view: View) {
        val logoutButton = view.findViewById<TextView>(R.id.logoutButton)
        logoutButton?.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Sei sicuro di voler uscire?")
            .setPositiveButton("Sì") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun performLogout() {
        firebaseAuth.signOut()

        // Naviga alla SignInActivity
        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}