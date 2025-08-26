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
import com.example.mountainpassport_girarifugi.R
import com.example.mountainpassport_girarifugi.user.SignInActivity
import com.example.mountainpassport_girarifugi.databinding.FragmentSettingsBinding
import com.example.mountainpassport_girarifugi.user.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.os.Build
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var profileImageView: ImageView
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            handleSelectedImage(it)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(requireContext(), "Permission denied to read external storage", Toast.LENGTH_SHORT).show()
        }
    }

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

        // Profile image initialization
        initProfileImage(view)

        // Setup settings FAB
        setupForwardButton(view)
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

        // Click listener for profile image button
        binding.changeImageButton.setOnClickListener {
            checkPermissionAndOpenGallery()
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

    // Fixed methods for profile image handling
    private fun initProfileImage(view: View) {
        profileImageView = view.findViewById(R.id.profileImageView)
        loadSavedProfileImage()
    }

    private fun checkPermissionAndOpenGallery() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openImagePicker()
                    }
                    else -> {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
            }
            else -> {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openImagePicker()
                    }
                    else -> {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun handleSelectedImage(imageUri: Uri) {
        try {
            // Copy the image to internal storage
            val savedImageFile = copyImageToInternalStorage(imageUri)

            if (savedImageFile != null) {
                // Display the image from internal storage
                val savedImageUri = Uri.fromFile(savedImageFile)
                profileImageView.setImageURI(savedImageUri)

                // Save the internal file path to SharedPreferences
                val sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
                sharedPreferences.edit()
                    .putString("profile_image_path", savedImageFile.absolutePath)
                    .apply()

                Toast.makeText(requireContext(), "Profile image aggiornata!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Errore nel salvare l'immagine", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Errore caricamento immagine: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun copyImageToInternalStorage(sourceUri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(sourceUri)
            val profileImageDir = File(requireContext().filesDir, "profile_images")

            if (!profileImageDir.exists()) {
                profileImageDir.mkdirs()
            }

            val imageFile = File(profileImageDir, "profile_image.jpg")
            val outputStream = FileOutputStream(imageFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            imageFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun loadSavedProfileImage() {
        val sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        val imagePath = sharedPreferences.getString("profile_image_path", null)

        imagePath?.let { path ->
            try {
                val imageFile = File(path)
                if (imageFile.exists()) {
                    val imageUri = Uri.fromFile(imageFile)
                    profileImageView.setImageURI(imageUri)
                }
            } catch (e: Exception) {
                // If image can't be loaded, keep default
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupForwardButton(view: View) {
        val fabForward = view.findViewById<FloatingActionButton>(R.id.fabForward)
        fabForward.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_profilefragment)
        }
    }
}