package ru.belkacar.telematics.geofence

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.locationtech.jts.geom.Geometry
import java.util.*

interface DomainId {
    val value: UUID
}

data class GeofenceId(override val value: UUID) : DomainId

@JsonIgnoreProperties(ignoreUnknown = true)
data class Geofence(
    val id: GeofenceId,
    val description: String,
    val geometry: Geometry,
    val type: GeofenceType,
    val attributes: Map<String, String>
)


data class GeofenceItem(
    val id: GeofenceId,
    val type: GeofenceTypeKey,
    val description: String,
    val attributes: Map<String, String>
)

data class GeofenceTypeKey(val value: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeofenceType(
    val key: GeofenceTypeKey,
    val description: String,
    val attributes: Map<String, String>
)