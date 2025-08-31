package com.example.mountainpassport_girarifugi.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.example.mountainpassport_girarifugi.data.model.TipoRifugio
import com.example.mountainpassport_girarifugi.data.repository.RifugioRepository
import com.example.mountainpassport_girarifugi.databinding.FragmentMapBinding
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent

class MapFragment : Fragment(), MapListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapController: IMapController
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private lateinit var rifugioRepository: RifugioRepository

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Lista dei rifugi caricati dal JSON
    private var rifugiList: List<Rifugio> = emptyList()

    // Flag per controllare il follow location
    private var isFollowingLocation = true
    private var userInteractedWithMap = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza il repository
        rifugioRepository = RifugioRepository(requireContext())

        // Inizializza il client di localizzazione
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Configura la richiesta di localizzazione
        setupLocationRequest()

        // Configura il callback per gli aggiornamenti di posizione
        setupLocationCallback()

        // Inizializza osmdroid
        initializeMap()

        // Configura i bottoni
        setupClickListeners()

        // Richiedi i permessi di localizzazione
        requestLocationPermissions()

        // Inizializzazione Search Cabin Button
        setupSeachCabinButton(view)

    }

    private fun setupSeachCabinButton(view: View) {
        val buttonFilterRifugi = view.findViewById<FloatingActionButton>(R.id.buttonFilterRifugi)
        buttonFilterRifugi.setOnClickListener {
            findNavController().navigate(R.id.action_mapFragment_to_SearchCabinFragment)
        }
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // Aggiorna ogni 5 secondi
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000) // Minimo 2 secondi tra aggiornamenti
            .setMaxUpdateDelayMillis(10000) // Massimo ritardo 10 secondi
            .build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    // Aggiorna la posizione solo se l'utente non ha interagito con la mappa
                    // o se è attivo il follow location
                    if (isFollowingLocation && !userInteractedWithMap) {
                        updateMapLocation(location)
                    }
                }
            }
        }
    }

    private fun initializeMap() {
        try {
            // Configura osmdroid
            Configuration.getInstance().load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext())
            )

            // Imposta User Agent per evitare problemi con le tile
            Configuration.getInstance().userAgentValue = requireContext().packageName

            // Configura la mappa
            binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
            binding.mapView.setMultiTouchControls(true)

            // Aggiungi il listener per detectare le interazioni utente
            binding.mapView.addMapListener(this)

            // Ottieni il controller della mappa
            mapController = binding.mapView.controller

            // Imposta zoom e posizione iniziale sulle Alpi italiane
            mapController.setZoom(9.0)
            val startPoint = GeoPoint(45.7370, 7.3210) // Aosta, Valle d'Aosta
            mapController.setCenter(startPoint)

            // Configura l'overlay per la posizione utente
            setupLocationOverlay()

            // Carica i rifugi dal JSON e aggiungi i marker
            loadRifugiAndAddMarkers()

            // Forza il refresh della mappa
            binding.mapView.invalidate()

            Toast.makeText(requireContext(), "Mappa caricata correttamente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Errore nel caricamento mappa: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadRifugiAndAddMarkers() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Carica i rifugi dal JSON in background
                val rifugi = withContext(Dispatchers.IO) {
                    rifugioRepository.getAllRifugi()
                }

                rifugiList = rifugi

                // Aggiungi i marker alla mappa
                addRifugiMarkers()

                // Mostra un messaggio con il numero di rifugi caricati
                Toast.makeText(
                    requireContext(),
                    "Caricati ${rifugi.size} rifugi sulla mappa",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Errore nel caricamento rifugi: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupLocationOverlay() {
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapView)
        myLocationOverlay?.enableMyLocation()
        // NON abilitare il follow location automatico
        // myLocationOverlay?.enableFollowLocation()
        binding.mapView.overlays.add(myLocationOverlay)
    }

    private fun addRifugiMarkers() {
        rifugiList.forEach { rifugio ->
            val marker = Marker(binding.mapView).apply {
                position = GeoPoint(rifugio.latitudine, rifugio.longitudine)
                title = rifugio.nome
                snippet = "${rifugio.localita} - ${rifugio.altitudine} m"

                // Imposta l'icona in base al tipo di rifugio
                icon = getMarkerIcon(rifugio.tipo)

                // Configura l'info window personalizzata
                infoWindow = CustomInfoWindow(binding.mapView, rifugio) { rifugioCliccato ->
                    onRifugioDettagliClick(rifugioCliccato)
                }

                // Gestisci il click sul marker
                setOnMarkerClickListener { marker, mapView ->
                    if (marker.isInfoWindowShown) {
                        marker.closeInfoWindow()
                    } else {
                        marker.showInfoWindow()
                    }
                    true
                }
            }

            binding.mapView.overlays.add(marker)
        }
    }

    private fun getMarkerIcon(tipo: TipoRifugio): Drawable? {
        return when (tipo) {
            TipoRifugio.RIFUGIO -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_cabin_24)
            TipoRifugio.BIVACCO -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_cabin_24)
            TipoRifugio.CAPANNA -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_cabin_24)
        }?.apply {
            // Colora l'icona
            setTint(ContextCompat.getColor(requireContext(), R.color.brown))
        }
    }

    private fun onRifugioDettagliClick(rifugio: Rifugio) {
        // Naviga al dettaglio del rifugio
        val bundle = Bundle().apply {
            putInt("rifugioId", rifugio.id)
        }
        findNavController().navigate(R.id.action_mapFragment_to_cabinFragment, bundle)
    }

    private fun setupClickListeners() {
        binding.buttonMyLocation.setOnClickListener {
            if (checkLocationPermissions()) {
                // Riabilita il follow location e centra sulla posizione utente
                isFollowingLocation = true
                userInteractedWithMap = false
                centerMapOnUserLocation()
            } else {
                requestLocationPermissions()
            }
        }
    }

    // Implementazione MapListener per detectare interazioni utente
    override fun onScroll(event: ScrollEvent?): Boolean {
        // L'utente ha fatto scroll, disabilita il follow location
        userInteractedWithMap = true
        isFollowingLocation = false
        return false
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        // L'utente ha fatto zoom, disabilita il follow location
        userInteractedWithMap = true
        isFollowingLocation = false
        return false
    }

    private fun requestLocationPermissions() {
        if (!checkLocationPermissions()) {
            // Mostra un messaggio informativo prima di richiedere i permessi
            Toast.makeText(
                requireContext(),
                "L'app ha bisogno dei permessi di localizzazione per mostrare la tua posizione sulla mappa",
                Toast.LENGTH_LONG
            ).show()

            // Richiedi i permessi
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
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
                    startLocationUpdates()
                    Toast.makeText(requireContext(), "Permessi di localizzazione concessi", Toast.LENGTH_SHORT).show()
                } else {
                    // Verifica se l'utente ha negato i permessi permanentemente
                    val shouldShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                    if (shouldShowRationale) {
                        Toast.makeText(
                            requireContext(),
                            "I permessi di localizzazione sono necessari per mostrare la tua posizione sulla mappa. Puoi abilitarli nelle impostazioni dell'app.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Permessi di localizzazione negati. La tua posizione non sarà mostrata sulla mappa.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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

    private fun centerMapOnUserLocation() {
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
            val locationTask: Task<Location> = fusedLocationClient.lastLocation
            locationTask.addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = GeoPoint(location.latitude, location.longitude)
                    mapController.setCenter(userLocation)
                    mapController.setZoom(18.7)
                    Toast.makeText(requireContext(), "Centrato sulla tua posizione", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Impossibile ottenere la posizione attuale", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Se i permessi non sono concessi, richiedili
            Toast.makeText(
                requireContext(),
                "Permessi di localizzazione necessari per centrare la mappa sulla tua posizione",
                Toast.LENGTH_LONG
            ).show()
            requestLocationPermissions()
        }
    }

    private fun updateMapLocation(location: Location) {
        // Aggiorna la posizione sulla mappa solo se il follow location è attivo
        if (isFollowingLocation && !userInteractedWithMap) {
            val userLocation = GeoPoint(location.latitude, location.longitude)
            mapController.setCenter(userLocation)
            binding.mapView.invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        if (checkLocationPermissions()) {
            startLocationUpdates()
        }
        myLocationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        stopLocationUpdates()
        myLocationOverlay?.disableMyLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLocationUpdates()
        binding.mapView.onDetach()
        _binding = null
    }
}