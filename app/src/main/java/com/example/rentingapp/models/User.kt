package com.example.rentingapp.models

data class User(
    val id: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val phoneNumber: String = "",
    val gender: Gender = Gender.OTHER,
    val address: Address? = null
)
    