package com.example.mountainpassport_girarifugi.ui.scan

import PuntiBottomSheet
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mountainpassport_girarifugi.R

class ScanFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflata il layout del fragment
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fabPunti = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabPunti)
        fabPunti.setOnClickListener {
            // Creiamo e mostriamo il Bottom Sheet
            val bottomSheet = PuntiBottomSheet()
            bottomSheet.show(parentFragmentManager, "PuntiBottomSheet")
        }
    }


}