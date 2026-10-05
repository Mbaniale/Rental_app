package com.example.models

import io.ktor.http.Parameters
import org.jetbrains.exposed.sql.SortOrder

class BadParam(message: String) : Exception(message)

data class PropertyFilter(
    // Level 1: basic
    val location: String? = null,
    val price: Double? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val available: Boolean? = null,
    // Level 2: specs
    val bedrooms: Int? = null,
    val minBedrooms: Int? = null,
    val maxBedrooms: Int? = null,
    val minBathrooms: Int? = null,
    val maxBathrooms: Int? = null,
    val propertyTypes: List<String> = emptyList(),
    // Level 3: advanced
    val keyword: String? = null,
    val sortBy: String = "id",
    val sortOrder: SortOrder = SortOrder.ASC,
    val page: Int = 1,
    val pageSize: Int = 20
)

private val allowedSortFields = setOf("id", "price", "bedrooms", "bathrooms")

private fun Parameters.double(name: String): Double? =
    this[name]?.let { it.toDoubleOrNull() ?: throw BadParam("Invalid $name") }

private fun Parameters.int(name: String): Int? =
    this[name]?.let { it.toIntOrNull() ?: throw BadParam("Invalid $name") }

private fun Parameters.bool(name: String): Boolean? =
    this[name]?.let { it.toBooleanStrictOrNull() ?: throw BadParam("Invalid $name (use true or false)") }

private fun checkRange(name: String, min: Number?, max: Number?) {
    if (min != null && max != null && min.toDouble() > max.toDouble())
        throw BadParam("min$name cannot be greater than max$name")
}

fun Parameters.toPropertyFilter(): PropertyFilter {
    val price = double("price")
    val minPrice = double("minPrice")
    val maxPrice = double("maxPrice")
    val bedrooms = int("bedrooms")
    val minBedrooms = int("minBedrooms")
    val maxBedrooms = int("maxBedrooms")
    val minBathrooms = int("minBathrooms")
    val maxBathrooms = int("maxBathrooms")
    checkRange("Price", minPrice, maxPrice)
    checkRange("Bedrooms", minBedrooms, maxBedrooms)
    checkRange("Bathrooms", minBathrooms, maxBathrooms)

    val sortBy = this["sortBy"] ?: "id"
    if (sortBy !in allowedSortFields)
        throw BadParam("sortBy must be one of: ${allowedSortFields.joinToString()}")

    val sortOrder = when (this["sortOrder"]?.lowercase()) {
        null, "asc" -> SortOrder.ASC
        "desc" -> SortOrder.DESC
        else -> throw BadParam("sortOrder must be asc or desc")
    }

    val page = int("page") ?: 1
    val pageSize = int("pageSize") ?: 20
    if (page < 1) throw BadParam("page must be at least 1")
    if (pageSize !in 1..100) throw BadParam("pageSize must be between 1 and 100")

    return PropertyFilter(
        location = this["location"]?.trim()?.takeIf { it.isNotEmpty() },
        price = price,
        minPrice = minPrice,
        maxPrice = maxPrice,
        available = bool("available"),
        bedrooms = bedrooms,
        minBedrooms = minBedrooms,
        maxBedrooms = maxBedrooms,
        minBathrooms = minBathrooms,
        maxBathrooms = maxBathrooms,
        // comma-separated: ?propertyType=apartment,studio
        propertyTypes = this["propertyType"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
        keyword = this["keyword"]?.trim()?.takeIf { it.isNotEmpty() },
        sortBy = sortBy,
        sortOrder = sortOrder,
        page = page,
        pageSize = pageSize
    )
}