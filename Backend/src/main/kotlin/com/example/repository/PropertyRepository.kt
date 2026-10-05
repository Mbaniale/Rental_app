package com.example.repository

import com.example.database.PropertiesTable
import com.example.models.Property
import com.example.models.PropertyFilter
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

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
        isAvailable = row[PropertiesTable.isAvailable],
        ownerId = row[PropertiesTable.ownerId]
    )

    fun getAllProperties(): List<Property> = transaction {
        PropertiesTable.selectAll().map(::resultRowToProperty)
    }

    private fun escapeLike(text: String): String =
        text.lowercase()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    fun searchProperties(filter: PropertyFilter): List<Property> = transaction {
        val conditions = mutableListOf<Op<Boolean>>()

        with(SqlExpressionBuilder) {
            // Level 1: basic
            filter.location?.let { conditions += PropertiesTable.location.lowerCase() like "%${escapeLike(it)}%" }
            filter.price?.let { conditions += PropertiesTable.price eq it.toBigDecimal() }
            filter.minPrice?.let { conditions += PropertiesTable.price greaterEq it.toBigDecimal() }
            filter.maxPrice?.let { conditions += PropertiesTable.price lessEq it.toBigDecimal() }
            filter.available?.let { conditions += PropertiesTable.isAvailable eq it }

            // Level 2: specs
            filter.bedrooms?.let { conditions += PropertiesTable.bedrooms eq it }
            filter.minBedrooms?.let { conditions += PropertiesTable.bedrooms greaterEq it }
            filter.maxBedrooms?.let { conditions += PropertiesTable.bedrooms lessEq it }
            filter.minBathrooms?.let { conditions += PropertiesTable.bathrooms greaterEq it }
            filter.maxBathrooms?.let { conditions += PropertiesTable.bathrooms lessEq it }
            if (filter.propertyTypes.isNotEmpty()) {
                conditions += PropertiesTable.propertyType.lowerCase() inList filter.propertyTypes.map { it.lowercase() }
            }

            // Level 3: keyword searches title and description
            filter.keyword?.let {
                val pattern = "%${escapeLike(it)}%"
                conditions += (PropertiesTable.title.lowerCase() like pattern) or
                        (PropertiesTable.description.lowerCase() like pattern)
            }
        }

        val combined: Op<Boolean> = conditions.fold(Op.TRUE as Op<Boolean>) { acc, op -> acc and op }

        val sortColumn: Expression<*> = when (filter.sortBy) {
            "price" -> PropertiesTable.price
            "bedrooms" -> PropertiesTable.bedrooms
            "bathrooms" -> PropertiesTable.bathrooms
            else -> PropertiesTable.id
        }

        PropertiesTable.selectAll()
            .where { combined }
            .orderBy(sortColumn to filter.sortOrder, PropertiesTable.id to SortOrder.ASC)
            .limit(filter.pageSize)
            .offset(((filter.page - 1) * filter.pageSize).toLong())
            .map(::resultRowToProperty)
    }

    fun getPropertyById(id: Int): Property? = transaction {
        PropertiesTable.selectAll().where { PropertiesTable.id eq id }
            .map(::resultRowToProperty)
            .singleOrNull()
    }

    /** Listings that belong to one landlord, newest first. */
    fun getPropertiesByOwner(ownerId: Int, page: Int, pageSize: Int): List<Property> = transaction {
        PropertiesTable.selectAll()
            .where { PropertiesTable.ownerId eq ownerId }
            .orderBy(PropertiesTable.id to SortOrder.DESC)
            .limit(pageSize)
            .offset(((page - 1) * pageSize).toLong())
            .map(::resultRowToProperty)
    }

    /** The owner always comes from the verified token, never from the request body. */
    fun createProperty(property: Property, ownerId: Int): Property = transaction {
        val insertStatement = PropertiesTable.insert {
            it[title] = property.title
            it[description] = property.description
            it[price] = property.price.toBigDecimal()
            it[location] = property.location
            it[bedrooms] = property.bedrooms
            it[bathrooms] = property.bathrooms
            it[propertyType] = property.propertyType
            it[isAvailable] = property.isAvailable
            it[PropertiesTable.ownerId] = ownerId
        }
        val generatedId = insertStatement[PropertiesTable.id]
        property.copy(id = generatedId, ownerId = ownerId)
    }

    /** Updates the editable fields. The owner is deliberately never changed here. */
    fun updateProperty(id: Int, property: Property): Boolean = transaction {
        PropertiesTable.update({ PropertiesTable.id eq id }) {
            it[title] = property.title
            it[description] = property.description
            it[price] = property.price.toBigDecimal()
            it[location] = property.location
            it[bedrooms] = property.bedrooms
            it[bathrooms] = property.bathrooms
            it[propertyType] = property.propertyType
            it[isAvailable] = property.isAvailable
        } > 0
    }

    fun deleteProperty(propertyId: Int): Boolean = transaction {
        PropertiesTable.deleteWhere {
            with(SqlExpressionBuilder) { PropertiesTable.id eq propertyId }
        } > 0
    }
}