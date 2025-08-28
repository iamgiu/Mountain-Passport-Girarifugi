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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.mountainpassport_girarifugi.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.Intent
import com.example.mountainpassport_girarifugi.CabinActivity

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

        setupUI(view)
        observeViewModel(view)

        val fabNotifications = view.findViewById<FloatingActionButton>(R.id.fabNotifications)
        fabNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationsFragment)
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

        // Usa il metodo del ViewModel per ottenere il nome della risorsa
        val nomeRisorsa = viewModel.getImageResourceName(escursione.nome)
        val resId = resources.getIdentifier(nomeRisorsa, "drawable", requireContext().packageName)

        // Se trovata, la imposti come immagine
        if (resId != 0) {
            backgroundImageView.setImageResource(resId)
        } else {
            // Immagine di fallback se non esiste
            backgroundImageView.setImageResource(R.drawable.mountain_background)
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
        val intent = Intent(requireContext(), CabinActivity::class.java).apply {
            // Passa tutti i dati del rifugio all'activity
            putExtra("RIFUGIO_NOME", escursione.nome)
            putExtra("RIFUGIO_ALTITUDINE", escursione.altitudine)
            putExtra("RIFUGIO_DISTANZA", escursione.distanza)
            putExtra("RIFUGIO_LOCALITA", escursione.localita)
            putExtra("RIFUGIO_COORDINATE", escursione.coordinate)
            putExtra("RIFUGIO_DIFFICOLTA", escursione.difficolta)
            putExtra("RIFUGIO_TEMPO", escursione.tempo)
            putExtra("RIFUGIO_DESCRIZIONE", escursione.descrizione)
            putExtra("RIFUGIO_ID", escursione.id)

            // Passa anche la lista dei servizi come stringa
            if (escursione.servizi.isNotEmpty()) {
                putExtra("RIFUGIO_SERVIZI", escursione.servizi.joinToString(","))
            }
        }
        startActivity(intent)
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
        val intent = Intent(requireContext(), CabinActivity::class.java).apply {
            // Passa i dati disponibili dalla card
            putExtra("RIFUGIO_NOME", rifugioCard.nome)
            putExtra("RIFUGIO_ALTITUDINE", rifugioCard.altitudine)
            putExtra("RIFUGIO_DISTANZA", rifugioCard.distanza)
            putExtra("RIFUGIO_DIFFICOLTA", rifugioCard.difficolta)
            putExtra("RIFUGIO_TEMPO", rifugioCard.tempo)

            // Per i dati mancanti, puoi usare valori di default o recuperarli dal ViewModel
            putExtra("RIFUGIO_LOCALITA", "Località da specificare")
            putExtra("RIFUGIO_COORDINATE", "0.0000,0.0000")
            putExtra("RIFUGIO_DESCRIZIONE", "Descrizione non disponibile")
        }
        startActivity(intent)
    }

    private fun setupFeedAmici(view: View, feedAmici: List<HomeViewModel.FeedAmico>) {
        val recyclerFeed = view.findViewById<RecyclerView>(R.id.recyclerFeedAmici)
        recyclerFeed.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        val adapter = FeedAmiciAdapter(feedAmici) { rifugioInfo ->
            val intent = Intent(requireContext(), CabinActivity::class.java).apply {
                putExtra("RIFUGIO_NOME", rifugioInfo.nome)
                putExtra("RIFUGIO_ALTITUDINE", "${rifugioInfo.altitudine} m")
                putExtra("RIFUGIO_LOCALITA", rifugioInfo.localita)

                // Valori di default per parametri mancanti
                putExtra("RIFUGIO_DISTANZA", "N/A")
                putExtra("RIFUGIO_COORDINATE", "0.0000,0.0000")
                putExtra("RIFUGIO_DIFFICOLTA", "Non specificata")
                putExtra("RIFUGIO_TEMPO", "Non specificato")
                putExtra("RIFUGIO_DESCRIZIONE", "Rifugio visitato da ${feedAmici.find { it.rifugioInfo == rifugioInfo }?.nomeUtente ?: "un amico"}")

                // Dati extra
                rifugioInfo.immagine?.let { putExtra("RIFUGIO_IMMAGINE", it) }
            }
            startActivity(intent)
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