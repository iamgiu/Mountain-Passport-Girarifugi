package com.example.mountainpassport_girarifugi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.graphics.Paint
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.mountainpassport_girarifugi.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.Intent
import com.bumptech.glide.Glide

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        
        // Imposta il repository per caricare dati dal JSON
        viewModel.setRepository(requireContext())

        setupUI(view)
        observeViewModel(view)

        val fabNotifications = view.findViewById<FloatingActionButton>(R.id.fabNotifications)
        fabNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationsFragment)
        }
        
        // Bottone refresh per cambiare i rifugi casuali
        val refreshButton = view.findViewById<ImageView>(R.id.refreshButton)
        refreshButton.setOnClickListener {
            viewModel.refreshRandomData()
            Toast.makeText(requireContext(), "Rifugi aggiornati!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI(view: View) {
        val tabPerTe = view.findViewById<TextView>(R.id.tab_per_te)
        val tabAmici = view.findViewById<TextView>(R.id.tab_amici)

        tabPerTe.setOnClickListener {
            viewModel.setActiveTab("rifugi")
        }
        tabAmici.setOnClickListener {
            viewModel.setActiveTab("amici")
        }

        // Inizializza con il tab rifugi attivo
        viewModel.setActiveTab("rifugi")
    }

    private fun observeViewModel(view: View) {
        // Osserva il tab attivo
        viewModel.currentTab.observe(viewLifecycleOwner) { activeTab ->
            highlightActiveButton(activeTab)
        }

        // Osserva l'escursione programmata
        viewModel.escursioneProgrammata.observe(viewLifecycleOwner) { escursione ->
            setupEscursioneProgrammata(view, escursione)
        }

        // Osserva il punteggio
        viewModel.punteggio.observe(viewLifecycleOwner) { punteggio ->
            setupPunteggio(view, punteggio)
        }

        // Osserva i rifugi bonus
        viewModel.rifugiBonus.observe(viewLifecycleOwner) { rifugiBonus ->
            setupRifugiBonus(view, rifugiBonus)
        }

        // Osserva i suggerimenti personalizzati
        viewModel.suggerimentiPersonalizzati.observe(viewLifecycleOwner) { suggerimenti ->
            setupSuggerimenti(view, suggerimenti)
        }

        // Osserva il feed amici
        viewModel.feedAmici.observe(viewLifecycleOwner) { feedAmici ->
            setupFeedAmici(view, feedAmici)
        }

        // Osserva gli errori
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                // Mostra errore (puoi usare Toast, Snackbar, etc.)
                // Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Osserva lo stato di loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Mostra/nascondi loading indicator se necessario
        }
    }

    private fun setupEscursioneProgrammata(view: View, escursione: HomeViewModel.Escursione) {
        val backgroundImageView = view.findViewById<ImageView>(R.id.backgroundImage)
        val cardEscursione = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardEscursione)

        // Carica l'immagine dall'URL se disponibile
        if (!escursione.immagine.isNullOrEmpty() && escursione.immagine.startsWith("http")) {
            // Carica immagine dall'URL usando Glide
            Glide.with(requireContext())
                .load(escursione.immagine)
                .placeholder(R.drawable.mountain_background)
                .error(R.drawable.mountain_background)
                .centerCrop()
                .into(backgroundImageView)
        } else {
            // Fallback per immagini locali
            val nomeRisorsa = viewModel.getImageResourceName(escursione.nome)
            val resId = resources.getIdentifier(nomeRisorsa, "drawable", requireContext().packageName)

            if (resId != 0) {
                backgroundImageView.setImageResource(resId)
            } else {
                // Immagine di fallback se non esiste
                backgroundImageView.setImageResource(R.drawable.mountain_background)
            }
        }

        // UI dinamico - escursione
        view.findViewById<TextView>(R.id.textNomeRifugio).text = escursione.nome
        view.findViewById<TextView>(R.id.textAltitudine).text = escursione.altitudine
        view.findViewById<TextView>(R.id.textDistanza).text = escursione.distanza

        // Click listener sull'intera card
        cardEscursione.setOnClickListener {
            navigateToRifugioDetail(escursione)
        }
    }

    // Metodo per navigare ai dettagli del rifugio
    private fun navigateToRifugioDetail(escursione: HomeViewModel.Escursione) {
        val bundle = Bundle().apply {
            putInt("rifugioId", escursione.id?.toIntOrNull() ?: 1) // Passa l'ID del rifugio
            putString("RIFUGIO_NOME", escursione.nome)
            putString("RIFUGIO_ALTITUDINE", escursione.altitudine)
            putString("RIFUGIO_DISTANZA", escursione.distanza)
            putString("RIFUGIO_LOCALITA", escursione.localita)
            putString("RIFUGIO_COORDINATE", escursione.coordinate)
            putString("RIFUGIO_DIFFICOLTA", escursione.difficolta)
            putString("RIFUGIO_TEMPO", escursione.tempo)
            putString("RIFUGIO_DESCRIZIONE", escursione.descrizione)

            // Passa anche la lista dei servizi come stringa
            if (escursione.servizi.isNotEmpty()) {
                putString("RIFUGIO_SERVIZI", escursione.servizi.joinToString(","))
            }
        }

        findNavController().navigate(R.id.action_homeFragment_to_cabinFragment, bundle)
    }

    private fun setupPunteggio(view: View, punteggio: Int) {
        val progress = view.findViewById<ProgressBar>(R.id.progressScore)
        val textScoreOverlay = view.findViewById<TextView>(R.id.textScoreOverlay)

        progress.progress = punteggio
        textScoreOverlay.text = "$punteggio%"
    }

    private fun setupRifugiBonus(view: View, rifugiBonus: List<HomeViewModel.RifugioCard>) {
        val recyclerRifugiBonus = view.findViewById<RecyclerView>(R.id.recyclerRifugiBonus)
        recyclerRifugiBonus.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val adapterRifugiBonus = RifugiHorizontalAdapter(
            rifugiBonus,
            true, // mostra badge bonus
            onRifugioClick = { rifugioCard ->
                navigateToRifugioFromCard(rifugioCard)
            }
        )
        recyclerRifugiBonus.adapter = adapterRifugiBonus
    }

    private fun setupSuggerimenti(view: View, suggerimenti: List<HomeViewModel.RifugioCard>) {
        val recyclerSuggerimenti = view.findViewById<RecyclerView>(R.id.recyclerSuggerimenti)
        recyclerSuggerimenti.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val adapterSuggerimenti = RifugiHorizontalAdapter(
            suggerimenti,
            false, // nascondi badge bonus
            onRifugioClick = { rifugioCard ->
                navigateToRifugioFromCard(rifugioCard)
            }
        )
        recyclerSuggerimenti.adapter = adapterSuggerimenti
    }

    // Metodo per navigare dai rifugi delle card orizzontali
    private fun navigateToRifugioFromCard(rifugioCard: HomeViewModel.RifugioCard) {
        // Usa lifecycleScope per chiamare la funzione suspend
        lifecycleScope.launch {
            try {
                // Trova il rifugio nel repository per ottenere l'ID
                val rifugio = viewModel.findRifugioByName(rifugioCard.nome)
                
                val bundle = Bundle().apply {
                    putInt("rifugioId", rifugio?.id ?: 1) // Passa l'ID del rifugio
                    putString("RIFUGIO_NOME", rifugioCard.nome)
                    putString("RIFUGIO_ALTITUDINE", rifugioCard.altitudine)
                    putString("RIFUGIO_DISTANZA", rifugioCard.distanza)
                    putString("RIFUGIO_DIFFICOLTA", rifugioCard.difficolta)
                    putString("RIFUGIO_TEMPO", rifugioCard.tempo)

                    // Per i dati mancanti, usa valori di default
                    putString("RIFUGIO_LOCALITA", rifugio?.localita ?: "Località da specificare")
                    putString("RIFUGIO_COORDINATE", "${rifugio?.latitudine ?: 0.0},${rifugio?.longitudine ?: 0.0}")
                    putString("RIFUGIO_DESCRIZIONE", rifugio?.descrizione ?: "Descrizione non disponibile")
                }

                findNavController().navigate(R.id.action_homeFragment_to_cabinFragment, bundle)
            } catch (e: Exception) {
                // Fallback se c'è un errore
                val bundle = Bundle().apply {
                    putInt("rifugioId", 1)
                    putString("RIFUGIO_NOME", rifugioCard.nome)
                    putString("RIFUGIO_ALTITUDINE", rifugioCard.altitudine)
                    putString("RIFUGIO_DISTANZA", rifugioCard.distanza)
                    putString("RIFUGIO_DIFFICOLTA", rifugioCard.difficolta)
                    putString("RIFUGIO_TEMPO", rifugioCard.tempo)
                    putString("RIFUGIO_LOCALITA", "Località da specificare")
                    putString("RIFUGIO_COORDINATE", "0.0,0.0")
                    putString("RIFUGIO_DESCRIZIONE", "Descrizione non disponibile")
                }
                findNavController().navigate(R.id.action_homeFragment_to_cabinFragment, bundle)
            }
        }
    }


    private fun setupFeedAmici(view: View, feedAmici: List<HomeViewModel.FeedAmico>) {
        val recyclerFeed = view.findViewById<RecyclerView>(R.id.recyclerFeedAmici)
        recyclerFeed.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        val adapter = FeedAmiciAdapter(feedAmici) { rifugioInfo ->
            val bundle = Bundle().apply {
                putString("RIFUGIO_NOME", rifugioInfo.nome)
                putString("RIFUGIO_ALTITUDINE", "${rifugioInfo.altitudine} m")
                putString("RIFUGIO_LOCALITA", rifugioInfo.localita)

                // Valori di default per parametri mancanti
                putString("RIFUGIO_DISTANZA", "N/A")
                putString("RIFUGIO_COORDINATE", "0.0000,0.0000")
                putString("RIFUGIO_DIFFICOLTA", "Non specificata")
                putString("RIFUGIO_TEMPO", "Non specificato")
                putString("RIFUGIO_DESCRIZIONE", "Rifugio visitato da ${feedAmici.find { it.rifugioInfo == rifugioInfo }?.nomeUtente ?: "un amico"}")

                //rifugioInfo.immagine?.let { putExtra("RIFUGIO_IMMAGINE", it) }
            }

            findNavController().navigate(R.id.action_homeFragment_to_cabinFragment, bundle)
        }

        recyclerFeed.adapter = adapter
    }

    private fun highlightActiveButton(activeButton: String) {
        val tabPerTe = view?.findViewById<TextView>(R.id.tab_per_te)
        val tabAmici = view?.findViewById<TextView>(R.id.tab_amici)
        val rifugiContent = view?.findViewById<LinearLayout>(R.id.rifugiContent)
        val amiciContent = view?.findViewById<LinearLayout>(R.id.amicicontent)

        when (activeButton) {
            "rifugi" -> {
                // attivo
                tabPerTe?.paintFlags = (tabPerTe?.paintFlags ?: 0) or Paint.UNDERLINE_TEXT_FLAG
                // non attivo
                tabAmici?.paintFlags = (tabAmici?.paintFlags ?: 0) and Paint.UNDERLINE_TEXT_FLAG.inv()
                tabPerTe?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_black))
                tabPerTe?.setTypeface(null, android.graphics.Typeface.BOLD)
                tabPerTe?.alpha = 1.0f
                tabPerTe?.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.setDuration(200)?.start()

                tabAmici?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                tabAmici?.setTypeface(null, android.graphics.Typeface.NORMAL)
                tabAmici?.alpha = 0.7f
                tabAmici?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(200)?.start()

                rifugiContent?.visibility = View.VISIBLE
                amiciContent?.visibility = View.GONE
            }

            "amici" -> {
                // attivo
                tabAmici?.paintFlags = (tabAmici?.paintFlags ?: 0) or Paint.UNDERLINE_TEXT_FLAG
                // non attivo
                tabPerTe?.paintFlags = (tabPerTe?.paintFlags ?: 0) and Paint.UNDERLINE_TEXT_FLAG.inv()
                tabAmici?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_black))
                tabAmici?.setTypeface(null, android.graphics.Typeface.BOLD)
                tabAmici?.alpha = 1.0f
                tabAmici?.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.setDuration(200)?.start()

                tabPerTe?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                tabPerTe?.setTypeface(null, android.graphics.Typeface.NORMAL)
                tabPerTe?.alpha = 0.7f
                tabPerTe?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(200)?.start()

                rifugiContent?.visibility = View.GONE
                amiciContent?.visibility = View.VISIBLE
            }
        }
    }
}