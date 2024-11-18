package com.example.rentingapp.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.rentingapp.R

class ImageAdapter(
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private val images = mutableListOf<Uri>()

    fun addImage(uri: Uri) {
        images.add(uri)
        notifyItemInserted(images.size - 1)
    }

    fun removeImage(position: Int) {
        if (position in images.indices) {
            images.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getImages(): List<Uri> = images.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position], position)
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(uri: Uri, position: Int) {
            imageView.setImageURI(uri)
            deleteButton.setOnClickListener { onDeleteClick(position) }
        }
    }
}
