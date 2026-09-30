package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Property(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val location: String,
    val bedrooms: Int,
    val bathrooms: Int,
    val propertyType: String,   // e.g. "Apartment", "House", "Studio"
    val isAvailable: Boolean = true
)