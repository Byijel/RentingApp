package com.example.rentingapp

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class RentalItem(
    val id: String = "",
    val applianceName: String = "",
    val dailyRate: Double = 0.0,
    val category: String = "",
    val condition: String = "",
    val description: String = "",
    val availability: Boolean = true,
    val image: @RawValue Blob? = null,
    val createdAt: @RawValue Timestamp = Timestamp.now(),
    val ownerName: String = "",
    val currentRenter: String? = null
) : Parcelable