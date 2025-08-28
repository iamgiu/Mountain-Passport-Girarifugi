package com.example.mountainpassport_girarifugi.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mountainpassport_girarifugi.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CabinFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cabin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ricevi i dati dagli arguments invece che dall'intent
        val rifugioNome = arguments?.getString("RIFUGIO_NOME") ?: "Rifugio Sconosciuto"
        val rifugioAltitudine = arguments?.getString("RIFUGIO_ALTITUDINE") ?: "0 m"
        val rifugioDistanza = arguments?.getString("RIFUGIO_DISTANZA") ?: "0 km"
        val rifugioLocalita = arguments?.getString("RIFUGIO_LOCALITA") ?: "Località sconosciuta"
        val rifugioCoordinate = arguments?.getString("RIFUGIO_COORDINATE") ?: "0.0000,0.0000"
        val rifugioDifficolta = arguments?.getString("RIFUGIO_DIFFICOLTA") ?: "Non specificata"
        val rifugioTempo = arguments?.getString("RIFUGIO_TEMPO") ?: "Non specificato"
        val rifugioDescrizione = arguments?.getString("RIFUGIO_DESCRIZIONE") ?: "Nessuna descrizione disponibile"

        setupUI(view, rifugioNome, rifugioAltitudine, rifugioDistanza, rifugioLocalita,
            rifugioCoordinate, rifugioDifficolta, rifugioTempo, rifugioDescrizione)
        setupClickListeners(view)
    }

    private fun setupUI(view: View, nome: String, altitudine: String, distanza: String,
                        localita: String, coordinate: String, difficolta: String,
                        tempo: String, descrizione: String) {

        // Imposta il nome del rifugio
        view.findViewById<TextView>(R.id.cabinNameTextView).text = nome

        // Imposta l'altitudine
        view.findViewById<TextView>(R.id.altitudeTextView).text = altitudine

        // Imposta le coordinate (dividi latitudine e longitudine)
        val coords = coordinate.split(",")
        if (coords.size == 2) {
            view.findViewById<TextView>(R.id.coordinatesTextView).text = "${coords[0]}\n${coords[1]}"
        }

        // Imposta la località
        view.findViewById<TextView>(R.id.locationTextView).text = localita

        // Imposta i dati del percorso
        view.findViewById<TextView>(R.id.distanceTextView).text = distanza
        view.findViewById<TextView>(R.id.timeTextView).text = tempo
        view.findViewById<TextView>(R.id.difficultyTextView).text = "Difficoltà: $difficolta"

        // Imposta la descrizione del percorso
        view.findViewById<TextView>(R.id.routeDescriptionTextView).text = descrizione

        // Imposta l'immagine del rifugio
        setupRifugioImage(view, nome)
    }

    private fun setupRifugioImage(view: View, nomeRifugio: String) {
        val imageView = view.findViewById<ImageView>(R.id.cabinImageView)

        // Converti il nome in un nome di risorsa valido
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
            // Immagine di fallback
            imageView.setImageResource(R.drawable.rifugio_torino)
        }
    }

    private fun setupClickListeners(view: View) {
        // Back button - usa Navigation Component
        view.findViewById<FloatingActionButton>(R.id.fabBack).setOnClickListener {
            findNavController().navigateUp()
        }

        // Save button (se vuoi implementare il salvataggio)
        view.findViewById<FloatingActionButton>(R.id.fabSave).setOnClickListener {
            // TODO: Implementa logica di salvataggio rifugio
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