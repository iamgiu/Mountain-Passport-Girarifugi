package com.example.mountainpassport_girarifugi.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Paint
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NotificationsFragment : Fragment() {

    private lateinit var viewModel: NotificationsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[NotificationsViewModel::class.java]

        setupUI(view)
        observeViewModel(view)

        // Setup settings FAB
        setupForwardButton(view)

    }

    private fun setupUI(view: View) {
        val filterTutte = view.findViewById<TextView>(R.id.filter_tutte)
        val filterRifugi = view.findViewById<TextView>(R.id.filter_rifugi)
        val filterAmici = view.findViewById<TextView>(R.id.filter_amici)
        val btnImpostazioni = view.findViewById<MaterialButton>(R.id.btnImpostazioni)

        // Setup filtri
        filterTutte.setOnClickListener {
            viewModel.setActiveFilter("tutte")
        }
        filterRifugi.setOnClickListener {
            viewModel.setActiveFilter("rifugi")
        }
        filterAmici.setOnClickListener {
            viewModel.setActiveFilter("amici")
        }

        // Setup pulsante impostazioni
        btnImpostazioni.setOnClickListener {
            viewModel.onImpostazioniClicked()
        }

        // Inizializza con il filtro "tutte" attivo
        viewModel.setActiveFilter("tutte")
    }

    private fun setupForwardButton(view: View) {
        val fabForward = view.findViewById<FloatingActionButton>(R.id.fabForwardHome)
        fabForward.setOnClickListener {
            findNavController().navigate(R.id.action_notificationsFragment_to_homeFragment)
        }
    }

    private fun observeViewModel(view: View) {
        // Osserva il filtro attivo
        viewModel.currentFilter.observe(viewLifecycleOwner) { activeFilter ->
            highlightActiveFilter(view, activeFilter)
        }

        // Osserva le notifiche recenti
        viewModel.notificheRecenti.observe(viewLifecycleOwner) { notifiche ->
            setupNotificheRecenti(view, notifiche)
            toggleEmptyState(view, notifiche.isEmpty() && (viewModel.notifichePrecedenti.value?.isEmpty() == true))
        }

        // Osserva le notifiche precedenti
        viewModel.notifichePrecedenti.observe(viewLifecycleOwner) { notifiche ->
            setupNotifichePrecedenti(view, notifiche)
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

        // Osserva eventi di navigazione
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.let {
                handleNavigationEvent(it)
                viewModel.clearNavigationEvent()
            }
        }
    }

    private fun setupNotificheRecenti(view: View, notifiche: List<NotificationsViewModel.Notifica>) {
        val recyclerRecent = view.findViewById<RecyclerView>(R.id.recyclerNotificationsRecent)
        recyclerRecent.layoutManager = LinearLayoutManager(context)

        val adapter = NotificationAdapter(notifiche) { notifica ->
            viewModel.onNotificaClicked(notifica)
        }
        recyclerRecent.adapter = adapter
    }

    private fun setupNotifichePrecedenti(view: View, notifiche: List<NotificationsViewModel.Notifica>) {
        val recyclerPrevious = view.findViewById<RecyclerView>(R.id.recyclerNotificationsPrevious)
        recyclerPrevious.layoutManager = LinearLayoutManager(context)

        val adapter = NotificationAdapter(notifiche) { notifica ->
            viewModel.onNotificaClicked(notifica)
        }
        recyclerPrevious.adapter = adapter
    }

    private fun highlightActiveFilter(view: View, activeFilter: String) {
        val filterTutte = view.findViewById<TextView>(R.id.filter_tutte)
        val filterRifugi = view.findViewById<TextView>(R.id.filter_rifugi)
        val filterAmici = view.findViewById<TextView>(R.id.filter_amici)

        // Reset tutti i filtri
        listOf(filterTutte, filterRifugi, filterAmici).forEach { filter ->
            filter?.paintFlags = (filter?.paintFlags ?: 0) and Paint.UNDERLINE_TEXT_FLAG.inv()
            filter?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            filter?.setTypeface(null, android.graphics.Typeface.NORMAL)
            filter?.alpha = 0.7f
            filter?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(200)?.start()
        }

        // Evidenzia il filtro attivo
        val activeView = when (activeFilter) {
            "tutte" -> filterTutte
            "rifugi" -> filterRifugi
            "amici" -> filterAmici
            else -> filterTutte
        }

        activeView?.apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setTextColor(ContextCompat.getColor(requireContext(), R.color.green_black))
            setTypeface(null, android.graphics.Typeface.BOLD)
            alpha = 1.0f
            animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start()
        }
    }

    private fun toggleEmptyState(view: View, isEmpty: Boolean) {
        val emptyStateLayout = view.findViewById<LinearLayout>(R.id.emptyStateLayout)
        val scrollView = view.findViewById<View>(R.id.scrollViewNotifications)

        if (isEmpty) {
            emptyStateLayout?.visibility = View.VISIBLE
            scrollView?.visibility = View.GONE
        } else {
            emptyStateLayout?.visibility = View.GONE
            scrollView?.visibility = View.VISIBLE
        }
    }

    private fun handleNavigationEvent(event: NotificationsViewModel.NavigationEvent) {
        when (event) {
            is NotificationsViewModel.NavigationEvent.OpenSettings -> {
                // Naviga alle impostazioni notifiche
                // findNavController().navigate(R.id.action_notifications_to_settings)
            }
            is NotificationsViewModel.NavigationEvent.OpenNotificationDetail -> {
                // Naviga al dettaglio della notifica
                // findNavController().navigate(
                //     R.id.action_notifications_to_detail,
                //     bundleOf("notifica_id" to event.notificaId)
                // )
            }
            is NotificationsViewModel.NavigationEvent.OpenRifugioDetail -> {
                // Naviga al dettaglio del rifugio
                // findNavController().navigate(
                //     R.id.action_notifications_to_rifugio_detail,
                //     bundleOf("rifugio_id" to event.rifugioId)
                // )
            }
            is NotificationsViewModel.NavigationEvent.OpenUserProfile -> {
                // Naviga al profilo dell'utente
                // findNavController().navigate(
                //     R.id.action_notifications_to_user_profile,
                //     bundleOf("user_id" to event.userId)
                // )
            }
        }
    }
}