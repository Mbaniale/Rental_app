package com.example

import com.example.database.DatabaseFactory
import com.example.repository.PropertyRepository
import com.example.routes.propertyRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
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

    install(CORS) {
        anyHost()
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }

    val propertyRepository = PropertyRepository() // Create repository instance

    routing {
        get("/") {
            call.respondText("aLEX bUILD THIS")
        }
        propertyRoutes(propertyRepository) // Pass repository to routes
    }
}