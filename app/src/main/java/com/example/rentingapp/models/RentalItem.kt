package com.example.rentingapp.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class RentalItem(
    val id: String = "",
    var applianceName: String = "",
    val dailyRate: Double = 0.0,
    val category: String = "",
    val condition: String = "",
    val description: String = "",
    val availability: Boolean = true,
    val image: @RawValue Blob? = null,
    val createdAt: @RawValue Timestamp = Timestamp.now(),
    var ownerName: String = "",
    val currentRenter: String? = null,
    val startDate: @RawValue Timestamp? = null,
    val endDate: @RawValue Timestamp? = null
) : Parcelable