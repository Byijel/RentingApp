package com.example.rentingapp.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
        binding.zipCodeInput.addTextChangedListener(createTextWatcher(binding.zipCodeLayout))

        binding.registerButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val firstName = binding.firstNameInput.text.toString()
            val lastName = binding.lastNameInput.text.toString()
            val phone = binding.phoneInput.text.toString()
            val city = binding.cityInput.text.toString()
            val zipCode = binding.zipCodeInput.text.toString()
            val street = binding.streetInput.text.toString()
            val number = binding.numberInput.text.toString()

            if (validateInputs(email, password, firstName, lastName)) {
                registerUser(email, password, firstName, lastName, phone, city, zipCode, street, number)
            }
        }
    }

    private fun validateInputs(email: String, password: String, firstName: String, lastName: String): Boolean {
        var isValid = true
        
        // Clear all previous errors
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        binding.firstNameLayout.error = null
        binding.lastNameLayout.error = null
        binding.phoneLayout.error = null
        binding.zipCodeLayout.error = null

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
        val passwordPattern = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

    private fun isValidPhone(phone: String): Boolean {
        val phonePattern = "^[+]?[0-9]{10,13}$"
        return phone.matches(phonePattern.toRegex())
    }

    private fun isValidZipCode(zipCode: String): Boolean {
        val zipCodePattern = "^[0-9]{5}(?:-[0-9]{4})?$"
        return zipCode.matches(zipCodePattern.toRegex())
    }

    private fun registerUser(
        email: String, password: String, firstName: String, lastName: String,
        phone: String, city: String, zipCode: String, street: String, number: String
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

        if (zipCode.isNotEmpty() && !isValidZipCode(zipCode)) {
            binding.zipCodeLayout.error = "Please enter a valid zip code"
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
                        "phone" to phone,
                        "address" to hashMapOf(
                            "city" to city,
                            "zipCode" to zipCode,
                            "street" to street,
                            "number" to number
                        )
                    )

                    db.collection("users")
                        .document(auth.currentUser!!.uid)
                        .set(user)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.nav_home)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
