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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabPerTe = view.findViewById<TextView>(R.id.tab_per_te)
        val tabAmici = view.findViewById<TextView>(R.id.tab_amici)

        tabPerTe.setOnClickListener { highlightActiveButton("rifugi") }
        tabAmici.setOnClickListener { highlightActiveButton("amici") }

        highlightActiveButton("rifugi")

        // Setup escursione programmata
        setupEscursioneProgrammata(view)

        // Setup punteggio
        setupPunteggio(view)

        // Setup RecyclerViews orizzontali
        setupRecyclerViews(view)

        // Setup Feed Amici
        setupFeedAmici(view)
    }

    private fun setupEscursioneProgrammata(view: View) {
        val escursione = Escursione(
            nome = "Rifugio Monte Bianco",
            altitudine = "2100 m",
            distanza = "18 km"
        )

        val backgroundImageView = view.findViewById<ImageView>(R.id.backgroundImage)

        // Converti il nome del rifugio in nome file
        val nomeRisorsa = escursione.nome
            .lowercase()
            .replace(" ", "_")
            .replace("à", "a")  // opzionale, per evitare crash
            .replace("è", "e")  // ecc. se hai accenti

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
    }

    private fun setupPunteggio(view: View) {
        val punteggio = 82
        // UI dinamico - punteggio
        val progress = view.findViewById<ProgressBar>(R.id.progressScore)
        val textScoreOverlay = view.findViewById<TextView>(R.id.textScoreOverlay)

        progress.progress = punteggio
        textScoreOverlay.text = "$punteggio%"
    }

    private fun setupRecyclerViews(view: View) {
        // Setup RecyclerView rifugi bonus
        val recyclerRifugiBonus = view.findViewById<RecyclerView>(R.id.recyclerRifugiBonus)
        recyclerRifugiBonus.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val rifugiBonus = getRifugiBonus()
        val adapterRifugiBonus = RifugiHorizontalAdapter(rifugiBonus, true) // true per mostrare badge bonus
        recyclerRifugiBonus.adapter = adapterRifugiBonus

        // Setup RecyclerView suggerimenti
        val recyclerSuggerimenti = view.findViewById<RecyclerView>(R.id.recyclerSuggerimenti)
        recyclerSuggerimenti.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        val suggerimenti = getSuggerimentiPersonalizzati()
        val adapterSuggerimenti = RifugiHorizontalAdapter(suggerimenti, false) // false per nascondere badge bonus
        recyclerSuggerimenti.adapter = adapterSuggerimenti
    }

    private fun setupFeedAmici(view: View) {
        val recyclerFeed = view.findViewById<RecyclerView>(R.id.recyclerFeedAmici)
        recyclerFeed.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        val feed = listOf(
            FeedAmico("Mario Rossi", R.drawable.ic_account_circle_24, "ha visitato un rifugio", "2 ore fa"),
            FeedAmico("Lucia Bianchi", R.drawable.ic_account_circle_24, "ha guadagnato un achievement", "5 ore fa"),
            FeedAmico("Giovanni Verde", R.drawable.ic_account_circle_24, "ha visitato un rifugio", "1 giorno fa"),
            FeedAmico("Anna Blu", R.drawable.ic_account_circle_24, "ha lasciato una recensione", "1 giorno fa"),
            FeedAmico("Marco Neri", R.drawable.ic_account_circle_24, "ha completato 5 rifugi", "2 giorni fa"),
            FeedAmico("Sofia Rosa", R.drawable.ic_account_circle_24, "ha visitato un rifugio", "3 giorni fa"),
            FeedAmico("Luca Viola", R.drawable.ic_account_circle_24, "ha guadagnato 150 punti", "4 giorni fa")
        )

        val adapter = FeedAmiciAdapter(feed)
        recyclerFeed.adapter = adapter
    }

    private fun getRifugiBonus(): List<RifugioCard> {
        return listOf(
            RifugioCard(
                nome = "Rifugio Laghi Gemelli",
                distanza = "3.2 km",
                altitudine = "2134 m",
                difficolta = "Medio",
                tempo = "2h 15m",
                immagine = "rifugio_laghi_gemelli",
                bonusPunti = 75
            ),
            RifugioCard(
                nome = "Capanna Margherita",
                distanza = "4.8 km",
                altitudine = "4554 m",
                difficolta = "Difficile",
                tempo = "6h 30m",
                immagine = "capanna_margherita",
                bonusPunti = 150
            ),
            RifugioCard(
                nome = "Rifugio Città di Milano",
                distanza = "2.1 km",
                altitudine = "1890 m",
                difficolta = "Facile",
                tempo = "1h 45m",
                immagine = "rifugio_citta_di_milano",
                bonusPunti = 50
            ),
            RifugioCard(
                nome = "Rifugio Gnifetti",
                distanza = "5.5 km",
                altitudine = "3647 m",
                difficolta = "Difficile",
                tempo = "4h 20m",
                immagine = "rifugio_gnifetti",
                bonusPunti = 120
            )
        )
    }

    private fun getSuggerimentiPersonalizzati(): List<RifugioCard> {
        return listOf(
            RifugioCard(
                nome = "Rifugio Torino",
                distanza = "1.8 km",
                altitudine = "3375 m",
                difficolta = "Medio",
                tempo = "3h 10m",
                immagine = "rifugio_torino"
            ),
            RifugioCard(
                nome = "Rifugio Elisabetta",
                distanza = "2.7 km",
                altitudine = "2195 m",
                difficolta = "Facile",
                tempo = "1h 50m",
                immagine = "rifugio_elisabetta"
            ),
            RifugioCard(
                nome = "Rifugio Vittorio Sella",
                distanza = "3.9 km",
                altitudine = "2584 m",
                difficolta = "Medio",
                tempo = "2h 45m",
                immagine = "rifugio_vittorio_sella"
            ),
            RifugioCard(
                nome = "Capanna Regina Margherita",
                distanza = "6.2 km",
                altitudine = "4554 m",
                difficolta = "Molto Difficile",
                tempo = "7h 00m",
                immagine = "capanna_regina_margherita"
            )
        )
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

    data class Escursione(val nome: String, val altitudine: String, val distanza: String)

    data class RifugioCard(
        val nome: String,
        val distanza: String,
        val altitudine: String,
        val difficolta: String,
        val tempo: String,
        val immagine: String,
        val bonusPunti: Int? = null
    )

    data class FeedAmico(
        val nomeUtente: String,
        val avatar: Int,
        val testoAttivita: String,
        val tempo: String,
        val tipoAttivita: TipoAttivita = TipoAttivita.GENERIC,
        val rifugioInfo: RifugioInfo? = null
    )

    enum class TipoAttivita {
        RIFUGIO_VISITATO,
        ACHIEVEMENT,
        PUNTI_GUADAGNATI,
        RECENSIONE,
        GENERIC
    }

    data class RifugioInfo(
        val nome: String,
        val localita: String,
        val altitudine: String,
        val puntiGuadagnati: Int,
        val immagine: String? = null
    )
}