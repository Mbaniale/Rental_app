package com.example.routes

import com.example.models.BadParam
import com.example.models.Property
import com.example.models.respondError
import com.example.models.toPropertyFilter
import com.example.repository.PropertyRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val OWNER_ROLES = setOf("LANDLORD", "ADMIN")

private fun ApplicationCall.userId(): Int =
    principal<JWTPrincipal>()!!.payload.subject.toInt()

private fun ApplicationCall.userRole(): String =
    principal<JWTPrincipal>()!!.payload.getClaim("role").asString()

/** The owner or an admin may change or remove a listing. */
private fun ApplicationCall.canManage(property: Property): Boolean =
    property.ownerId == userId() || userRole() == "ADMIN"

private fun Property.validate() {
    if (title.isBlank() || title.length > 255) throw BadParam("Title is required (max 255 characters)")
    if (description.length > 5000) throw BadParam("Description is too long (max 5000 characters)")
    if (price <= 0) throw BadParam("Price must be greater than 0")
    if (price > 99_999_999.0) throw BadParam("Price is too large")
    if (bedrooms < 0 || bathrooms < 0) throw BadParam("Bedrooms and bathrooms cannot be negative")
    if (location.isBlank() || location.length > 255) throw BadParam("Location is required (max 255 characters)")
    if (propertyType.isBlank() || propertyType.length > 50) throw BadParam("Property type is required (max 50 characters)")
}

fun Route.propertyRoutes(repository: PropertyRepository) {
    route("/properties") {

        // ---------- Public: anyone can browse ----------

        // GET all properties, with optional filters, sorting and paging
        get {
            val filter = try {
                call.request.queryParameters.toPropertyFilter()
            } catch (e: BadParam) {
                call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid parameters")
                return@get
            }
            call.respond(repository.searchProperties(filter))
        }

        // GET property by ID
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respondError(HttpStatusCode.BadRequest, "Invalid ID Format")
                return@get
            }
            val property = repository.getPropertyById(id)
            if (property == null) {
                call.respondError(HttpStatusCode.NotFound, "Property not Found")
            } else {
                call.respond(property)
            }
        }

        // ---------- Protected: need a valid token ----------
        authenticate("auth-jwt") {

            // GET the logged-in landlord's own listings (for a dashboard)
            get("/mine") {
                val page = (call.request.queryParameters["page"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
                val size = (call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
                call.respond(repository.getPropertiesByOwner(call.userId(), page, size))
            }

            // POST - create a listing owned by the logged-in user
            post {
                if (call.userRole() !in OWNER_ROLES) {
                    call.respondError(HttpStatusCode.Forbidden, "Only landlords can add properties")
                    return@post
                }
                val property = call.receive<Property>()
                try {
                    property.validate()
                } catch (e: BadParam) {
                    call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid input")
                    return@post
                }
                // The id and ownerId in the body are ignored; the server sets both.
                call.respond(HttpStatusCode.Created, repository.createProperty(property, call.userId()))
            }

            // PUT - update a listing (owner or admin only)
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respondError(HttpStatusCode.BadRequest, "Invalid ID format")
                    return@put
                }
                val existing = repository.getPropertyById(id)
                if (existing == null) {
                    call.respondError(HttpStatusCode.NotFound, "Property not found")
                    return@put
                }
                if (!call.canManage(existing)) {
                    call.respondError(HttpStatusCode.Forbidden, "You can only edit your own properties")
                    return@put
                }
                val updated = call.receive<Property>()
                try {
                    updated.validate()
                } catch (e: BadParam) {
                    call.respondError(HttpStatusCode.BadRequest, e.message ?: "Invalid input")
                    return@put
                }
                repository.updateProperty(id, updated)
                call.respond(repository.getPropertyById(id)!!)
            }

            // DELETE - remove a listing (owner or admin only)
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respondError(HttpStatusCode.BadRequest, "Property id must be a whole number, got '${call.parameters["id"]}'")
                    return@delete
                }
                val existing = repository.getPropertyById(id)
                if (existing == null) {
                    call.respondError(HttpStatusCode.NotFound, "Property not found")
                    return@delete
                }
                if (!call.canManage(existing)) {
                    call.respondError(HttpStatusCode.Forbidden, "You can only delete your own properties")
                    return@delete
                }
                repository.deleteProperty(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}