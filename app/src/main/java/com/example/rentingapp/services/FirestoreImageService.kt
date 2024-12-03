package com.example.rentingapp.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.Blob
import java.io.ByteArrayOutputStream
import java.util.UUID

class FirestoreImageService(private val context: Context) {
    
    companion object {
        private const val TAG = "FirestoreImageService"
        private const val MAX_IMAGE_SIZE = 900000 // ~900KB to stay under Firestore's 1MB limit
        private const val INITIAL_QUALITY = 80
        private const val MIN_QUALITY = 5
        private const val QUALITY_DECREMENT = 5
        private const val MAX_IMAGE_DIMENSION = 800
    }

    /**
     * Convert a list of image URIs to a list of compressed Firestore Blobs
     */
    fun uriToBlobs(uris: List<Uri>): List<Pair<String, Blob>> {
        return uris.mapNotNull { uri ->
            try {
                // Generate a unique ID for this image
                val imageId = UUID.randomUUID().toString()
                
                // Convert URI to compressed bitmap bytes
                val bytes = compressImage(uri) ?: return@mapNotNull null
                
                // Convert to Firestore Blob
                val blob = Blob.fromBytes(bytes)
                
                // Return pair of ID and blob
                Pair(imageId, blob)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process image: ${e.message}")
                null
            }
        }
    }

    private fun compressImage(uri: Uri): ByteArray? {
        try {
            // Load bitmap with reduced size
            val bitmap = loadScaledBitmap(uri) ?: return null

            var quality = INITIAL_QUALITY
            var bytes: ByteArray
            
            // Compress with decreasing quality until size is acceptable
            do {
                ByteArrayOutputStream().use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    bytes = outputStream.toByteArray()
                    quality -= QUALITY_DECREMENT
                }
            } while (bytes.size > MAX_IMAGE_SIZE && quality > MIN_QUALITY)

            // Clean up
            bitmap.recycle()

            // Check final size
            return if (bytes.size <= MAX_IMAGE_SIZE) {
                bytes
            } else {
                Log.e(TAG, "Image too large even after maximum compression: ${bytes.size} bytes")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image", e)
            return null
        }
    }

    private fun loadScaledBitmap(uri: Uri): Bitmap? {
        return try {
            // First decode bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            // Calculate scale factor
            val scale = calculateScaleFactor(options.outWidth, options.outHeight)

            // Decode bitmap with scale factor
            options.apply {
                inJustDecodeBounds = false
                inSampleSize = scale
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap", e)
            null
        }
    }

    private fun calculateScaleFactor(width: Int, height: Int): Int {
        var scale = 1
        while (width / scale > MAX_IMAGE_DIMENSION || height / scale > MAX_IMAGE_DIMENSION) {
            scale *= 2
        }
        return scale
    }

    /**
     * Convert a Firestore Blob back to a Bitmap
     */
    fun blobToBitmap(blob: Blob): Bitmap? {
        return try {
            val bytes = blob.toBytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting blob to bitmap", e)
            null
        }
    }

    // Add this function for single URI
    fun uriToBlob(uri: Uri): Blob? {
        return try {
            // Convert URI to compressed bitmap bytes
            val bytes = compressImage(uri) ?: return null
            // Convert to Firestore Blob
            Blob.fromBytes(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image: ${e.message}")
            null
        }
    }
}
