package com.example.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

object DatabaseFactory {
    fun init(application: Application) {
        val config = application.environment.config
        val hikariConfig = HikariConfig().apply {
            driverClassName = config.property("database.driver").getString()
            jdbcUrl = config.property("database.url").getString()
            username = config.property("database.user").getString()
            password = config.property("database.password").getString()
            maximumPoolSize = config.property("database.maxPoolSize").getString().toInt()
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(PropertiesTable)

            if (PropertiesTable.selectAll().count() == 0L) {
                val samples = listOf(
                    listOf("Sunny 2BR in Kilimani", "Bright apartment near Yaya Centre", "85000", "Kilimani, Nairobi", 2, 2, "Apartment", true),
                    listOf("Modern Studio in Westlands", "Compact studio with backup power", "45000", "Westlands, Nairobi", 1, 1, "Studio", true),
                    listOf("Family Maisonette in Karen", "4BR maisonette with a garden", "220000", "Karen, Nairobi", 4, 3, "Maisonette", true),
                    listOf("Cozy 1BR in Ruaka", "Quiet compound, secure parking", "32000", "Ruaka, Kiambu", 1, 1, "Apartment", false),
                    listOf("Bedsitter in Roysambu", "Near Thika Road, water included", "12000", "Roysambu, Nairobi", 1, 1, "Bedsitter", true)
                )
                samples.forEach { s ->
                    PropertiesTable.insert {
                        it[title] = s[0] as String
                        it[description] = s[1] as String
                        it[price] = (s[2] as String).toBigDecimal()
                        it[location] = s[3] as String
                        it[bedrooms] = s[4] as Int
                        it[bathrooms] = s[5] as Int
                        it[propertyType] = s[6] as String
                        it[isAvailable] = s[7] as Boolean
                    }
                }
            }
        }
    }
}