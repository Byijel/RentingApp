package com.example.rentingapp.models

data class Address(
    val id: Int = 0,
    val street: String = "",
    val houseNumber: Int = 0,
    val city: String = "",
    val postalCode: String = "",
    val country: String = ""
)
