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
import com.example.mountainpassport_girarifugi.databinding.FragmentMapBinding
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapController: IMapController
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var myLocationOverlay: MyLocationNewOverlay? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Lista dei rifugi di esempio
    private val rifugiEsempio = listOf(
        Rifugio(
            id = 1,
            nome = "Rifugio Torino",
            localita = "Courmayeur, Valle d'Aosta",
            altitudine = 3375,
            latitudine = 45.8467,
            longitudine = 6.8719,
            tipo = TipoRifugio.RIFUGIO
        ),
        Rifugio(
            id = 2,
            nome = "Rifugio Vittorio Sella",
            localita = "Alagna Valsesia, Piemonte",
            altitudine = 2584,
            latitudine = 45.9167,
            longitudine = 7.9333,
            tipo = TipoRifugio.RIFUGIO
        ),
        Rifugio(
            id = 3,
            nome = "Bivacco della Grigna",
            localita = "Mandello del Lario, Lombardia",
            altitudine = 2184,
            latitudine = 45.9333,
            longitudine = 9.3833,
            tipo = TipoRifugio.BIVACCO
        )
    )

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
                    updateMapLocation(location)
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

            // Ottieni il controller della mappa
            mapController = binding.mapView.controller

            // Imposta zoom e posizione iniziale sulle Alpi italiane
            mapController.setZoom(9.0)
            val startPoint = GeoPoint(45.7370, 7.3210) // Aosta, Valle d'Aosta
            mapController.setCenter(startPoint)

            // Configura l'overlay per la posizione utente
            setupLocationOverlay()

            // Aggiungi i marker dei rifugi
            addRifugiMarkers()

            // Forza il refresh della mappa
            binding.mapView.invalidate()

            Toast.makeText(requireContext(), "Mappa caricata correttamente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Errore nel caricamento mappa: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupLocationOverlay() {
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapView)
        myLocationOverlay?.enableMyLocation()
        myLocationOverlay?.enableFollowLocation()
        binding.mapView.overlays.add(myLocationOverlay)
    }

    private fun addRifugiMarkers() {
        rifugiEsempio.forEach { rifugio ->
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
        // Qui puoi navigare alla scheda dettagli del rifugio
        // Per ora mostra un toast
        Toast.makeText(
            requireContext(),
            "Apertura dettagli per: ${rifugio.nome}",
            Toast.LENGTH_SHORT
        ).show()

        // Esempio di navigazione (uncomment quando avrai il fragment dei dettagli):
        // findNavController().navigate(
        //     MapFragmentDirections.actionMapFragmentToDettagliRifugioFragment(rifugio.id)
        // )
    }

    private fun setupClickListeners() {
        binding.buttonMyLocation.setOnClickListener {
            if (checkLocationPermissions()) {
                centerMapOnUserLocation()
            } else {
                requestLocationPermissions()
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
                    Toast.makeText(requireContext(), "Permessi di localizzazione negati", Toast.LENGTH_LONG).show()
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
        }
    }

    private fun updateMapLocation(location: Location) {
        // Aggiorna la posizione sulla mappa in tempo reale
        val userLocation = GeoPoint(location.latitude, location.longitude)

        mapController.setCenter(userLocation)

        // L'overlay si aggiornerà automaticamente
        binding.mapView.invalidate()
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