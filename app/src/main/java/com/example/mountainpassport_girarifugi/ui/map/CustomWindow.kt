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

    /**
     * Finestra che si apre nella mappa con le informazioni basilari del rifugio
     */
    override fun onOpen(item: Any?) {
        val textNome = mView.findViewById<TextView>(R.id.textNome)
        val textLocalita = mView.findViewById<TextView>(R.id.textLocalita)
        val textAltitudine = mView.findViewById<TextView>(R.id.textAltitudine)
        val imageRifugio = mView.findViewById<ImageView>(R.id.imageRifugio)
        val buttonDettagli = mView.findViewById<Button>(R.id.buttonDettagli)
        val buttonClose = mView.findViewById<ImageButton>(R.id.buttonClose)

        textNome.text = rifugio.nome
        textLocalita.text = rifugio.localita
        textAltitudine.text = "${rifugio.altitudine} m s.l.m."

        if (!rifugio.immagineUrl.isNullOrEmpty()) {
            Glide.with(mView.context)
                .load(rifugio.immagineUrl)
                .placeholder(R.drawable.ic_cabin_24)
                .error(R.drawable.ic_cabin_24)
                .centerCrop()
                .into(imageRifugio)
        } else {
            imageRifugio.setImageResource(R.drawable.ic_cabin_24)
        }

        buttonDettagli.setOnClickListener {
            onDettagliClick(rifugio)
            close()
        }

        buttonClose.setOnClickListener {
            close() // Chiudi l'info window
        }
    }

    override fun onClose() {
        // Cleanup se necessario
    }
}