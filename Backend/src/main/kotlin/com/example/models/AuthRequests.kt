package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequests(
    val email: String? = null,
    val phone: String? = null,
    val password: String,
    val role: String = "Tenant"
)

@Serializable
data class LoginRequest(
    val identifier: String, // email or phone
    val password: String
)

@Serializable
data class UserResponse(
    val id: Int,
    val email: String?,
    val phone: String?,
    val role: String
)

@Serializable
data class LoginResponse(
    val message: String,
    val user: UserResponse,
    val token: String
)