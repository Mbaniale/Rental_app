package com.example.routes

import com.example.auth.JwtService
import com.example.models.BadParam
import com.example.models.LoginRequest
import com.example.models.LoginResponse
import com.example.models.RegisterRequests
import com.example.models.respondError
import com.example.service.AuthService
import com.example.service.Conflict
import com.example.service.InvalidCredentials
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService, jwtService: JwtService) {
    route("/auth") {

        post("/register") {
            val req = call.receive<RegisterRequests>()
            try {
                call.respond(HttpStatusCode.Created, authService.register(req))
            } catch (e: BadParam) {
                call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid input")
            } catch (e: Conflict) {
                call.respondError(HttpStatusCode.Conflict, e.message ?: "Already exists")
            }
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            try {
                val user = authService.login(req)
                call.respond(LoginResponse("Login successful", user, jwtService.createToken(user)))
            } catch (e: InvalidCredentials) {
                call.respondError(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }
    }
}