package com.example.mountainpassport_girarifugi

import android.os.Bundle
import android.util.Log
import com.example.mountainpassport_girarifugi.data.repository.FirebaseInitializer
import com.example.mountainpassport_girarifugi.utils.NotificationHelper
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
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseInitializer: FirebaseInitializer

    private val TAG = "MainActivity"

    // Richiesta permessi notifiche per Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Permesso notifiche concesso")
        } else {
            Log.w(TAG, "Permesso notifiche negato")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inizializza Firebase
        firebaseInitializer = FirebaseInitializer(this)
        initializeFirebase()

        // Inizializza il sistema di notifiche
        initializeNotifications()

        // Richiedi permessi notifiche se necessario
        requestNotificationPermission()

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

    private fun initializeNotifications() {
        try {
            // Crea il canale di notifica
            NotificationHelper.createNotificationChannel(this)
            Log.d(TAG, "Sistema di notifiche inizializzato")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nell'inizializzazione delle notifiche: ${e.message}")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Permesso notifiche già concesso")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Mostra spiegazione all'utente
                    showNotificationPermissionDialog()
                }
                else -> {
                    // Richiedi il permesso
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permesso Notifiche")
            .setMessage("L'app ha bisogno del permesso per inviare notifiche per tenerti aggiornato sui tuoi progressi e le attività dei tuoi amici.")
            .setPositiveButton("Concedi") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
}