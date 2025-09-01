package com.example.mountainpassport_girarifugi.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.ui.map.RifugioSavedEventBus
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.bumptech.glide.Glide

class CabinFragment : Fragment() {

    private lateinit var viewModel: CabinViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cabin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[CabinViewModel::class.java]

        // Configura gli observer
        setupObservers(view)

        // Configura i click listeners
        setupClickListeners(view)

        // Carica i dati del rifugio
        loadRifugioData()
    }

    private fun setupObservers(view: View) {
        viewModel.rifugio.observe(viewLifecycleOwner) { rifugio ->
            rifugio?.let { populateUI(view, it) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Mostra/nasconde loading se hai un ProgressBar
            view.findViewById<ProgressBar>(R.id.progressBar)?.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.isSaved.observe(viewLifecycleOwner) { isSaved ->
            updateSaveButtonIcon(view, isSaved)
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearSuccessMessage()
            }
        }
    }

    private fun loadRifugioData() {
        val rifugioId = arguments?.getInt("rifugioId")
        val rifugioNome = arguments?.getString("RIFUGIO_NOME")

        when {
            // Caso 1: Abbiamo l'ID (navigazione moderna)
            rifugioId != null && rifugioId != 0 -> {
                viewModel.loadRifugioById(rifugioId)
            }
            // Caso 2: Abbiamo solo il nome (navigazione legacy dal HomeFragment)
            rifugioNome != null -> {
                viewModel.loadRifugioByName(rifugioNome)
            }
            // Caso 3: Abbiamo tutti i dati negli arguments (compatibilità totale)
            else -> {
                loadFromArguments()
            }
        }
    }

    private fun loadFromArguments() {
        // Carica i dati direttamente dagli arguments per compatibilità
        val rifugioNome = arguments?.getString("RIFUGIO_NOME") ?: "Rifugio Sconosciuto"
        val rifugioAltitudine = arguments?.getString("RIFUGIO_ALTITUDINE") ?: "0 m"
        val rifugioDistanza = arguments?.getString("RIFUGIO_DISTANZA") ?: "0 km"
        val rifugioLocalita = arguments?.getString("RIFUGIO_LOCALITA") ?: "Località sconosciuta"
        val rifugioCoordinate = arguments?.getString("RIFUGIO_COORDINATE") ?: "0.0000,0.0000"
        val rifugioDifficolta = arguments?.getString("RIFUGIO_DIFFICOLTA") ?: "Non specificata"
        val rifugioTempo = arguments?.getString("RIFUGIO_TEMPO") ?: "Non specificato"
        val rifugioDescrizione = arguments?.getString("RIFUGIO_DESCRIZIONE") ?: "Nessuna descrizione disponibile"

        viewModel.loadFromArguments(
            rifugioNome, rifugioAltitudine, rifugioDistanza, rifugioLocalita,
            rifugioCoordinate, rifugioDifficolta, rifugioTempo, rifugioDescrizione
        )
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<FloatingActionButton>(R.id.fabBack)?.setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<FloatingActionButton>(R.id.fabSave)?.setOnClickListener {
            val currentState = viewModel.isSaved.value ?: false
            viewModel.toggleSaveRifugio()

            val message = if (!currentState) "Rifugio salvato!" else "Rifugio rimosso dai salvati"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

    }

    private fun populateUI(view: View, rifugio: CabinViewModel.RifugioDisplay) {
        // Dati principali
        view.findViewById<TextView>(R.id.cabinNameTextView)?.text = rifugio.nome
        view.findViewById<TextView>(R.id.altitudeTextView)?.text = rifugio.altitudine
        view.findViewById<TextView>(R.id.locationTextView)?.text = rifugio.localita

        // Coordinate
        val coords = rifugio.coordinate.split(",")
        if (coords.size == 2) {
            view.findViewById<TextView>(R.id.coordinatesTextView)?.text = "${coords[0]}\n${coords[1]}"
        }

        // Dati del percorso
        view.findViewById<TextView>(R.id.distanceTextView)?.text = rifugio.distanza
        view.findViewById<TextView>(R.id.timeTextView)?.text = rifugio.tempo
        view.findViewById<TextView>(R.id.difficultyTextView)?.text = "Difficoltà: ${rifugio.difficolta}"
        view.findViewById<TextView>(R.id.routeDescriptionTextView)?.text = rifugio.descrizione

        // Immagine del rifugio
        setupRifugioImage(view, rifugio.nome, rifugio.immagineUrl)

        // Punti (se il rifugio ha un ID valido)
        if (rifugio.id != -1) {
            view.findViewById<TextView>(R.id.pointsTextView)?.text = rifugio.punti
        }
    }

    private fun updateSaveButtonIcon(view: View, isSaved: Boolean) {
        val iconRes = if (isSaved) {
            R.drawable.ic_bookmark_added_24px
        } else {
            R.drawable.ic_bookmark_add_24px
        }
        view.findViewById<FloatingActionButton>(R.id.fabSave)?.setImageResource(iconRes)
    }

    private fun setupRifugioImage(view: View, nomeRifugio: String, immagineUrl: String?) {
        val imageView = view.findViewById<ImageView>(R.id.cabinImageView) ?: return

        // Prova prima con l'URL se disponibile
        if (!immagineUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(immagineUrl)
                .placeholder(R.drawable.rifugio_torino)
                .error(R.drawable.rifugio_torino)
                .centerCrop()
                .into(imageView)
        } else {
            // Fallback alle immagini locali
            val nomeRisorsa = nomeRifugio
                .lowercase()
                .replace(" ", "_")
                .replace("à", "a")
                .replace("è", "e")
                .replace("ì", "i")
                .replace("ò", "o")
                .replace("ù", "u")

            val resId = resources.getIdentifier(nomeRisorsa, "drawable", requireContext().packageName)

            if (resId != 0) {
                imageView.setImageResource(resId)
            } else {
                imageView.setImageResource(R.drawable.rifugio_torino)
            }
        }
    }

    companion object {
        fun newInstance(
            nome: String,
            altitudine: String,
            distanza: String,
            localita: String,
            coordinate: String,
            difficolta: String,
            tempo: String,
            descrizione: String
        ): CabinFragment {
            val fragment = CabinFragment()
            val args = Bundle().apply {
                putString("RIFUGIO_NOME", nome)
                putString("RIFUGIO_ALTITUDINE", altitudine)
                putString("RIFUGIO_DISTANZA", distanza)
                putString("RIFUGIO_LOCALITA", localita)
                putString("RIFUGIO_COORDINATE", coordinate)
                putString("RIFUGIO_DIFFICOLTA", difficolta)
                putString("RIFUGIO_TEMPO", tempo)
                putString("RIFUGIO_DESCRIZIONE", descrizione)
            }
            fragment.arguments = args
            return fragment
        }
    }
}