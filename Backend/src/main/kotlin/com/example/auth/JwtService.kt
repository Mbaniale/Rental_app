package com.example.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.example.models.UserResponse
import java.util.Date

class JwtService(
    secret: String,
    private val issuer: String,
    val audience: String
) {
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun createToken(user: UserResponse): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(user.id.toString())              // NEW: userId() reads this
            .withClaim("userId", user.id)
            .withClaim("role", user.role.uppercase())     // CHANGED: routes compare against "LANDLORD"
            .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
            .sign(algorithm)
}