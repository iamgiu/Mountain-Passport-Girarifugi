package com.example.mountainpassport_girarifugi.ui.scan

import PuntiBottomSheet
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class ScanFragment : Fragment() {

    private lateinit var viewModel: ScanViewModel
    private var barcodeView: DecoratedBarcodeView? = null
    private var isScanning = false
    
    private val TAG = "ScanFragment"
    
    // Callback per il risultato della scansione
    private val barcodeCallback = BarcodeCallback { result: BarcodeResult ->
        if (!isScanning) {
            isScanning = true
            processQRResult(result.text)
        }
    }
    
    // Contract per la richiesta di permessi
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startScanner()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    // Contract per la scansione QR
    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            processQRResult(result.contents)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inizializza ViewModel
        viewModel = ViewModelProvider(this)[ScanViewModel::class.java]
        viewModel.initialize(requireContext())
        
        setupUI(view)
        observeViewModel()
    }
    
    private fun setupUI(view: View) {
        // Pulsante indietro
        val backButton = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.backButton)
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Pulsante info punti
        val fabPunti = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabPunti)
        fabPunti.setOnClickListener {
            val bottomSheet = PuntiBottomSheet()
            bottomSheet.show(parentFragmentManager, "PuntiBottomSheet")
        }
        
        // Pulsante attiva scanner
        val btnActivateScanner = view.findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.btn_activate_scanner)
        btnActivateScanner.setOnClickListener {
            checkCameraPermissionAndStart()
        }
        

    }
    
    private fun observeViewModel() {
        viewModel.scanResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ScanViewModel.ScanResult.Success -> {
                    showSuccessDialog(result.rifugio, result.pointsEarned)
                }
                is ScanViewModel.ScanResult.AlreadyVisited -> {
                    showAlreadyVisitedDialog(result.rifugio)
                }
                is ScanViewModel.ScanResult.Error -> {
                    showErrorDialog(result.message)
                }
            }
            isScanning = false
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Mostra/nascondi loading indicator se necessario
        }
    }
    
    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun startScanner() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Punta la fotocamera verso il QR code del rifugio")
            .setBeepEnabled(true)
            .setBarcodeImageEnabled(true)
            .setOrientationLocked(false)
        
        scanLauncher.launch(options)
    }
    
    private fun processQRResult(qrContent: String) {
        Log.d(TAG, "QR Code scansionato: $qrContent")
        viewModel.processQRCode(qrContent)
    }
    
    private fun showSuccessDialog(rifugio: Rifugio, pointsEarned: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🎉 Visita Registrata!")
            .setMessage("Hai visitato ${rifugio.nome} e guadagnato $pointsEarned punti!")
            .setPositiveButton("Fantastico!") { _, _ -> }
            .setNegativeButton("Chiudi", null)
            .show()
    }
    
    private fun showAlreadyVisitedDialog(rifugio: Rifugio) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🏔️ Già Visitato")
            .setMessage("Hai già visitato ${rifugio.nome} in precedenza.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("❌ Errore")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permesso Fotocamera")
            .setMessage("L'app ha bisogno del permesso fotocamera per scansionare i QR code dei rifugi.")
            .setPositiveButton("Concedi") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permesso Negato")
            .setMessage("Senza il permesso fotocamera non puoi scansionare i QR code. Puoi concedere il permesso nelle impostazioni.")
            .setPositiveButton("Impostazioni") { _, _ ->
                // TODO Apri impostazioni app
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        isScanning = false
    }
    
    override fun onPause() {
        super.onPause()
        isScanning = false
    }
}