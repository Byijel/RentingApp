package com.example.rentingapp

data class RentalItem(
    val id: String = "",
    val applianceName: String = "",
    val dailyRate: Double = 0.0,
    val category: String = "",
    val condition: String = "",
    val description: String = "",
    val availability: Boolean = true
)