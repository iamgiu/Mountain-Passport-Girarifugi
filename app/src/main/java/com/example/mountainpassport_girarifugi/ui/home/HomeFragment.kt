package com.example.mountainpassport_girarifugi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.graphics.Paint
import android.widget.TextView
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
import com.google.android.material.button.MaterialButton
import com.example.mountainpassport_girarifugi.ui.map.RifugioSavedEventBus

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
        setupEventBusObserver() // AGGIUNTO: Observer per eventi di salvataggio rifugi

        val fabNotifications = view.findViewById<FloatingActionButton>(R.id.fabNotifications)
        fabNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationsFragment)
        }

        val fabScan = view.findViewById<MaterialButton>(R.id.scanButton)
        fabScan.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_scanFragment)
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
            viewModel.refreshFeedAmici()
        }

        // Inizializza con il tab rifugi attivo
        viewModel.setActiveTab("rifugi")
    }

    /**
     * AGGIUNTO: Observer per l'evento di salvataggio rifugi
     */
    private fun setupEventBusObserver() {
        RifugioSavedEventBus.rifugioSavedEvent.observe(viewLifecycleOwner) {
            // Quando un rifugio viene salvato/rimosso, aggiorna la lista dei rifugi salvati
            viewModel.refreshRifugiSalvati()
        }
    }

    private fun observeViewModel(view: View) {
        // Osserva il tab attivo
        viewModel.currentTab.observe(viewLifecycleOwner) { activeTab ->
            highlightActiveButton(activeTab)
        }

        // Osserva il punteggio
        viewModel.punteggio.observe(viewLifecycleOwner) { punteggio ->
            setupPunteggio(view, punteggio)
        }

        viewModel.rifugiSalvati.observe(viewLifecycleOwner) { rifugiSalvati ->
            setupRifugiSalvati(view, rifugiSalvati)
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

    private fun setupRifugiSalvati(view: View, rifugiSalvati: List<HomeViewModel.RifugioCard>) {
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerRifugiSalvati)
        val emptyMessage = view.findViewById<TextView>(R.id.emptyRifugiSalvatiTextView)

        if (rifugiSalvati.isEmpty()) {
            // Mostra il messaggio quando non ci sono rifugi salvati
            recycler.visibility = View.GONE
            emptyMessage.visibility = View.VISIBLE
            emptyMessage.text = "Inizia ad esplorare!"
        } else {
            // Mostra la lista quando ci sono rifugi salvati
            recycler.visibility = View.VISIBLE
            emptyMessage.visibility = View.GONE

            recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            val adapter = RifugiSalvatiAdapter(rifugiSalvati) { rifugioCard ->
                navigateToRifugioFromCard(rifugioCard)
            }
            recycler.adapter = adapter
        }
    }

    // Metodo per navigare dai rifugi delle card orizzontali - AGGIORNATO
    private fun navigateToRifugioFromCard(rifugioCard: HomeViewModel.RifugioCard) {
        lifecycleScope.launch {
            try {
                val rifugio = viewModel.findRifugioByName(rifugioCard.nome)

                val bundle = Bundle().apply {
                    if (rifugio != null) {
                        // OPZIONE 1: Se trovato nel JSON, passa l'ID (navigazione moderna)
                        putInt("rifugioId", rifugio.id)
                    } else {
                        // OPZIONE 2: Se non trovato nel JSON, passa tutti i dati (navigazione legacy)
                        putString("RIFUGIO_NOME", rifugioCard.nome)
                        putString("RIFUGIO_ALTITUDINE", rifugioCard.altitudine)
                        putString("RIFUGIO_DISTANZA", rifugioCard.distanza)
                        putString("RIFUGIO_LOCALITA", "Località da specificare")
                        putString("RIFUGIO_COORDINATE", "0.0000,0.0000")
                        putString("RIFUGIO_DIFFICOLTA", rifugioCard.difficolta)
                        putString("RIFUGIO_TEMPO", rifugioCard.tempo)
                        putString("RIFUGIO_DESCRIZIONE", "Descrizione non disponibile")
                    }
                }

                findNavController().navigate(R.id.action_homeFragment_to_cabinFragment, bundle)

            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Errore navigazione: ${e.message}")

                // Fallback: passa sempre i dati della card
                val bundle = Bundle().apply {
                    putString("RIFUGIO_NOME", rifugioCard.nome)
                    putString("RIFUGIO_ALTITUDINE", rifugioCard.altitudine)
                    putString("RIFUGIO_DISTANZA", rifugioCard.distanza)
                    putString("RIFUGIO_LOCALITA", "Località da specificare")
                    putString("RIFUGIO_COORDINATE", "0.0000,0.0000")
                    putString("RIFUGIO_DIFFICOLTA", rifugioCard.difficolta)
                    putString("RIFUGIO_TEMPO", rifugioCard.tempo)
                    putString("RIFUGIO_DESCRIZIONE", "Descrizione non disponibile")
                }
                findNavController().navigate(R.id.action_homeFragment_to_cabinFragment, bundle)
            }
        }
    }

    // Metodo per navigare dai feed amici - AGGIORNATO
    private fun setupFeedAmici(view: View, feedAmici: List<HomeViewModel.FeedAmico>) {
        val recyclerFeed = view.findViewById<RecyclerView>(R.id.recyclerFeedAmici)
        val emptyFeedLayout = view.findViewById<View>(R.id.emptyFeedLayout)

        if (feedAmici.isEmpty()) {
            recyclerFeed.visibility = View.GONE
            emptyFeedLayout.visibility = View.VISIBLE
        } else {
            recyclerFeed.visibility = View.VISIBLE
            emptyFeedLayout.visibility = View.GONE

            recyclerFeed.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            val adapter = FeedAmiciAdapter(feedAmici) { rifugioInfo ->
                // AGGIORNATO: Cerca il rifugio nel database e usa l'ID
                lifecycleScope.launch {
                    try {
                        val rifugio = viewModel.findRifugioByName(rifugioInfo.nome)

                        if (rifugio != null) {
                            // Usa l'ID del rifugio trovato
                            val bundle = Bundle().apply {
                                putInt("rifugioId", rifugio.id)
                            }
                            findNavController().navigate(R.id.action_homeFragment_to_cabinFragment, bundle)
                        } else {
                            // Fallback: crea un messaggio di errore
                            Toast.makeText(
                                requireContext(),
                                "Rifugio non trovato: ${rifugioInfo.nome}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Errore nell'apertura del rifugio",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            recyclerFeed.adapter = adapter
        }
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