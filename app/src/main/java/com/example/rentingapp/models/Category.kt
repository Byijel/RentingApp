package com.example.rentingapp.models

enum class Category(val displayName: String) {
    ELECTRONICS("Electronics"),
    TOOLS("Tools"),
    SPORTS("Sports Equipment"),
    CAMPING("Camping Gear"),
    PARTY("Party & Events"),
    GARDEN("Garden Equipment"),
    VEHICLES("Vehicles"),
    CLOTHING("Clothing"),
    BOOKS("Books"),
    OTHER("Other");

    override fun toString(): String = displayName

    companion object {
        fun getDisplayNames(): Array<String> = values().map { it.displayName }.toTypedArray()
    }
}
