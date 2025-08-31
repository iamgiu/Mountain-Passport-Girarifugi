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
import androidx.lifecycle.lifecycleScope
import com.example.mountainpassport_girarifugi.data.repository.MonthlyChallengeRepository
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

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

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // Nascondi la navigation bar
        hideSystemUI()

        // Inizializza Firebase
        firebaseInitializer = FirebaseInitializer(this)
        initializeFirebase()

        // Inizializza il sistema di notifiche
        initializeNotifications()

        // Richiedi permessi notifiche se necessario
        requestNotificationPermission()

        val navView: BottomNavigationView = binding.bottomNavigationView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // FIXED: Gestisce il FAB scan con navigazione corretta
        binding.fabScan.setOnClickListener {
            try {
                // Verifica se siamo già nella home, altrimenti naviga prima lì
                when (navController.currentDestination?.id) {
                    R.id.nav_home -> {
                        // Siamo già in home, naviga direttamente al scan
                        navController.navigate(R.id.action_homeFragment_to_scanFragment)
                    }
                    else -> {
                        // Siamo in un'altra pagina, torna prima alla home
                        navController.navigate(R.id.nav_home)
                        // Poi naviga al scan (potrebbe richiedere un piccolo delay)
                        navController.navigate(R.id.action_homeFragment_to_scanFragment)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore navigazione scan: ${e.message}")
                // Fallback: naviga sempre alla home prima
                navController.navigate(R.id.nav_home)
            }
        }

        // Setup bottom navigation (esclude scan dal controllo automatico)
        navView.setupWithNavController(navController)

        // OPTIONAL: Gestisci manualmente il click sul centro del bottom nav se necessario
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_scan -> {
                    // Non fare nulla per il scan - è gestito dal FAB
                    false
                }
                else -> {
                    // Comportamento normale per altri elementi
                    navController.navigate(item.itemId)
                    true
                }
            }
        }

        // Avvia reset mensile
        lifecycleScope.launch {
            val repo = MonthlyChallengeRepository()
            repo.resetMonthlyPointsIfNeeded()
        }
    }

    private fun hideSystemUI() {
        // Abilita edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            // Nascondi la navigation bar
            hide(WindowInsetsCompat.Type.navigationBars())
            // Imposta il comportamento quando l'utente swipea per mostrare le barre
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Chiamato quando l'activity diventa visibile
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
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