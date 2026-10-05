package com.example.service

import com.example.auth.PasswordHasher
import com.example.models.BadParam
import com.example.models.LoginRequest

import com.example.models.RegisterRequests
import com.example.models.UserResponse
import com.example.repository.User
import com.example.repository.UserRepository

import org.jetbrains.exposed.exceptions.ExposedSQLException

class Conflict(message: String) : Exception(message)
class InvalidCredentials : Exception("Invalid credentials")

class AuthService(private val users: UserRepository) {

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val allowedRoles = setOf("TENANT", "LANDLORD") // ADMIN is promoted manually in the DB

    fun register(req: RegisterRequests): UserResponse {
        val email = req.email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val rawPhone = req.phone?.trim()?.takeIf { it.isNotEmpty() }

        if (email == null && rawPhone == null) throw BadParam("Provide an email or a phone number")
        if (email != null && !emailRegex.matches(email)) throw BadParam("Invalid email")
        val phone = rawPhone?.let { normalizePhone(it) ?: throw BadParam("Invalid phone number") }

        if (req.password.length < 8) throw BadParam("Password must be at least 8 characters")
        if (req.password.length > 72) throw BadParam("Password must be at most 72 characters")

        val role = req.role.trim().uppercase()
        if (role !in allowedRoles) throw BadParam("role must be one of: ${allowedRoles.joinToString()}")

        if (email != null && users.findByEmail(email) != null) throw Conflict("Email already registered")
        if (phone != null && users.findByPhone(phone) != null) throw Conflict("Phone already registered")

        val user = try {
            users.create(email, phone, PasswordHasher.hash(req.password), role)
        } catch (e: ExposedSQLException) {
            // two simultaneous registrations can slip past the checks above; the DB unique index catches them
            throw Conflict("Account already exists")
        }
        return user.toResponse()
    }

    fun login(req: LoginRequest): UserResponse {
        val id = req.identifier.trim()
        val user = if (id.contains("@")) {
            users.findByEmail(id.lowercase())
        } else {
            normalizePhone(id)?.let { users.findByPhone(it) }
        }
        // Same error for "no such user" and "wrong password", so attackers can't discover which accounts exist
        if (user == null || !PasswordHasher.verify(req.password, user.passwordHash)) throw InvalidCredentials()
        return user.toResponse()
    }

    private fun User.toResponse() = UserResponse(id, email, phone, role)

    // Accepts 0712345678, 712345678, 254712345678, +254 712 345 678 -> +254712345678
    private fun normalizePhone(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        val national = when {
            digits.startsWith("254") && digits.length == 12 -> digits.substring(3)
            digits.startsWith("0") && digits.length == 10 -> digits.substring(1)
            digits.length == 9 -> digits
            else -> return null
        }
        return if (national[0] == '7' || national[0] == '1') "+254$national" else null
    }
}