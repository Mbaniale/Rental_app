package com.example

import com.example.auth.JwtService
import com.example.database.DatabaseFactory
import com.example.models.BadParam
import com.example.models.ErrorResponse
import com.example.models.bodyErrorMessage
import com.example.models.respondError
import com.example.repository.PropertyRepository
import com.example.repository.UserRepository
import com.example.routes.authRoutes
import com.example.routes.propertyRoutes
import com.example.service.AuthService
import com.example.service.Conflict
import com.example.service.InvalidCredentials
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Initialize the database
    DatabaseFactory.init(this)

    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<BadParam> { call, e ->
            call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid input")
        }
        exception<Conflict> { call, e ->
            call.respondError(HttpStatusCode.Conflict, e.message ?: "Already exists")
        }
        exception<InvalidCredentials> { call, _ ->
            call.respondError(HttpStatusCode.Unauthorized, "Invalid credentials")
        }
        exception<BadRequestException> { call, e ->
            call.respondError(HttpStatusCode.BadRequest, e.bodyErrorMessage())
        }
        exception<kotlinx.serialization.SerializationException> { call, e ->
            call.respondError(HttpStatusCode.BadRequest, e.bodyErrorMessage())
        }
        exception<UnsupportedMediaTypeException> { call, _ ->
            call.respondError(HttpStatusCode.UnsupportedMediaType, "Content-Type must be application/json")
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled: ${call.request.local.method.value} ${call.request.local.uri}", cause)
            call.respondError(HttpStatusCode.InternalServerError, "Something went wrong")
        }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }

    val cfg = environment.config
    val jwtService = JwtService(
        secret = cfg.property("jwt.secret").getString(),
        issuer = cfg.property("jwt.issuer").getString(),
        audience = cfg.property("jwt.audience").getString()
    )
    install(Authentication) {
        jwt("auth-jwt") {
            realm = cfg.property("jwt.realm").getString()
            verifier(jwtService.verifier)
            validate { cred ->
                // Reject tokens with no user id, so stale tokens get 401 instead of a 500
                if (cred.payload.audience.contains(jwtService.audience) &&
                    cred.payload.subject?.toIntOrNull() != null
                ) JWTPrincipal(cred.payload) else null
            }
            challenge { _, _ ->
                val reason = if (call.request.headers[HttpHeaders.Authorization] == null)
                    "Authorization header is missing"
                else
                    "Token is invalid or expired"
                call.respondError(HttpStatusCode.Unauthorized, reason)
            }
        }
    }

    val propertyRepository = PropertyRepository()

    val userRepository = UserRepository()
    val authService = AuthService(userRepository)

    routing {
        get("/") {
            call.respondText("aLEX bUILD THIS")
        }
        staticResources("/app", "static")
        propertyRoutes(propertyRepository)
        authRoutes(authService, jwtService)
    }
}