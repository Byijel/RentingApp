package com.example.rentingapp.ui.register

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rentingapp.R
import com.example.rentingapp.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import android.text.TextWatcher
import android.text.Editable
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.os.Environment
import android.provider.MediaStore.Files.FileColumns
import androidx.core.content.FileProvider
import android.Manifest
import android.util.Log
import com.example.rentingapp.services.FirestoreImageService
import com.google.firebase.firestore.Blob

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var imageService: FirestoreImageService
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.profileImageView.setImageURI(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.extras?.get("data")?.let { bitmap ->
                val uri = saveBitmapToUri(bitmap as Bitmap)
                selectedImageUri = uri
                binding.profileImageView.setImageURI(uri)
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to take pictures", Toast.LENGTH_SHORT).show()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchGallery()
        } else {
            Toast.makeText(context, "Storage permission is required to select images", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth and Firestore
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        storage = Firebase.storage
        imageService = FirestoreImageService(requireContext())

        // Setup image buttons
        binding.addImageButton.setOnClickListener {
            checkStoragePermission()
        }

        binding.buttonTakePicture.setOnClickListener {
            checkCameraPermission()
        }

        // Set initial profile image
        binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)

        // Create a simple TextWatcher
        val createTextWatcher = { field: TextInputLayout ->
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    field.error = null
                }
                override fun afterTextChanged(s: Editable?) {}
            }
        }

        // Add text change listeners to clear errors
        binding.emailInput.addTextChangedListener(createTextWatcher(binding.emailLayout))
        binding.passwordInput.addTextChangedListener(createTextWatcher(binding.passwordLayout))
        binding.firstNameInput.addTextChangedListener(createTextWatcher(binding.firstNameLayout))
        binding.lastNameInput.addTextChangedListener(createTextWatcher(binding.lastNameLayout))
        binding.phoneInput.addTextChangedListener(createTextWatcher(binding.phoneLayout))

        binding.registerButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val firstName = binding.firstNameInput.text.toString()
            val lastName = binding.lastNameInput.text.toString()
            val phone = binding.phoneInput.text.toString()

            if (validateInputs(email, password, firstName, lastName)) {
                registerUser(email, password, firstName, lastName, phone)
            }
        }
    }

    private fun registerUser(email: String, password: String, firstName: String, lastName: String, phone: String) {
        if (!validateInputs(email, password, firstName, lastName)) {
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(context, "Please select a profile image", Toast.LENGTH_SHORT).show()
            return
        }

        // Process the image first
        val imageBlob = imageService.uriToBlob(selectedImageUri!!)
        if (imageBlob == null) {
            Toast.makeText(context, "Failed to process profile image. Please try a different image.", Toast.LENGTH_LONG).show()
            return
        }

        // Show progress and disable button
        binding.registerButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.isIndeterminate = true

        // First create the Firebase Auth user
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                // Authentication successful, now save user data
                val userId = authResult.user?.uid ?: run {
                    handleRegistrationError("Failed to get user ID")
                    return@addOnSuccessListener
                }

                // Create user data with image blob
                val userData = hashMapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email,
                    "phone" to phone,
                    "profileImage" to imageBlob,
                    "hasAddress" to false  // Default flag for address registration
                )

                // Save user data to Firestore
                db.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        // Navigate to address registration
                        findNavController().navigate(R.id.action_registerFragment_to_address_registration)
                    }
                    .addOnFailureListener { e ->
                        handleRegistrationError("Error saving user data: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                handleRegistrationError("Registration failed: ${e.message}")
            }
    }

    private fun handleRegistrationError(errorMessage: String) {
        Log.e("RegisterFragment", errorMessage)
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()

        // Reset UI
        binding.registerButton.isEnabled = true
        binding.progressBar.visibility = View.GONE
        binding.progressBar.isIndeterminate = false
    }

    // Helper method to retrieve and display stored profile image
    private fun displayStoredProfileImage(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val imageBlob = document.get("profileImage") as? Blob
                imageBlob?.let { blob ->
                    val bitmap = imageService.blobToBitmap(blob)
                    bitmap?.let {
                        binding.profileImageView.setImageBitmap(it)
                    }
                }
            }
    }

    // Optional: Add method to update profile image later
    private fun updateProfileImage(userId: String, newImageUri: Uri) {
        val newImageBlob = imageService.uriToBlob(newImageUri)
        newImageBlob?.let { blob ->
            db.collection("users").document(userId)
                .update("profileImage", blob)
                .addOnSuccessListener {
                    Toast.makeText(context, "Profile image updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update image: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun saveBitmapToUri(bitmap: Bitmap): Uri {
        val filesDir = requireContext().getExternalFilesDir(null)
        val imageFile = File(filesDir, "profile_${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }
        } catch (e: IOException) {
            Log.e("RegisterFragment", "Error saving bitmap to file", e)
        }
        return Uri.fromFile(imageFile)
    }

    // Existing validation methods remain the same
    private fun validateInputs(email: String, password: String, firstName: String, lastName: String): Boolean {
        var isValid = true

        // Clear all previous errors
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        binding.firstNameLayout.error = null
        binding.lastNameLayout.error = null
        binding.phoneLayout.error = null

        // Email validation
        if (!isValidEmail(email)) {
            binding.emailLayout.error = "Please enter a valid email address"
            isValid = false
        }

        // Password validation
        if (!isValidPassword(password)) {
            binding.passwordLayout.error = "Password must be at least 8 characters long and contain at least one number, one uppercase letter, and one special character"
            isValid = false
        }

        // Name validation
        if (firstName.isEmpty()) {
            binding.firstNameLayout.error = "First name is required"
            isValid = false
        } else if (firstName.length < 2) {
            binding.firstNameLayout.error = "First name must be at least 2 characters long"
            isValid = false
        }

        if (lastName.isEmpty()) {
            binding.lastNameLayout.error = "Last name is required"
            isValid = false
        } else if (lastName.length < 2) {
            binding.lastNameLayout.error = "Last name must be at least 2 characters long"
            isValid = false
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    private fun isValidPassword(password: String): Boolean {
        val hasNumber = password.any { it.isDigit() }
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasSpecialChar = password.any { it in "@#$%^&+=!?" }
        val isLongEnough = password.length >= 8
        val hasNoWhitespace = !password.contains("\\s".toRegex())

        return hasNumber && hasUpperCase && hasSpecialChar && isLongEnough && hasNoWhitespace
    }

    private fun isValidPhone(phone: String): Boolean {
        val phonePattern = "^[+]?[0-9]{10,13}$"
        return phone.matches(phonePattern.toRegex())
    }

    private fun checkCameraPermission() {
        when {
            requireContext().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(context, "Camera permission is required to take pictures", Toast.LENGTH_SHORT).show()
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkStoragePermission() {
        when {
            requireContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                launchGallery()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                Toast.makeText(context, "Storage permission is required to select images", Toast.LENGTH_SHORT).show()
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun launchCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
