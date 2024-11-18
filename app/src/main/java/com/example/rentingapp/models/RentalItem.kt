package com.example.rentingapp.models

import android.graphics.Bitmap

data class RentalItem(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val pricePerDay: Double = 0.0,
    val category: String = "",
    val condition: String = "",
    val location: Address? = null,
    val images: List<Bitmap> = emptyList(),
    val rentable: Boolean = true,
    val rents: List<Rent> = emptyList()
) {
    fun rentItem(user: User, days: Int): Rent {
        // Implementation will be added later
        return Rent(user = user, duration = days)
    }
}
