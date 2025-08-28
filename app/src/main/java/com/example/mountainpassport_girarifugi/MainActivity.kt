package com.example.mountainpassport_girarifugi

import android.os.Bundle
import android.util.Log
import com.example.mountainpassport_girarifugi.data.repository.FirebaseInitializer
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mountainpassport_girarifugi.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseInitializer: FirebaseInitializer

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inizializza Firebase
        firebaseInitializer = FirebaseInitializer(this)
        initializeFirebase()

        val navView: BottomNavigationView = binding.bottomNavigationView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_map,
                R.id.nav_leaderboard,
                R.id.nav_profile
            )
        )

        // Gestisce il FAB separatamente
        binding.fabScan.setOnClickListener {
            navController.navigate(R.id.nav_scan)
        }

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun initializeFirebase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verifica se Firebase è già inizializzato
                if (!firebaseInitializer.isFirebaseInitialized()) {
                    Log.d(TAG, "Firebase non inizializzato, procedo con l'inizializzazione...")
                    firebaseInitializer.initializeFirebase()
                } else {
                    Log.d(TAG, "Firebase già inizializzato")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore nell'inizializzazione Firebase: ${e.message}")
            }
        }
    }
}