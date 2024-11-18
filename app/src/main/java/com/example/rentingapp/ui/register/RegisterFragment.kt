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
        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun registerUser(
        email: String, password: String, firstName: String, lastName: String,
        phone: String, city: String, zipCode: String, street: String, number: String
    ) {
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
