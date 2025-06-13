package com.example.mountainpassport_girarifugi.ui.map

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.data.model.Rifugio
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow

class CustomInfoWindow(
    mapView: MapView,
    private val rifugio: Rifugio,
    private val onDettagliClick: (Rifugio) -> Unit
) : InfoWindow(R.layout.custom_info_window, mapView) {

    override fun onOpen(item: Any?) {
        val view = mView

        // Trova le view
        val imageRifugio = view.findViewById<ImageView>(R.id.imageRifugio)
        val textNome = view.findViewById<TextView>(R.id.textNome)
        val textLocalita = view.findViewById<TextView>(R.id.textLocalita)
        val textAltitudine = view.findViewById<TextView>(R.id.textAltitudine)
        val buttonDettagli = view.findViewById<Button>(R.id.buttonDettagli)

        // Imposta i dati
        textNome.text = rifugio.nome
        textLocalita.text = rifugio.localita
        textAltitudine.text = "${rifugio.altitudine} m s.l.m."

        // Per ora usa un'immagine placeholder
        imageRifugio.setImageResource(R.drawable.ic_cabin_24)

        // Gestisci i click sui bottoni
        buttonDettagli.setOnClickListener {
            onDettagliClick(rifugio)
        }
    }

    override fun onClose() {
        // Cleanup se necessario
    }
}