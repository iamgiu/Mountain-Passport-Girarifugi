package com.example.mountainpassport_girarifugi.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.example.mountainpassport_girarifugi.databinding.FragmentSearchCabinBinding
import com.google.android.gms.location.*
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SearchCabinFragment : Fragment() {

    private var _binding: FragmentSearchCabinBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SearchCabinViewModel
    private lateinit var adapter: SearchCabinAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private val LOCATION_PERMISSION_REQUEST_CODE = 1002

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchCabinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza ViewModel
        viewModel = ViewModelProvider(this)[SearchCabinViewModel::class.java]

        // Inizializza il client di localizzazione
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupLocationRequest()
        setupLocationCallback()

        // Configura l'adapter
        setupAdapter()

        // Configura la RecyclerView
        setupRecyclerView()

        // Configura la SearchView
        setupSearchView()

        // Configura i bottoni
        setupClickListeners()

        // Osserva i dati del ViewModel
        observeViewModel()

        // Richiedi i permessi di localizzazione e ottieni la posizione
        requestLocationPermissions()
    }

    private fun setupAdapter() {
        adapter = SearchCabinAdapter(
            onRifugioClick = { rifugio ->
                onRifugioClick(rifugio)
            },
            getDistance = { rifugio ->
                viewModel.getDistanceToRifugio(rifugio)
            }
        )
    }

    private fun setupRecyclerView() {
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SearchCabinFragment.adapter

            // Aggiungi animazioni
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            
            // Aggiungi un listener per il layout
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                // Quando il layout cambia, prova a scrollare al top se necessario
                adapter?.let { safeAdapter ->
                    if (safeAdapter.itemCount > 0) {
                        post {
                            scrollToPosition(0)
                        }
                    }
                }
            }
        }
    }

    private fun setupSearchView() {
        binding.searchView.apply {
            queryHint = "Cerca rifugi..."

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { viewModel.searchRifugi(it) }
                    clearFocus()
                    
                    // Scroll al top dopo la ricerca
                    binding.recyclerViewUsers.postDelayed({
                        binding.recyclerViewUsers.scrollToPosition(0)
                    }, 100)
                    
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let { viewModel.searchRifugi(it) }
                    
                    // Scroll al top quando cambia il testo
                    if (!newText.isNullOrEmpty()) {
                        binding.recyclerViewUsers.postDelayed({
                            binding.recyclerViewUsers.scrollToPosition(0)
                        }, 100)
                    }
                    
                    return true
                }
            })

            // Gestisci il pulsante di chiusura
            setOnCloseListener {
                viewModel.clearSearch()
                false
            }

            // Gestisci anche quando la query viene cancellata manualmente
            setOnSearchClickListener {
                // Quando si clicca sulla search view, assicurati che sia visibile
            }
        }
    }

    private fun setupClickListeners() {
        // Bottone per tornare alla mappa
        binding.fabForwardMap.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeViewModel() {
        viewModel.filteredRifugi.observe(viewLifecycleOwner) { rifugi ->
            android.util.Log.d("SearchCabinFragment", "Lista aggiornata con ${rifugi.size} elementi")
            if (rifugi.isNotEmpty()) {
                android.util.Log.d("SearchCabinFragment", "Primo elemento: ${rifugi[0].nome}")
            }
            
            adapter.submitList(rifugi)
            
            // Scroll al primo elemento dopo aver aggiornato la lista
            if (rifugi.isNotEmpty()) {
                android.util.Log.d("SearchCabinFragment", "Tentativo di scroll al primo elemento")
                binding.recyclerViewUsers.postDelayed({
                    try {
                        // Forza un layout refresh
                        binding.recyclerViewUsers.layoutManager?.requestLayout()
                        
                        // Prova prima con scroll immediato
                        binding.recyclerViewUsers.scrollToPosition(0)
                        android.util.Log.d("SearchCabinFragment", "Scroll immediato eseguito")
                        
                        // Se non funziona, prova con smooth scroll
                        binding.recyclerViewUsers.postDelayed({
                            binding.recyclerViewUsers.smoothScrollToPosition(0)
                            android.util.Log.d("SearchCabinFragment", "Smooth scroll eseguito")
                        }, 50)
                    } catch (e: Exception) {
                        android.util.Log.e("SearchCabinFragment", "Errore nello scroll: ${e.message}")
                    }
                }, 300) // Aumentato ulteriormente il delay
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.hasResults.observe(viewLifecycleOwner) { hasResults ->
            binding.textViewNoResults.visibility = if (hasResults) View.GONE else View.VISIBLE
            binding.recyclerViewUsers.visibility = if (hasResults) View.VISIBLE else View.GONE

            if (!hasResults) {
                binding.textViewNoResults.text = "Nessun rifugio trovato"
            }
        }
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // Aggiorna ogni 10 secondi
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000) // Minimo 5 secondi tra aggiornamenti
            .setMaxUpdateDelayMillis(15000) // Massimo ritardo 15 secondi
            .build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    viewModel.updateUserLocation(location)
                    // Continua ad aggiornare la posizione per mantenere l'ordinamento aggiornato
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        if (!checkLocationPermissions()) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    private fun checkLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                    Toast.makeText(
                        requireContext(),
                        "GPS attivato! I rifugi saranno ordinati per distanza.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Permessi di localizzazione negati. I rifugi non saranno ordinati per distanza.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun getCurrentLocation() {
        if (checkLocationPermissions()) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            // Prima prova con l'ultima posizione conosciuta
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.updateUserLocation(location)
                    // Avvia aggiornamenti continui per mantenere la posizione aggiornata
                    startLocationUpdates()
                } else {
                    // Se non c'è una posizione conosciuta, richiedi aggiornamenti
                    startLocationUpdates()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (checkLocationPermissions()) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun onRifugioClick(rifugio: Rifugio) {
        // Naviga al CabinFragment con l'ID del rifugio selezionato
        try {
            val bundle = Bundle().apply {
                putInt("rifugioId", rifugio.id)
            }

            // Naviga al fragment del rifugio
            findNavController().navigate(R.id.action_searchCabinFragment_to_cabinFragment, bundle)

        } catch (e: Exception) {
            // Fallback in caso di errore di navigazione
            Toast.makeText(
                requireContext(),
                "Apertura dettagli per: ${rifugio.nome}\nAltitudine: ${rifugio.altitudine}m\nDistanza: ${viewModel.getDistanceToRifugio(rifugio)}",
                Toast.LENGTH_LONG
            ).show()

            // Log dell'errore per il debug
            android.util.Log.e("SearchCabinFragment", "Errore navigazione: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // I rifugi vengono caricati automaticamente nel ViewModel
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLocationUpdates()
        _binding = null
    }
}