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

    private var rifugiList: List<Rifugio> = emptyList()

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

    /**
     * Inizializza il repository
     *
     * Configura richiesta e callback di localizzazione
     *
     * Avvia la mappa
     *
     * Collega i bottoni
     *
     * Richiede i permessi di localizzaizone
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rifugioRepository = RifugioRepository(requireContext())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupLocationRequest()

        setupLocationCallback()

        initializeMap()

        setupClickListeners()

        requestLocationPermissions()

        setupSeachCabinButton(view)

    }

    /**
     * Passa dalla mappa alla pagina SearchCabin
     */
    private fun setupSeachCabinButton(view: View) {
        val buttonFilterRifugi = view.findViewById<FloatingActionButton>(R.id.buttonFilterRifugi)
        buttonFilterRifugi.setOnClickListener {
            findNavController().navigate(R.id.action_mapFragment_to_SearchCabinFragment)
        }
    }

    /**
     * Aggiorna la posizione ogni 5 secondi
     */
    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdateDelayMillis(10000)
            .build()
    }

    /**
     * Aggiorna la posizione solo se l'utente non ha interagito manualmente e isFollowingLocation è true
     */
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    if (isFollowingLocation && !userInteractedWithMap) {
                        updateMapLocation(location)
                    }
                }
            }
        }
    }

    /**
     * Configura osmdroid
     *
     * Centra inizialmente la mappa su Valle d'Aosta con zoom 9
     *
     * Carica i rifugi dal JSON tramite repository
     *
     * Aggiorna la mappa
     */
    private fun initializeMap() {
        try {
            Configuration.getInstance().load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext())
            )

            Configuration.getInstance().userAgentValue = requireContext().packageName

            binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
            binding.mapView.setMultiTouchControls(true)

            binding.mapView.addMapListener(this)

            mapController = binding.mapView.controller

            mapController.setZoom(9.0)
            val startPoint = GeoPoint(45.7370, 7.3210) // Aosta, Valle d'Aosta
            mapController.setCenter(startPoint)

            setupLocationOverlay()

            loadRifugiAndAddMarkers()

            binding.mapView.invalidate()

            Toast.makeText(requireContext(), "Mappa caricata correttamente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Errore nel caricamento mappa: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Carica i rifugi nella mappa con il rispettivo marker
     */
    private fun loadRifugiAndAddMarkers() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Carica i rifugi dal JSON in background
                val rifugi = withContext(Dispatchers.IO) {
                    rifugioRepository.getAllRifugi()
                }

                rifugiList = rifugi

                addRifugiMarkers()

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

        binding.mapView.overlays.add(myLocationOverlay)
    }

    /**
     * Ogni marker ha posizione (longitudine e latitudine), mostra nome e altitudine, il pulsante per i dettagli che porta alla pagina del rifugio corrispondente
     */
    private fun addRifugiMarkers() {
        rifugiList.forEach { rifugio ->
            val marker = Marker(binding.mapView).apply {
                position = GeoPoint(rifugio.latitudine, rifugio.longitudine)
                title = rifugio.nome
                snippet = "${rifugio.localita} - ${rifugio.altitudine} m"

                icon = getMarkerIcon(rifugio.tipo)

                infoWindow = CustomInfoWindow(binding.mapView, rifugio) { rifugioCliccato ->
                    onRifugioDettagliClick(rifugioCliccato)
                }

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
            setTint(ContextCompat.getColor(requireContext(), R.color.brown))
        }
    }

    private fun onRifugioDettagliClick(rifugio: Rifugio) {
        val bundle = Bundle().apply {
            putInt("rifugioId", rifugio.id)
        }
        findNavController().navigate(R.id.action_mapFragment_to_cabinFragment, bundle)
    }

    private fun setupClickListeners() {
        binding.buttonMyLocation.setOnClickListener {
            if (checkLocationPermissions()) {
                isFollowingLocation = true
                userInteractedWithMap = false
                centerMapOnUserLocation()
            } else {
                requestLocationPermissions()
            }
        }
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        userInteractedWithMap = true
        isFollowingLocation = false
        return false
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        userInteractedWithMap = true
        isFollowingLocation = false
        return false
    }

    /**
     * Richiede i permessi di localizzazione
     */
    private fun requestLocationPermissions() {
        if (!checkLocationPermissions()) {
            Toast.makeText(
                requireContext(),
                "L'app ha bisogno dei permessi di localizzazione per mostrare la tua posizione sulla mappa",
                Toast.LENGTH_LONG
            ).show()

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
            Toast.makeText(
                requireContext(),
                "Permessi di localizzazione necessari per centrare la mappa sulla tua posizione",
                Toast.LENGTH_LONG
            ).show()
            requestLocationPermissions()
        }
    }

    private fun updateMapLocation(location: Location) {
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