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
import com.example.mountainpassport_girarifugi.utils.NotificationHelper
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

        viewModel = ViewModelProvider(this)[CabinViewModel::class.java]

        reviewsAdapter = ReviewAdapter()

        setupObservers()

        setupClickListeners()

        loadRifugioData()

        setupReviewsRecyclerView()
    }

    private fun setupObservers() {
        viewModel.rifugio.observe(viewLifecycleOwner) { rifugio ->
            rifugio?.let { populateUI(it) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
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
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearSuccessMessage()
            }
        }

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            viewModel.rifugio.value?.let { populateUI(it) }
        }
    }

    private fun loadRifugioData() {
        val rifugioId = arguments?.getInt("rifugioId") ?: run {
            Toast.makeText(requireContext(), "Errore: ID rifugio mancante", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        viewModel.loadRifugio(rifugioId)
    }

    /**
     * Setup dei bottoni
     */
    private fun setupClickListeners() {
        binding.fabBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.fabSave.setOnClickListener {
            val currentState = viewModel.isSaved.value ?: false
            viewModel.toggleSaveRifugio()

            val message = if (!currentState) {
                "Rifugio salvato!"
            } else {
                "Rifugio rimosso dai salvati"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        binding.fabAddReview.setOnClickListener {
            showAddReviewDialog()
        }
    }

    private fun populateUI(rifugio: com.example.mountainpassport_girarifugi.data.model.Rifugio) {
        with(binding) {
            cabinNameTextView.text = rifugio.nome
            altitudeTextView.text = "${rifugio.altitudine}m"
            coordinatesTextView.text = "${rifugio.latitudine}\n${rifugio.longitudine}"
            locationTextView.text = rifugio.localita

            if (!rifugio.immagineUrl.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(rifugio.immagineUrl)
                    .placeholder(R.drawable.rifugio_torino)
                    .error(R.drawable.rifugio_torino)
                    .centerCrop()
                    .into(cabinImageView)
            } else {
                cabinImageView.setImageResource(R.drawable.rifugio_torino)
            }

            stampView.setImageResource(R.drawable.stamps)

            openingPeriodTextView.text = viewModel.getOpeningPeriod(rifugio)
            bedsTextView.text = viewModel.getBeds(rifugio)

            updateServicesVisibility(rifugio)

            distanceTextView.text = viewModel.getDistance(rifugio)
            elevationTextView.text = viewModel.getElevation(rifugio)
            timeTextView.text = viewModel.getTime(rifugio)
            difficultyTextView.text = viewModel.getDifficulty(rifugio)
            routeDescriptionTextView.text = viewModel.getRouteDescription(rifugio)

            reviewsRatingTextView.text = "⭐ ${viewModel.getAverageRating(rifugio)} (${viewModel.getReviewCount(rifugio)} recensioni)"

            val rifugioPoints = viewModel.getRifugioPoints(rifugio)
            pointsTextView.text = if (rifugioPoints.isDoublePoints) {
                "+${rifugioPoints.totalPoints} punti (doppi!)"
            } else {
                "+${rifugioPoints.totalPoints} punti"
            }
        }
    }

    /**
     * Cambia icona del favSave
     */
    private fun updateSaveButtonIcon(isSaved: Boolean) {
        val iconRes = if (isSaved) {
            R.drawable.ic_bookmark_added_24px
        } else {
            R.drawable.ic_bookmark_add_24px
        }

        binding.fabSave.setImageResource(iconRes)
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

    /**
     * Controlla se ci sono i servizi all'interno del JSON
     */
    private fun updateServicesVisibility(rifugio: com.example.mountainpassport_girarifugi.data.model.Rifugio) {
        android.util.Log.d("CabinFragment", "Aggiornando servizi per rifugio: ${rifugio.nome} (Tipo: ${rifugio.tipo}, Altitudine: ${rifugio.altitudine})")

        val hasHotWater = viewModel.hasHotWater(rifugio)
        val hasShowers = viewModel.hasShowers(rifugio)
        val hasElectricity = viewModel.hasElectricity(rifugio)
        val hasRestaurant = viewModel.hasRestaurant(rifugio)

        android.util.Log.d("CabinFragment", "Servizi disponibili - Acqua: $hasHotWater, Docce: $hasShowers, Elettricità: $hasElectricity, Ristorante: $hasRestaurant")

        with(binding) {
            hotWaterLayout.visibility = if (hasHotWater) View.VISIBLE else View.GONE
            android.util.Log.d("CabinFragment", "Acqua calda visibility: ${hotWaterLayout.visibility}")

            showersLayout.visibility = if (hasShowers) View.VISIBLE else View.GONE
            android.util.Log.d("CabinFragment", "Docce visibility: ${showersLayout.visibility}")

            electricityLayout.visibility = if (hasElectricity) View.VISIBLE else View.GONE
            android.util.Log.d("CabinFragment", "Elettricità visibility: ${electricityLayout.visibility}")

            restaurantLayout.visibility = if (hasRestaurant) View.VISIBLE else View.GONE
            android.util.Log.d("CabinFragment", "Ristorante visibility: ${restaurantLayout.visibility}")
        }
    }

    /**
     * Apre il dialogo per aggiungere una recensione al rifugio
     */
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