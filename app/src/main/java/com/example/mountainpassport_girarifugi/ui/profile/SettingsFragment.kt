package com.example.mountainpassport_girarifugi.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.mountainpassport_girarifugi.SignInActivity
import com.example.mountainpassport_girarifugi.databinding.FragmentSettingsBinding
import com.example.mountainpassport_girarifugi.model.User

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        binding.salvaButton.setOnClickListener {
            val nome = binding.nomeEt.text.toString()
            val cognome = binding.cognomeEt.text.toString()
            val nickname = binding.nicknameEt.text.toString()

            viewModel.validateAndSaveProfile(nome, cognome, nickname)
        }

        binding.annullaButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        binding.resetPasswordButton.setOnClickListener {
            showResetPasswordConfirmationDialog()
        }
    }

    private fun setupObservers() {
        // Observer per l'utente corrente
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            populateFields(user)
        }

        // Observer per lo stato di caricamento
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.salvaButton.isEnabled = !isLoading
            binding.annullaButton.isEnabled = !isLoading
            binding.logoutButton.isEnabled = !isLoading
            binding.resetPasswordButton.isEnabled = !isLoading

            // Puoi aggiungere un progress bar se necessario
        }

        // Observer per gli errori di validazione
        viewModel.validationErrors.observe(viewLifecycleOwner) { errors ->
            binding.nomeLayout.error = errors.nomeError
            binding.cognomeLayout.error = errors.cognomeError
            binding.nicknameLayout.error = errors.nicknameError
        }

        // Observer per i messaggi di errore
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.onErrorMessageHandled()
            }
        }

        // Observer per il successo del salvataggio
        viewModel.saveSuccess.observe(viewLifecycleOwner) { saveSuccess ->
            if (saveSuccess) {
                Toast.makeText(requireContext(), "Profilo aggiornato con successo!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                viewModel.onSaveSuccessHandled()
            }
        }

        // Observer per l'evento di logout
        viewModel.logoutEvent.observe(viewLifecycleOwner) { shouldLogout ->
            if (shouldLogout) {
                navigateToSignIn()
                viewModel.onLogoutEventHandled()
            }
        }

        // Observer per il successo del reset password
        viewModel.resetPasswordSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(
                    requireContext(),
                    "Email di reset password inviata! Controlla la tua casella di posta.",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.onResetPasswordSuccessHandled()
            }
        }

        // Observer per gli errori del reset password
        viewModel.resetPasswordError.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.onResetPasswordErrorHandled()
            }
        }
    }

    private fun populateFields(user: User) {
        binding.nomeEt.setText(user.nome)
        binding.cognomeEt.setText(user.cognome)
        binding.nicknameEt.setText(user.nickname)
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Sei sicuro di voler uscire?")
            .setPositiveButton("Sì") { _, _ ->
                viewModel.performLogout()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun showResetPasswordConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Password")
            .setMessage("Vuoi ricevere un'email per reimpostare la password?")
            .setPositiveButton("Sì") { _, _ ->
                viewModel.sendPasswordResetEmail()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun navigateToSignIn() {
        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}