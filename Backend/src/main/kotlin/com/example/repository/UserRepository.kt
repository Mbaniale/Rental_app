package com.example.repository

import com.example.database.UsersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class User(
    val id: Int,
    val email: String?,
    val phone: String?,
    val passwordHash: String,
    val role: String
)

class UserRepository {
    private fun rowToUser(row: ResultRow) = User(
        id = row[UsersTable.id],
        email = row[UsersTable.email],
        phone = row[UsersTable.phone],
        passwordHash = row[UsersTable.passwordHash],
        role = row[UsersTable.role]
    )

    fun create(email: String?, phone: String?, passwordHash: String, role: String): User = transaction {
        val stmt = UsersTable.insert {
            it[UsersTable.email] = email
            it[UsersTable.phone] = phone
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = role
            it[UsersTable.createdAt] = System.currentTimeMillis()
        }
        User(stmt[UsersTable.id], email, phone, passwordHash, role)
    }

    fun findByEmail(email: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.email eq email }
            .map(::rowToUser).singleOrNull()
    }

    fun findByPhone(phone: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.phone eq phone }
            .map(::rowToUser).singleOrNull()
    }
}