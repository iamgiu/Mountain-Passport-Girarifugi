package com.example.mountainpassport_girarifugi.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mountainpassport_girarifugi.R
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

        viewModel = ViewModelProvider(this)[NotificationsViewModel::class.java]

        observeViewModel(view)

        setupForwardButton(view)
    }

    /**
     *
     * Pulsante per tornare indietro alla home
     *
     */
    private fun setupForwardButton(view: View) {
        val fabForward = view.findViewById<FloatingActionButton>(R.id.fabForwardHome)
        fabForward.setOnClickListener {
            findNavController().navigate(R.id.action_notificationsFragment_to_homeFragment)
        }
    }

    /**
     *
     * Osserva i LiveData del ViewModel
     *
     */
    private fun observeViewModel(view: View) {
        viewModel.notificheRecenti.observe(viewLifecycleOwner) { notifiche ->
            setupNotificheRecenti(view, notifiche)
            toggleEmptyState(
                view,
                notifiche.isEmpty() && (viewModel.notifichePrecedenti.value?.isEmpty() == true)
            )
        }

        viewModel.notifichePrecedenti.observe(viewLifecycleOwner) { notifiche ->
            setupNotifichePrecedenti(view, notifiche)
        }
    }

    /**
     *
     * Setup RecyclerView per notifiche recenti
     *
     */
    private fun setupNotificheRecenti(view: View, notifiche: List<NotificationsViewModel.Notifica>) {
        val recyclerRecent = view.findViewById<RecyclerView>(R.id.recyclerNotificationsRecent)
        recyclerRecent.layoutManager = LinearLayoutManager(context)

        val adapter = NotificationAdapter(
            notifiche = notifiche,
            onNotificaClick = { notifica ->
                viewModel.onNotificaClicked(notifica)
            },
            onAcceptFriendRequest = { userId, notificationId ->
                viewModel.accettaRichiestaAmicizia(userId, notificationId)
            },
            onDeclineFriendRequest = { userId, notificationId ->
                viewModel.rifiutaRichiestaAmicizia(userId, notificationId)
            }
        )
        recyclerRecent.adapter = adapter
    }

    /**
     *
     * Setup RecyclerView per notifiche precedenti
     *
     */
    private fun setupNotifichePrecedenti(view: View, notifiche: List<NotificationsViewModel.Notifica>) {
        val recyclerPrevious = view.findViewById<RecyclerView>(R.id.recyclerNotificationsPrevious)
        recyclerPrevious.layoutManager = LinearLayoutManager(context)

        val adapter = NotificationAdapter(
            notifiche = notifiche,
            onNotificaClick = { notifica ->
                viewModel.onNotificaClicked(notifica)
            },
            onAcceptFriendRequest = { userId, notificationId ->
                viewModel.accettaRichiestaAmicizia(userId, notificationId)
            },
            onDeclineFriendRequest = { userId, notificationId ->
                viewModel.rifiutaRichiestaAmicizia(userId, notificationId)
            }
        )
        recyclerPrevious.adapter = adapter
    }

    /**
     *
     * Mostra un layout vuoto se non ci sono notifiche altrimenti mostra la lista delle notifiche
     *
     */
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
}
