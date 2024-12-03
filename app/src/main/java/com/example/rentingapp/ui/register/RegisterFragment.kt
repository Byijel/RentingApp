package com.example.rentingapp.ui.register

import android.app.Activity
import android.content.Intent
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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rentingapp.R
import com.example.rentingapp.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
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

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var profileImageView: ImageView
    private lateinit var addImageButton: Button
    private lateinit var takePictureButton: Button
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            profileImageView.setImageURI(it)
        }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(context, "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                profileImageView.setImageBitmap(imageBitmap)
                selectedImageUri = saveImageToTempFile(imageBitmap)
            }
        }
    }

    private fun saveImageToTempFile(bitmap: Bitmap): Uri {
        val tempFile = File.createTempFile("profile_image", ".jpg", requireContext().cacheDir)
        tempFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return Uri.fromFile(tempFile)
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

        // Initialize image-related views
        profileImageView = binding.profileImageView
        addImageButton = binding.addImageButton
        takePictureButton = binding.buttonTakePicture

        // Setup image buttons
        addImageButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        takePictureButton.setOnClickListener {
            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
        }

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

    private fun registerUser(
        email: String, password: String, firstName: String, lastName: String,
        phone: String
    ) {
        // Validate all fields
        if (!validateInputs(email, password, firstName, lastName)) {
            return
        }

        // Additional validations for optional fields if they're not empty
        if (phone.isNotEmpty() && !isValidPhone(phone)) {
            binding.phoneLayout.error = "Please enter a valid phone number"
            return
        }

        binding.registerButton.isEnabled = false // Prevent multiple clicks

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Save additional user information to Firestore
                    val user = hashMapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to email,
                        "phone" to phone
                    )

                    // Add image to user data if selected
                    selectedImageUri?.let { uri ->
                        user["profileImageUri"] = uri.toString()
                    }

                    db.collection("users")
                        .document(auth.currentUser!!.uid)
                        .set(user)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                            // Navigate to address registration after successful registration
                            findNavController().navigate(R.id.nav_address_registration)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            binding.registerButton.isEnabled = true
                        }
                } else {
                    Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    binding.registerButton.isEnabled = true
                }
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
