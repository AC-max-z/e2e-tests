package ru.belkacar.core.test

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


@Deprecated("")
data class GeofenceCarEvent(
    val id: String,
    val carId: CarId<*>,
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
@Deprecated("")
interface GeofenceEventPayload {
    val carId: CarId<*>
    val geofence: Geofence
}

@Deprecated("")
data class Geofence(
    val id: GeofenceId,
    val type: GeofenceType,
    val description: String,
    val attributes: Any
)

@Deprecated("")
data class GeofenceId(val value: String)

@Deprecated("")
data class GeofenceType(val value: String)

@Deprecated("")
data class CarEnteredGeofence(override val carId: CarId<*>, override val geofence: Geofence) : GeofenceEventPayload

@Deprecated("")
data class CarLeavedGeofence(override val carId: CarId<*>, override val geofence: Geofence) : GeofenceEventPayload

