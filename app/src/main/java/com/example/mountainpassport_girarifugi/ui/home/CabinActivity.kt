package com.example.mountainpassport_girarifugi

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CabinActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_cabin)

        supportActionBar?.hide()

        // Ricevi i dati passati dall'HomeFragment
        val rifugioNome = intent.getStringExtra("RIFUGIO_NOME") ?: "Rifugio Sconosciuto"
        val rifugioAltitudine = intent.getStringExtra("RIFUGIO_ALTITUDINE") ?: "0 m"
        val rifugioDistanza = intent.getStringExtra("RIFUGIO_DISTANZA") ?: "0 km"
        val rifugioLocalita = intent.getStringExtra("RIFUGIO_LOCALITA") ?: "Località sconosciuta"
        val rifugioCoordinate = intent.getStringExtra("RIFUGIO_COORDINATE") ?: "0.0000,0.0000"
        val rifugioDifficolta = intent.getStringExtra("RIFUGIO_DIFFICOLTA") ?: "Non specificata"
        val rifugioTempo = intent.getStringExtra("RIFUGIO_TEMPO") ?: "Non specificato"
        val rifugioDescrizione = intent.getStringExtra("RIFUGIO_DESCRIZIONE") ?: "Nessuna descrizione disponibile"

        setupUI(rifugioNome, rifugioAltitudine, rifugioDistanza, rifugioLocalita,
            rifugioCoordinate, rifugioDifficolta, rifugioTempo, rifugioDescrizione)
        setupClickListeners()
    }

    private fun setupUI(nome: String, altitudine: String, distanza: String,
                        localita: String, coordinate: String, difficolta: String,
                        tempo: String, descrizione: String) {

        // Imposta il nome del rifugio
        findViewById<TextView>(R.id.cabinNameTextView).text = nome

        // Imposta l'altitudine
        findViewById<TextView>(R.id.altitudeTextView).text = altitudine

        // Imposta le coordinate (dividi latitudine e longitudine)
        val coords = coordinate.split(",")
        if (coords.size == 2) {
            findViewById<TextView>(R.id.coordinatesTextView).text = "${coords[0]}\n${coords[1]}"
        }

        // Imposta la località
        findViewById<TextView>(R.id.locationTextView).text = localita

        // Imposta i dati del percorso
        findViewById<TextView>(R.id.distanceTextView).text = distanza
        findViewById<TextView>(R.id.timeTextView).text = tempo
        findViewById<TextView>(R.id.difficultyTextView).text = "Difficoltà: $difficolta"

        // Imposta la descrizione del percorso
        findViewById<TextView>(R.id.routeDescriptionTextView).text = descrizione

        // Imposta l'immagine del rifugio
        setupRifugioImage(nome)
    }

    private fun setupRifugioImage(nomeRifugio: String) {
        val imageView = findViewById<ImageView>(R.id.cabinImageView)

        // Converti il nome in un nome di risorsa valido
        val nomeRisorsa = nomeRifugio
            .lowercase()
            .replace(" ", "_")
            .replace("à", "a")
            .replace("è", "e")
            .replace("ì", "i")
            .replace("ò", "o")
            .replace("ù", "u")

        val resId = resources.getIdentifier(nomeRisorsa, "drawable", packageName)

        if (resId != 0) {
            imageView.setImageResource(resId)
        } else {
            // Immagine di fallback
            imageView.setImageResource(R.drawable.rifugio_torino)
        }
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<FloatingActionButton>(R.id.fabBack).setOnClickListener {
            finish()
        }

        // Save button (se vuoi implementare il salvataggio)
        findViewById<FloatingActionButton>(R.id.fabSave).setOnClickListener {
            // TODO: Implementa logica di salvataggio rifugio
        }
    }
}