package com.example.rentingapp.models

import android.graphics.Bitmap
import com.google.firebase.firestore.Blob

data class RentalItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val pricePerDay: Double = 0.0,
    val rentType: RentType = RentType.OTHER,
    val condition: String = "",
    val location: Address? = null,
    val images: List<Bitmap> = emptyList(),
    val rentable: Boolean = true,
    val rents: List<Rent> = emptyList(),
    val image: Blob? = null
) {
    fun rentItem(user: User, days: Int): Rent {
        // Implementation will be added later
        return Rent(user = user, duration = days)
    }
}