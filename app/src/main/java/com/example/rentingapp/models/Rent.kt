package com.example.rentingapp.models

import java.util.Date

data class Rent(
    val id: Int = 0,
    val lockedDates: Date = Date(),
    val user: User? = null,
    val duration: Int = 0
)
