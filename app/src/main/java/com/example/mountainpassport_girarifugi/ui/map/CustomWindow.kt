package com.example.mountainpassport_girarifugi.ui.map

import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import com.bumptech.glide.Glide
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow

class CustomInfoWindow(
    mapView: MapView,
    private val rifugio: Rifugio,
    private val onDettagliClick: (Rifugio) -> Unit
) : InfoWindow(R.layout.custom_info_window, mapView) {

    override fun onOpen(item: Any?) {
        // Popola i dati del rifugio
        val textNome = mView.findViewById<TextView>(R.id.textNome)
        val textLocalita = mView.findViewById<TextView>(R.id.textLocalita)
        val textAltitudine = mView.findViewById<TextView>(R.id.textAltitudine)
        val imageRifugio = mView.findViewById<ImageView>(R.id.imageRifugio)
        val buttonDettagli = mView.findViewById<Button>(R.id.buttonDettagli)
        val buttonClose = mView.findViewById<ImageButton>(R.id.buttonClose)

        // Imposta i dati
        textNome.text = rifugio.nome
        textLocalita.text = rifugio.localita
        textAltitudine.text = "${rifugio.altitudine} m s.l.m."

        // Carica l'immagine del rifugio dall'URL se disponibile
        if (!rifugio.immagineUrl.isNullOrEmpty()) {
            // Carica l'immagine dall'URL usando Glide
            Glide.with(mView.context)
                .load(rifugio.immagineUrl)
                .placeholder(R.drawable.ic_cabin_24) // Immagine di fallback
                .error(R.drawable.ic_cabin_24) // Immagine in caso di errore
                .centerCrop()
                .into(imageRifugio)
        } else {
            // Se non c'è URL, usa l'immagine di default
            imageRifugio.setImageResource(R.drawable.ic_cabin_24)
        }

        // Gestisci il click sul pulsante dettagli
        buttonDettagli.setOnClickListener {
            onDettagliClick(rifugio)
            close() // Chiudi l'info window dopo aver navigato
        }

        // Gestisci il click sul pulsante di chiusura (X)
        buttonClose.setOnClickListener {
            close() // Chiudi l'info window
        }
    }

    override fun onClose() {
        // Cleanup se necessario
    }
}