package com.example.rentingapp.ui.rentOut

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import com.example.rentingapp.R
import com.example.rentingapp.adapters.ImageAdapter
import com.example.rentingapp.models.Category
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class RentOut : Fragment() {
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var addPhotosButton: MaterialButton
    private lateinit var submitButton: MaterialButton
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var nameEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var priceEditText: TextInputEditText
    private lateinit var categorySpinner: AutoCompleteTextView

    private val db = Firebase.firestore

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                addImageToAdapter(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rent_out, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupImageAdapter()
        setupCategoryDropdown()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        photosRecyclerView = view.findViewById(R.id.photosRecyclerView)
        addPhotosButton = view.findViewById(R.id.addPhotosButton)
        submitButton = view.findViewById(R.id.submitButton)
        nameEditText = view.findViewById(R.id.nameEditText)
        descriptionEditText = view.findViewById(R.id.descriptionEditText)
        priceEditText = view.findViewById(R.id.priceEditText)
        categorySpinner = view.findViewById(R.id.categorySpinner)
    }

    private fun setupCategoryDropdown() {
        val categories = Category.getDisplayNames()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        categorySpinner.setAdapter(adapter)
    }

    private fun setupImageAdapter() {
        imageAdapter = ImageAdapter { position ->
            removeImage(position)
        }
        photosRecyclerView.adapter = imageAdapter
    }

    private fun setupClickListeners() {
        addPhotosButton.setOnClickListener {
            openImagePicker()
        }

        submitButton.setOnClickListener {
            if (validateForm()) {
                submitButton.isEnabled = false // Prevent double submission
                createRentOutPost()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (nameEditText.text.isNullOrBlank()) {
            nameEditText.error = "Name is required"
            isValid = false
        }

        if (descriptionEditText.text.isNullOrBlank()) {
            descriptionEditText.error = "Description is required"
            isValid = false
        }

        if (priceEditText.text.isNullOrBlank()) {
            priceEditText.error = "Price is required"
            isValid = false
        }

        if (categorySpinner.text.isNullOrBlank()) {
            categorySpinner.error = "Category is required"
            isValid = false
        }

        return isValid
    }

    private fun createRentOutPost() {
        val rentOutPost = hashMapOf(
            "name" to nameEditText.text.toString(),
            "description" to descriptionEditText.text.toString(),
            "price" to (priceEditText.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0),
            "category" to categorySpinner.text.toString(),
            "createdAt" to com.google.firebase.Timestamp.now(),
            "available" to true
        )

        db.collection("RentOutPosts")
            .add(rentOutPost)
            .addOnSuccessListener {
                Toast.makeText(context, "Post created successfully!", Toast.LENGTH_SHORT).show()
                clearForm()
                submitButton.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error creating post: ${e.message}", Toast.LENGTH_LONG).show()
                submitButton.isEnabled = true
            }
    }

    private fun clearForm() {
        nameEditText.text?.clear()
        descriptionEditText.text?.clear()
        priceEditText.text?.clear()
        categorySpinner.text?.clear()
        imageAdapter.clearImages()
        photosRecyclerView.visibility = View.GONE
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun addImageToAdapter(uri: Uri) {
        imageAdapter.addImage(uri)
        photosRecyclerView.visibility = View.VISIBLE
    }

    private fun removeImage(position: Int) {
        imageAdapter.removeImage(position)
        if (imageAdapter.itemCount == 0) {
            photosRecyclerView.visibility = View.GONE
        }
    }
}