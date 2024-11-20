package com.example.rentingapp

import com.google.firebase.firestore.Blob
import com.google.firebase.Timestamp

data class RentalItem(
    val id: String = "",
    val applianceName: String = "",
    val dailyRate: Double = 0.0,
    val category: String = "",
    val condition: String = "",
    val description: String = "",
    val availability: Boolean = true,
    val image: Blob? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val ownerName: String = ""
)