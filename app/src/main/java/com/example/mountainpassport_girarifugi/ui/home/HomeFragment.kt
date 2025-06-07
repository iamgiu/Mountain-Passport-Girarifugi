package com.example.mountainpassport_girarifugi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
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

        // Trova i pulsanti
        val btnRifugi = view.findViewById<MaterialCardView>(R.id.btnAttivitaRifugi)
        val btnAmici = view.findViewById<MaterialCardView>(R.id.btnAttivitaAmici)

        // Aggiungi click listeners
        btnRifugi.setOnClickListener {
            highlightActiveButton("rifugi")
        }

        btnAmici.setOnClickListener {
            highlightActiveButton("amici")
        }

        // Inizializza con "rifugi" come attivo di default
        highlightActiveButton("rifugi")
    }

    private fun highlightActiveButton(activeButton: String) {
        val btnRifugi = view?.findViewById<MaterialCardView>(R.id.btnAttivitaRifugi)
        val btnAmici = view?.findViewById<MaterialCardView>(R.id.btnAttivitaAmici)
        val rifugiContent = view?.findViewById<LinearLayout>(R.id.rifugiContent)
        val amiciContent = view?.findViewById<LinearLayout>(R.id.amiciContent)

        when (activeButton) {
            "rifugi" -> {
                // Pulsante Rifugi ATTIVO
                btnRifugi?.apply {
                    animate()
                        .scaleX(1.1f)      // Più grande
                        .scaleY(1.1f)
                        .translationZ(16f)  // Più alto
                        .setDuration(300)
                        .start()

                    cardElevation = 16f
                    alpha = 1.0f
                }

                // Pulsante Amici INATTIVO
                btnAmici?.apply {
                    animate()
                        .scaleX(0.95f)     // Più piccolo
                        .scaleY(0.95f)
                        .translationZ(2f)
                        .setDuration(300)
                        .start()

                    cardElevation = 4f
                    alpha = 0.6f       // Più trasparente
                }

                // Mostra contenuto rifugi, nascondi amici
                rifugiContent?.visibility = View.VISIBLE
                amiciContent?.visibility = View.GONE
            }

            "amici" -> {
                // Pulsante Amici ATTIVO
                btnAmici?.apply {
                    animate()
                        .scaleX(1.1f)      // Più grande
                        .scaleY(1.1f)
                        .translationZ(16f)  // Più alto
                        .setDuration(300)
                        .start()

                    cardElevation = 16f
                    alpha = 1.0f
                }

                // Pulsante Rifugi INATTIVO
                btnRifugi?.apply {
                    animate()
                        .scaleX(0.95f)     // Più piccolo
                        .scaleY(0.95f)
                        .translationZ(2f)
                        .setDuration(300)
                        .start()

                    cardElevation = 4f
                    alpha = 0.6f       // Più trasparente
                }

                // Mostra contenuto amici, nascondi rifugi
                rifugiContent?.visibility = View.GONE
                amiciContent?.visibility = View.VISIBLE
            }
        }
    }
}