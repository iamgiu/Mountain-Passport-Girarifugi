package com.example.mountainpassport_girarifugi.ui.map

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.databinding.FragmentCabinBinding
import com.example.mountainpassport_girarifugi.data.model.RifugioPoints
import com.bumptech.glide.Glide
import android.widget.Button
import android.widget.EditText

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

        // Inizializza l'adapter per le recensioni
        reviewsAdapter = ReviewAdapter()

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
        
        viewModel.reviews.observe(viewLifecycleOwner) { reviews ->
            android.util.Log.d("CabinFragment", "Recensioni aggiornate: ${reviews.size} recensioni")
            reviewsAdapter.updateReviews(reviews)
            updateReviewsVisibility(reviews)
            
            if (reviews.isNotEmpty()) {
                Toast.makeText(requireContext(), "Recensioni aggiornate: ${reviews.size} recensioni", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearSuccessMessage()
            }
        }
        
        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            // Aggiorna le statistiche quando cambiano
            viewModel.rifugio.value?.let { populateUI(it) }
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

        // Bottone registra visita
        binding.fabVisit.setOnClickListener {
            val rifugio = viewModel.rifugio.value
            if (rifugio != null) {
                viewModel.recordVisit()
            }
        }

        // Bottone salva rifugio
        binding.fabSave.setOnClickListener {
            val currentState = viewModel.isSaved.value ?: false
            viewModel.toggleSaveRifugio()
            
            // Mostra il toast appropriato
            val message = if (!currentState) {
                "Rifugio salvato!"
            } else {
                "Rifugio rimosso dai salvati"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        
        // Bottone per aggiungere recensioni di test
        binding.addTestReviewsButton.setOnClickListener {
            android.util.Log.d("CabinFragment", "Bottone Aggiungi Recensioni Test cliccato")
            Toast.makeText(requireContext(), "Aggiungendo recensioni di test...", Toast.LENGTH_SHORT).show()
            viewModel.addTestReviews()
        }
        
        // Bottone per aggiungere recensione utente
        binding.addReviewButton.setOnClickListener {
            showAddReviewDialog()
        }
    }

    private fun populateUI(rifugio: com.example.mountainpassport_girarifugi.data.model.Rifugio) {
        with(binding) {
            // Dati principali
            cabinNameTextView.text = rifugio.nome
            altitudeTextView.text = "${rifugio.altitudine}m"
            coordinatesTextView.text = "${rifugio.latitudine}\n${rifugio.longitudine}"
            locationTextView.text = rifugio.localita

            // Immagine del rifugio
            if (!rifugio.immagineUrl.isNullOrEmpty()) {
                // Carica l'immagine dall'URL usando Glide
                Glide.with(requireContext())
                    .load(rifugio.immagineUrl)
                    .placeholder(R.drawable.rifugio_torino) // Immagine di fallback
                    .error(R.drawable.rifugio_torino) // Immagine in caso di errore
                    .centerCrop()
                    .into(cabinImageView)
            } else {
                // Se non c'è URL, usa l'immagine di default
                cabinImageView.setImageResource(R.drawable.rifugio_torino)
            }

            // Timbro (placeholder)
            stampView.setImageResource(R.drawable.stamps)

            // Servizi (usa le funzioni del ViewModel)
            openingPeriodTextView.text = viewModel.getOpeningPeriod(rifugio)
            bedsTextView.text = viewModel.getBeds(rifugio)
            
            // Servizi dinamici
            updateServicesVisibility(rifugio)

            // Come arrivare (usa le funzioni del ViewModel)
            distanceTextView.text = viewModel.getDistance(rifugio)
            elevationTextView.text = viewModel.getElevation(rifugio)
            timeTextView.text = viewModel.getTime(rifugio)
            difficultyTextView.text = viewModel.getDifficulty(rifugio)
            routeDescriptionTextView.text = viewModel.getRouteDescription(rifugio)

            // Recensioni (usa le funzioni del ViewModel)
            reviewsRatingTextView.text = "⭐ ${viewModel.getAverageRating(rifugio)} (${viewModel.getReviewCount(rifugio)} recensioni)"

            // Punti disponibili per questo rifugio
            val rifugioPoints = viewModel.getRifugioPoints(rifugio)
            pointsTextView.text = if (rifugioPoints.isDoublePoints) {
                "+${rifugioPoints.totalPoints} punti (doppi!)"
            } else {
                "+${rifugioPoints.totalPoints} punti"
            }
        }
    }

    private fun updateSaveButtonIcon(isSaved: Boolean) {
        val iconRes = if (isSaved) {
            R.drawable.ic_bookmark_24px // Icona piena (puoi creare ic_bookmark_filled_24px)
        } else {
            R.drawable.ic_bookmark_24px // Icona vuota
        }

        binding.fabSave.setImageResource(iconRes)
        
        // Non mostrare toast qui, solo aggiornare l'icona
        // Il toast verrà mostrato solo quando l'utente clicca il bottone
    }

    private fun setupReviewsRecyclerView() {
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewsAdapter
        }
    }
    
    private fun updateReviewsVisibility(reviews: List<com.example.mountainpassport_girarifugi.data.model.Review>) {
        if (reviews.isEmpty()) {
            binding.recyclerViewUsers.visibility = View.GONE
            binding.noReviewsTextView.visibility = View.VISIBLE
        } else {
            binding.recyclerViewUsers.visibility = View.VISIBLE
            binding.noReviewsTextView.visibility = View.GONE
        }
    }
    
    private fun updateServicesVisibility(rifugio: com.example.mountainpassport_girarifugi.data.model.Rifugio) {
        android.util.Log.d("CabinFragment", "Aggiornando servizi per rifugio: ${rifugio.nome} (Tipo: ${rifugio.tipo}, Altitudine: ${rifugio.altitudine})")
        
        val hasHotWater = viewModel.hasHotWater(rifugio)
        val hasShowers = viewModel.hasShowers(rifugio)
        val hasElectricity = viewModel.hasElectricity(rifugio)
        val hasRestaurant = viewModel.hasRestaurant(rifugio)
        
        android.util.Log.d("CabinFragment", "Servizi disponibili - Acqua: $hasHotWater, Docce: $hasShowers, Elettricità: $hasElectricity, Ristorante: $hasRestaurant")
        
        with(binding) {
            // Acqua calda
            hotWaterLayout.visibility = if (hasHotWater) View.VISIBLE else View.GONE
            android.util.Log.d("CabinFragment", "Acqua calda visibility: ${hotWaterLayout.visibility}")
            
            // Docce
            showersLayout.visibility = if (hasShowers) View.VISIBLE else View.GONE
            android.util.Log.d("CabinFragment", "Docce visibility: ${showersLayout.visibility}")
            
            // Luce elettrica
            electricityLayout.visibility = if (hasElectricity) View.VISIBLE else View.GONE
            android.util.Log.d("CabinFragment", "Elettricità visibility: ${electricityLayout.visibility}")
            
            // Ristorante
            restaurantLayout.visibility = if (hasRestaurant) View.VISIBLE else View.GONE
            android.util.Log.d("CabinFragment", "Ristorante visibility: ${restaurantLayout.visibility}")
        }
    }
    
    private fun showAddReviewDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_review, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val commentEditText = dialogView.findViewById<EditText>(R.id.commentEditText)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        
        submitButton.setOnClickListener {
            val rating = ratingBar.rating
            val comment = commentEditText.text.toString().trim()
            
            if (comment.isEmpty()) {
                Toast.makeText(requireContext(), "Inserisci un commento", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (rating == 0f) {
                Toast.makeText(requireContext(), "Inserisci una valutazione", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.addUserReview(rating, comment)
            dialog.dismiss()
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}