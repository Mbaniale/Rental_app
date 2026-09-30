package com.example.routes

import com.example.models.Property
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.repository.PropertyRepository

val properties = mutableListOf(
    Property(
        id = 1,
        title = "Modern 2-Bedroom Apartment",
        description ="Spacious apartment in the city center with great views",
        price = 23000.00,
        location = "Nairobi, Westlands",
        bedrooms = 2,
        bathrooms = 2,
        propertyType = "Apartment"

    ),
            Property(
            id = 2,
    title = "Cozy Studio Near University",
    description = "Perfect for students, fully furnished",
    price = 450.0,
    location = "Nairobi, South B",
    bedrooms = 1,
    bathrooms = 1,
    propertyType = "Studio"
)
)
fun Route.propertyRoutes(repository:PropertyRepository){
    route("/properties"){
    //get all properties
    get{
        val properties = repository.getAllProperties()
        call.respond(properties)
    }
    //get properties by ID
    get("/{id}"){
    val id = call.parameters["id"]?.toIntOrNull()
        if(id == null
            ){
            call.respond(HttpStatusCode.BadRequest, "Invalid ID Format")
            return@get
        }
    val property = repository.getPropertyById(id)

    if (property == null){
        call.respond(HttpStatusCode.NotFound, "Property not Found")
    }else {
        call.respond(property)
    }
}
//POST - create a new property
        post {
            val property = call.receive<Property>()
            // The ID from the request body is ignored; the database will generate it.
            val newProperty = repository.createProperty(property)
            call.respond(HttpStatusCode.Created, newProperty)
}}}