package com.example.mountainpassport_girarifugi.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mountainpassport_girarifugi.R

class MapFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    // Questa funzione si esegue dopo che il layout è pronto
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // QUI scrivi cosa deve fare la tua schermata
        setupMap()           // Prepara la mappa
        loadRifugi()         // Carica i rifugi
        //setupClickListeners() // Prepara i click sui bottoni
    }

    private fun setupMap() {
        // Codice per mostrare la mappa
        //binding.textView.text = "Questa è la mappa!"
    }

    private fun loadRifugi() {
        // Codice per caricare i rifugi dal database
    }
}
