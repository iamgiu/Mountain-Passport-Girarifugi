package com.example.mountainpassport_girarifugi.ui.profile.settings

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
import android.graphics.BitmapFactory
import android.util.Base64
import android.app.Activity
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.content.Context

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var profileImageView: ImageView
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            launchCropActivity(it)
        }
    }

    private val cropActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Recupera l'immagine croppata dalle SharedPreferences
            val croppedBitmap = getCroppedImageFromTemp()
            croppedBitmap?.let { bitmap ->
                handleCroppedImage(bitmap)
            }
        }
    }

    private fun getCroppedImageFromTemp(): Bitmap? {
        return try {
            val sharedPreferences = requireContext().getSharedPreferences("crop_temp", Context.MODE_PRIVATE)
            val base64String = sharedPreferences.getString("cropped_image", null)

            if (!base64String.isNullOrEmpty()) {
                // Pulisce i dati temporanei
                sharedPreferences.edit().remove("cropped_image").apply()

                // Converte da Base64 a Bitmap
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun launchCropActivity(imageUri: Uri) {
        val intent = Intent(requireContext(), ImageCropActivity::class.java)
        intent.putExtra(ImageCropActivity.EXTRA_IMAGE_URI, imageUri)
        cropActivityLauncher.launch(intent)
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

        // Inizializzazione immagine profilo
        initProfileImage(view)

        // FAB Settings inizializzato
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

        viewModel.imageUploadSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Immagine profilo caricata con successo!", Toast.LENGTH_SHORT).show()
                viewModel.onImageUploadSuccessHandled()
            }
        }

        viewModel.profileImageUri.observe(viewLifecycleOwner) { imageUrl ->
            if (!imageUrl.isNullOrEmpty()) {
                loadImageFromBase64(imageUrl)
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

    private fun handleCroppedImage(bitmap: Bitmap) {
        try {

            profileImageView.setImageBitmap(bitmap)

            // Converto a Base64 per caricare immagine di profilo
            uploadBitmapAsBase64(bitmap)

            Toast.makeText(requireContext(), "Caricamento immagine profilo...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Errore nella selezione immagine: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun uploadBitmapAsBase64(bitmap: Bitmap) {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64String = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            viewModel.updateUserProfileImageDirect(base64String)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Errore nel processare l'immagine: ${e.message}", Toast.LENGTH_LONG).show()
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
        val currentUser = viewModel.currentUser.value
        if (!currentUser?.profileImageUrl.isNullOrEmpty()) {
            loadImageFromBase64(currentUser!!.profileImageUrl)
        }
    }

    private fun loadImageFromBase64(base64String: String) {
        try {
            if (base64String.isNotEmpty()) {
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                profileImageView.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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