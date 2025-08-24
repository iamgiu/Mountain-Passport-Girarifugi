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
import com.example.mountainpassport_girarifugi.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
    private lateinit var firestore: FirebaseFirestore

    // Views
    private lateinit var fullNameTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var monthlyScoreTextView: TextView
    private lateinit var visitedRefugesTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza Firebase Auth e Firestore
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Inizializza le view
        initViews(view)

        // Configura la RecyclerView dei timbri
        setupStampsRecyclerView()

        // Carica i dati utente da Firebase
        loadUserData()

        // Carica i dati di esempio per i timbri
        loadSampleData()

        // Setup logout button
        setupLogoutButton(view)
    }

    private fun initViews(view: View) {
        stampsRecyclerView = view.findViewById(R.id.stampsRecyclerView)
        fullNameTextView = view.findViewById(R.id.fullNameTextView)
        usernameTextView = view.findViewById(R.id.usernameTextView)
        monthlyScoreTextView = view.findViewById(R.id.monthlyScoreTextView)
        visitedRefugesTextView = view.findViewById(R.id.visitedRefugesTextView)
    }

    private fun loadUserData() {
        val currentUser = firebaseAuth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        updateUIWithUserData(it)
                    }
                } else {
                    // Se il documento non esiste, mostra dati di default
                    setDefaultData()
                }
            }
            .addOnFailureListener {
                // In caso di errore, mostra dati di default
                setDefaultData()
            }
    }

    private fun updateUIWithUserData(user: User) {
        // Nome completo
        val fullName = "${user.nome} ${user.cognome}".trim()
        if (fullName.isNotBlank()) {
            fullNameTextView.text = fullName
        } else {
            fullNameTextView.text = "Nome non impostato"
        }

        // Nickname/Username
        if (user.nickname.isNotBlank()) {
            usernameTextView.text = user.nickname
        } else {
            usernameTextView.text = "username_non_impostato"
        }

        // Statistiche (static for now)
        monthlyScoreTextView.text = "1,245"
        visitedRefugesTextView.text = "23"
    }

    private fun setDefaultData() {
        fullNameTextView.text = "Nome non disponibile"
        usernameTextView.text = "username"
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

    override fun onResume() {
        super.onResume()
        // Ricarica i dati utente quando il fragment torna in primo piano
        loadUserData()
    }
}