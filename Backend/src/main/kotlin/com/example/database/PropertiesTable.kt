#!/usr/bin/env kotlin

package com.example.database

import org.jetbrains.exposed.sql.Table

object PropertiesTable : Table("properties") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val description = text("description")
    val price = decimal("price", 10, 2) // 10 digits total, 2 after decimal
    val location = varchar("location", 255)
    val bedrooms = integer("bedrooms")
    val bathrooms = integer("bathrooms")
    val propertyType = varchar("property_type", 50)
    val isAvailable = bool("is_available").default(true)

    override val primaryKey = PrimaryKey(id)
}