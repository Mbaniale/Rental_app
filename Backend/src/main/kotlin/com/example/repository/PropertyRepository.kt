

package com.example.repository

import com.example.database.PropertiesTable
import com.example.models.Property
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.selectAll

class PropertyRepository {
    // Maps a database row to your Property data class
    private fun resultRowToProperty(row: ResultRow): Property = Property(
        id = row[PropertiesTable.id],
        title = row[PropertiesTable.title],
        description = row[PropertiesTable.description],
        price = row[PropertiesTable.price].toDouble(),
        location = row[PropertiesTable.location],
        bedrooms = row[PropertiesTable.bedrooms],
        bathrooms = row[PropertiesTable.bathrooms],
        propertyType = row[PropertiesTable.propertyType],
        isAvailable = row[PropertiesTable.isAvailable]
    )

    fun getAllProperties(): List<Property> = transaction {
        PropertiesTable.selectAll().map(::resultRowToProperty)
    }

    fun getPropertyById(id: Int): Property? = transaction {
        PropertiesTable.selectAll().where { PropertiesTable.id eq id }
            .map(::resultRowToProperty)
            .singleOrNull()
    }

    fun createProperty(property: Property): Property = transaction {
        val insertStatement = PropertiesTable.insert {
            it[title] = property.title
            it[description] = property.description
            it[price] = property.price.toBigDecimal()
            it[location] = property.location
            it[bedrooms] = property.bedrooms
            it[bathrooms] = property.bathrooms
            it[propertyType] = property.propertyType
            it[isAvailable] = property.isAvailable
        }
        // Retrieve the generated ID and return the full object
        val generatedId = insertStatement[PropertiesTable.id]
        property.copy(id = generatedId)
    }
}