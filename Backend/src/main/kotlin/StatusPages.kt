package com.example

import com.example.models.bodyErrorMessage
import com.example.models.respondError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
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
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
}