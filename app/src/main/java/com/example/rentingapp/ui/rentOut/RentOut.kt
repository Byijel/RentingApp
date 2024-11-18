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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import com.example.rentingapp.R
import com.example.rentingapp.adapters.ImageAdapter
import com.google.android.material.button.MaterialButton

class RentOut : Fragment() {
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var addPhotosButton: MaterialButton
    private lateinit var imageAdapter: ImageAdapter

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
        
        // Initialize views
        photosRecyclerView = view.findViewById(R.id.photosRecyclerView)
        addPhotosButton = view.findViewById(R.id.addPhotosButton)

        // Setup image adapter
        imageAdapter = ImageAdapter { position ->
            removeImage(position)
        }
        photosRecyclerView.adapter = imageAdapter

        // Setup click listener for add photos button
        addPhotosButton.setOnClickListener {
            openImagePicker()
        }
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