package com.example.rentingapp.models

enum class Condition(val displayName: String) {
    NEW("New"),
    LIKE_NEW("Like New"),
    VERY_GOOD("Very Good"),
    GOOD("Good"),
    ACCEPTABLE("Acceptable");

    companion object {
        fun fromDisplayName(displayName: String): Condition? {
            return values().find { it.displayName == displayName }
        }
    }
}
