package ru.belkacar.core.test

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


data class GeofenceCarEvent(
    val id: String,
    val carId: CarId,
    val timestamp: String,
    val payload: GeofenceEventPayload
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(CarEnteredGeofence::class),
        JsonSubTypes.Type(CarLeavedGeofence::class)
    ]
)
interface GeofenceEventPayload {
    val carId: CarId
    val geofence: Geofence
}

data class Geofence(
    val id: GeofenceId,
    val type: GeofenceType,
    val description: String,
    val attributes: Any
)

data class GeofenceId(val value: String)

data class GeofenceType(val value: String)

data class CarEnteredGeofence(override val carId: CarId, override val geofence: Geofence) : GeofenceEventPayload

data class CarLeavedGeofence(override val carId: CarId, override val geofence: Geofence) : GeofenceEventPayload

