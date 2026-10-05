package com.example.models

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class ErrorResponse(val error: String)

suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) =
    respond(status, ErrorResponse(message))

/** Walks the cause chain and turns a parsing failure into a readable reason. */
fun Throwable.bodyErrorMessage(): String {
    var t: Throwable? = this
    while (t != null) {
        if (t is MissingFieldException) {
            return "Missing required field(s): ${t.missingFields.joinToString(", ")}"
        }
        if (t is SerializationException) {
            val msg = t.message.orEmpty()
            Regex("unknown key '([^']+)'").find(msg)?.let {
                return "Unknown field '${it.groupValues[1]}' is not allowed"
            }
            Regex("at path: \\$\\.?(\\S*)").find(msg)?.let {
                val path = it.groupValues[1]
                if (path.isNotEmpty()) return "Invalid value or wrong type for field '$path'"
            }
            return "Request body is not valid JSON"
        }
        t = t.cause
    }
    return "Request body is missing or malformed"
}