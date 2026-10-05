package com.example.database

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex().nullable()
    val phone = varchar("phone", 20).uniqueIndex().nullable()
    val passwordHash = varchar("password_hash", 100)
    val role = varchar("role", 20)
    val createdAt = long("created_at") // epoch millis

    override val primaryKey = PrimaryKey(id)
}