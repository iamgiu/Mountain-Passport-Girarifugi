package com.example.mountainpassport_girarifugi.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.databinding.FragmentCabinBinding

class CabinFragment : Fragment() {

    private var _binding: FragmentCabinBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CabinViewModel
    private lateinit var reviewsAdapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCabinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[CabinViewModel::class.java]

        // Configura gli observer
        setupObservers()

        // Configura i click listeners
        setupClickListeners()

        // Carica i dati del rifugio
        loadRifugioData()

        // Configura la RecyclerView per le recensioni
        setupReviewsRecyclerView()
    }

    private fun setupObservers() {
        viewModel.rifugio.observe(viewLifecycleOwner) { rifugio ->
            rifugio?.let { populateUI(it) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Mostra/nasconde un indicatore di caricamento se necessario
            if (isLoading) {
                // binding.progressBar.visibility = View.VISIBLE
            } else {
                // binding.progressBar.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.isSaved.observe(viewLifecycleOwner) { isSaved ->
            updateSaveButtonIcon(isSaved)
        }
    }

    private fun loadRifugioData() {
        // Recupera l'ID del rifugio dagli argomenti del bundle
        val rifugioId = arguments?.getInt("rifugioId") ?: run {
            Toast.makeText(requireContext(), "Errore: ID rifugio mancante", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        viewModel.loadRifugio(rifugioId)
    }

    private fun setupClickListeners() {
        // Bottone indietro
        binding.fabBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Bottone salva rifugio
        binding.fabSave.setOnClickListener {
            viewModel.toggleSaveRifugio()
        }
    }

    private fun populateUI(rifugio: com.example.mountainpassport_girarifugi.data.model.Rifugio) {
        with(binding) {
            // Dati principali
            cabinNameTextView.text = rifugio.nome
            altitudeTextView.text = "${rifugio.altitudine}m"
            coordinatesTextView.text = "${rifugio.latitudine}\n${rifugio.longitudine}"
            locationTextView.text = rifugio.localita

            // Immagine del rifugio (placeholder)
            cabinImageView.setImageResource(R.drawable.rifugio_torino)

            // Timbro (placeholder)
            stampView.setImageResource(R.drawable.stamps)

            // Servizi (usa le funzioni del ViewModel)
            openingPeriodTextView.text = viewModel.getOpeningPeriod(rifugio)
            bedsTextView.text = viewModel.getBeds(rifugio)

            // Come arrivare (usa le funzioni del ViewModel)
            distanceTextView.text = viewModel.getDistance(rifugio)
            elevationTextView.text = viewModel.getElevation(rifugio)
            timeTextView.text = viewModel.getTime(rifugio)
            difficultyTextView.text = viewModel.getDifficulty(rifugio)
            routeDescriptionTextView.text = viewModel.getRouteDescription(rifugio)

            // Recensioni (usa le funzioni del ViewModel)
            reviewsRatingTextView.text = "⭐ ${viewModel.getAverageRating(rifugio)} (${viewModel.getReviewCount(rifugio)} recensioni)"
        }
    }

    private fun updateSaveButtonIcon(isSaved: Boolean) {
        val iconRes = if (isSaved) {
            R.drawable.ic_bookmark_24px // Icona piena (puoi creare ic_bookmark_filled_24px)
        } else {
            R.drawable.ic_bookmark_24px // Icona vuota
        }

        binding.fabSave.setImageResource(iconRes)

        val message = if (isSaved) {
            "Rifugio salvato!"
        } else {
            "Rifugio rimosso dai salvati"
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun setupReviewsRecyclerView() {
        // Configura l'adapter per le recensioni (quando sarà implementato)
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            // adapter = reviewsAdapter (quando sarà implementato)
        }

        // Per ora nascondiamo la RecyclerView e mostriamo il placeholder
        binding.recyclerViewUsers.visibility = View.GONE
        binding.noReviewsTextView.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}